package com.prod.fbrigati.myfinance.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.prod.fbrigati.myfinance.Utility;

/**
 * Created by FBrigati on 27/04/2017.
 */

public class DataProvider extends ContentProvider {

    final static String LOG_TAG = DataProvider.class.getSimpleName();

    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private DataDBHelper mOpenHelper;

    static final int STATEMENT = 100;
    static final int STATEMENT_WITH_ID = 101;
    static final int STATEMENT_WITH_ACCTNUMBER = 102;
    static final int STATEMENT_STATS_TRIMESTER = 103;
    static final int STATEMENT_STATS_MONTH = 104;
    static final int STATEMENT_WIDGET_DATA = 105;
    static final int STATEMENT_LINEGRAPH_DATA = 106;
    static final int CATEGORY = 200;
    static final int CATEGORY_WITH_ACQUIRER = 201;
    static final int BUDGET = 300;
    static final int BUDGET_WITH_MONTH = 301;
    static final int BUDGET_WIDGET = 302;
    static final int CUREX = 400;
    static final int CUREX_WITH_BASE = 401;


    private static final SQLiteQueryBuilder mStatementQueryBuilder;
    private static final SQLiteQueryBuilder mCurrencyQueryBuilder;



    static{
        mStatementQueryBuilder = new SQLiteQueryBuilder();

        //This is an inner join which looks like
        //statement LEFT JOIN category ON statement.category = category._id
        mStatementQueryBuilder.setTables(
                DataContract.StatementEntry.TABLE_NAME + " LEFT JOIN " +
                        DataContract.CategoryEntry.TABLE_NAME +
                        " ON " + DataContract.StatementEntry.TABLE_NAME +
                        "." + DataContract.StatementEntry.COLUMN_CATEGORY_KEY +
                        " = " + DataContract.CategoryEntry.TABLE_NAME +
                        "." + DataContract.CategoryEntry.COLUMN_CATEGORY_USER_KEY);
    }

    static{
        mCurrencyQueryBuilder = new SQLiteQueryBuilder();
        mCurrencyQueryBuilder.setTables(DataContract.CurrencyExEntry.TABLE_NAME);}



    //statement._ID = ?
    private static final String sStatementIDSelection =
            DataContract.StatementEntry.TABLE_NAME +
                    "." + DataContract.StatementEntry._ID + " = ?";


        //currencyex.symbol like '%Base'
    private static final String sBaseCurrencySelection =
            DataContract.CurrencyExEntry.TABLE_NAME +
                    "." + DataContract.CurrencyExEntry.COLUMN_SYMBOL + " like ?";


    //statement.account = ? AND date = ?
    private static final String sAcctnumberAndDateSelection =
            DataContract.StatementEntry.TABLE_NAME +
                    "." + DataContract.StatementEntry.COLUMN_ACCOUNT_NUMBER + " = ? AND " +
                    DataContract.StatementEntry.COLUMN_DATE + " = ? ";

    private Cursor getCurrenciesByBaseCurrency(
            Uri uri, String[] projection, String sortOrder) {
        String baseCurrency = DataContract.CurrencyExEntry.getBaseCurrenyFromUri(uri);
        //Log.v(LOG_TAG, "baseCurrency: " + baseCurrency);
        return mCurrencyQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sBaseCurrencySelection,
                new String[]{baseCurrency + "%"},
                null,
                null,
                sortOrder
        );
    }


    static UriMatcher buildUriMatcher() {
        // I know what you're thinking.  Why create a UriMatcher when you can use regular
        // expressions instead?  Because you're not crazy, that's why.

        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = DataContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, DataContract.PATH_STATEMENT, STATEMENT);
        matcher.addURI(authority, DataContract.PATH_STATEMENT + "/*", STATEMENT_WITH_ID);
        matcher.addURI(authority, DataContract.PATH_STATEMENT + "/#", STATEMENT_WITH_ACCTNUMBER);
        matcher.addURI(authority, DataContract.PATH_STATEMENT + "/*/#", STATEMENT_STATS_MONTH);
        matcher.addURI(authority, DataContract.PATH_STATEMENT + "/widget/data", STATEMENT_WIDGET_DATA);
        matcher.addURI(authority, DataContract.PATH_STATEMENT + "/*/*/#", STATEMENT_STATS_TRIMESTER);
        matcher.addURI(authority, DataContract.PATH_STATEMENT + "/*/*/*/#", STATEMENT_LINEGRAPH_DATA);


        matcher.addURI(authority, DataContract.PATH_CATEGORY, CATEGORY);
        matcher.addURI(authority, DataContract.PATH_CATEGORY + "/*", CATEGORY_WITH_ACQUIRER);

        matcher.addURI(authority, DataContract.PATH_BUDGET, BUDGET);
        matcher.addURI(authority, DataContract.PATH_BUDGET + "/#", BUDGET_WITH_MONTH);
        matcher.addURI(authority, DataContract.PATH_BUDGET + "/widget/#", BUDGET_WIDGET);

        matcher.addURI(authority, DataContract.PATH_CUREX, CUREX);
        matcher.addURI(authority, DataContract.PATH_CUREX + "/*", CUREX_WITH_BASE);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DataDBHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        //Log.v(LOG_TAG, "Query ID: " + uri);

        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            // statement/*
            case STATEMENT_WITH_ID:
            {
                retCursor = getStatementByID(uri, projection, sortOrder);
                break;
            }
            // statement/#"
            case STATEMENT_WITH_ACCTNUMBER:
            {
                retCursor = getStatementByAccount(uri, projection, sortOrder);
                break;
            }
            // "statement/#/*"
            case STATEMENT_STATS_MONTH: {
                retCursor = getStatsByMOnth(uri, projection, sortOrder);
                break;
            }
            case STATEMENT_STATS_TRIMESTER: {
                retCursor = getStatsPieChartByTrimester(uri, projection, sortOrder);
                break;
            }
            case STATEMENT_LINEGRAPH_DATA: {
                retCursor = getStatsLineGraphByTrimester(uri, projection, sortOrder);
                break;
            }
            // "statement"
            case STATEMENT: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        DataContract.StatementEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case STATEMENT_WIDGET_DATA: {
                retCursor = getWidgetDataCursor(uri, projection, sortOrder);
                break;
            }
            // "Category"
            case CATEGORY: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        DataContract.CategoryEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            // "Budget"
            case BUDGET_WITH_MONTH: {
                retCursor = getBudgetWithMonth(uri, projection, sortOrder);
                break;
            }
            case BUDGET_WIDGET: {
                retCursor = getWidgetCollectionCursor(uri, projection, sortOrder);
                break;
            }
            // "Currencies"
            case CUREX: {
                //Log.v(LOG_TAG, "currency query called");
                retCursor = mOpenHelper.getReadableDatabase().query(
                        DataContract.CurrencyExEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            // "Currencies"
            case CUREX_WITH_BASE: {

                retCursor = getCurrenciesByBaseCurrency(uri, projection, sortOrder);
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    private Cursor getStatsByMOnth(Uri uri, String[] projection, String sortOrder) {
        //String category = DataContract.BudgetEntry.getBudgetCategory(uri);
        int month = DataContract.StatementEntry.getMonthFromUri(uri);


        return mOpenHelper.getReadableDatabase().rawQuery(
                "select a._ID, a.date, a.amount, a.category from statement a " +
                        " where substr(a.date,5,2)*1 = ? ", new String[] {String.valueOf(month)}); // new String[] {String.valueOf(month)});
    }

    private Cursor getStatsPieChartByTrimester(Uri uri, String[] projection, String sortOrder) {
        //String category = DataContract.BudgetEntry.getBudgetCategory(uri);
        int trimestre = DataContract.StatementEntry.getMonthFromUri(uri);
        int year = Utility.getStatsNavYear(getContext());


        switch (trimestre){
            case 1:
                return mOpenHelper.getReadableDatabase().rawQuery(
                        "select a.category, sum(a.amount) from statement a " +
                                " where substr(a.date,5,2)*1 BETWEEN 1 AND 3 " +
                                " and substr(a.date,1,4)*1 =" + year +
                                " and a.trxcode >=6 " +
                                " group by a.category" , null);
            case 2:
                return mOpenHelper.getReadableDatabase().rawQuery(
                        "select a.category, sum(a.amount) from statement a " +
                                " where substr(a.date,5,2)*1 BETWEEN 4 AND 6 " +
                                " and substr(a.date,1,4)*1 =" + year +
                                " and a.trxcode >=6 " +
                                " group by a.category", null);
            case 3:
                return mOpenHelper.getReadableDatabase().rawQuery(
                        "select a.category, sum(a.amount) from statement a " +
                                " where substr(a.date,5,2)*1 BETWEEN 7 AND 9 " +
                                " AND substr(a.date,1,4)*1 =" + year +
                                " and a.trxcode >=6 " +
                                " group by a.category", null);
            case 4:
                return mOpenHelper.getReadableDatabase().rawQuery(
                        "select a.category, sum(a.amount) from statement a " +
                                " where substr(a.date,5,2)*1 BETWEEN 10 AND 12 " +
                                " AND substr(a.date,1,4)*1 =" + year +
                                " and a.trxcode >=6 " +
                                " group by a.category", null);
            default:
                return null;
        }
    }

    private Cursor getStatsLineGraphByTrimester(Uri uri, String[] projection, String sortOrder) {
        //String category = DataContract.BudgetEntry.getBudgetCategory(uri);
        int trimestre = DataContract.StatementEntry.getMonthFromUri(uri);
        String cat = DataContract.StatementEntry.getCategoryFromUri(uri);
        int year = Utility.getStatsNavYear(getContext());


        switch (trimestre){
            case 1:
                if(cat.trim().equals("All")){
                    return mOpenHelper.getReadableDatabase().rawQuery(
                            "select a.category, a.date, sum(a.amount) from statement a " +
                                    " where substr(a.date,5,2)*1 BETWEEN 1 AND 3 " +
                                    " and substr(a.date,1,4)*1 =" + year +
                                    " and a.trxcode >=6 " +
                                    " group by a.date, a.category " +
                                    " order by a.category, a.date " , null);
                }else{
                return mOpenHelper.getReadableDatabase().rawQuery(
                        "select a.category, a.date, sum(a.amount) from statement a " +
                                " where substr(a.date,5,2)*1 BETWEEN 1 AND 3 " +
                                " and substr(a.date,1,4)*1 =" + year +
                                " and a.category ='" + cat + "'" +
                                " and a.trxcode >=6 " +
                                " group by a.date, a.category " +
                                " order by a.date " , null);}
            case 2:
                if(cat.trim().equals("All")){
                return mOpenHelper.getReadableDatabase().rawQuery(
                        "select a.category, a.date, sum(a.amount) from statement a " +
                                " where substr(a.date,5,2)*1 BETWEEN 4 AND 6 " +
                                " and substr(a.date,1,4)*1 =" + year +
                                " and a.trxcode >=6 " +
                                " group by a.date, a.category " +
                                " order by a.category, a.date ", null);}
                else{
                    return mOpenHelper.getReadableDatabase().rawQuery(
                            "select a.category, a.date, sum(a.amount) from statement a " +
                                    " where substr(a.date,5,2)*1 BETWEEN 4 AND 6 " +
                                    " and substr(a.date,1,4)*1 =" + year +
                                    " and a.category ='" + cat + "'" +
                                    " and a.trxcode >=6 " +
                                    " group by a.date, a.category " +
                                    " order by a.date ", null);
                }
            case 3:
                if(cat.trim().equals("All")){
                    return mOpenHelper.getReadableDatabase().rawQuery(
                            "select a.category, a.date, sum(a.amount) from statement a " +
                                    " where substr(a.date,5,2)*1 BETWEEN 7 AND 9 " +
                                    " AND substr(a.date,1,4)*1 =" + year +
                                    " and a.trxcode >=6 " +
                                    " group by a.date, a.category " +
                                    " order by a.category, a.date ", null);
                }else{
                return mOpenHelper.getReadableDatabase().rawQuery(
                        "select a.category, a.date, sum(a.amount) from statement a " +
                                " where substr(a.date,5,2)*1 BETWEEN 7 AND 9 " +
                                " AND substr(a.date,1,4)*1 =" + year +
                                " and a.category ='" + cat + "'" +
                                " and a.trxcode >=6 " +
                                " group by a.date, a.category " +
                                " order by a.date ", null);}
            case 4:
                if(cat.trim().equals("All")){
                return mOpenHelper.getReadableDatabase().rawQuery(
                        "select a.category, a.date, sum(a.amount) from statement a " +
                                " where substr(a.date,5,2)*1 BETWEEN 10 AND 12 " +
                                " AND substr(a.date,1,4)*1 =" + year +
                                " and a.trxcode >=6 " +
                                " group by a.date, a.category " +
                                " order by a.category, a.date ", null);
                }else{
                    return mOpenHelper.getReadableDatabase().rawQuery(
                            "select a.category, a.date, sum(a.amount) from statement a " +
                                    " where substr(a.date,5,2)*1 BETWEEN 10 AND 12 " +
                                    " AND substr(a.date,1,4)*1 =" + year +
                                    " and a.category ='" + cat + "'" +
                                    " and a.trxcode >=6 " +
                                    " group by a.date, a.category " +
                                    " order by a.date ", null);
                }
            default:
                return null;
        }
    }

    private Cursor getWidgetDataCursor(Uri uri, String[] projection, String sortOrder) {

        return mOpenHelper.getReadableDatabase().rawQuery(
                        "ifnull(" +
                        "(SELECT amount FROM statement as a " +
                        "WHERE a.date = cast(date('now', '%Y%m%d') as INT) " +
                        " and a.trxcode >=6 " +
                        "), 0) " +
                        "UNION " +
                        "ifnull(SELECT date, amount, 'week' as period FROM statement as b " +
                        "WHERE b.date >= DATE('now', 'weekday 0', '-7 days') " +
                        " and a.trxcode >=6 " +
                        "), 0) " +
                        "UNION " +
                        "ifnull(" +
                        "(SELECT date, amount, 'month' as period FROM statement as c " +
                        "WHERE substr(c.date,5,2)*1 = date('now', %m)*1 " +
                        " and a.trxcode >=6 " +
                        "), 0)",null);
    }

    private Cursor getWidgetCollectionCursor(Uri uri, String[] projection, String sortOrder) {

        int month = DataContract.BudgetEntry.getBudgetMonth(uri);

        return mOpenHelper.getReadableDatabase().rawQuery(
                "select S._ID, S.month, S.year , S.category, S.amount, T.amount, T.date From budget as S inner join " +
                        "(select substr(a.date,5,2)*1 as Month, sum(amount) amount, category, date from statement as a " +
                        "group by category, substr(a.date,5,2)*1) as T ON " +
                        "S.month = T.month and S.category = T.category " +
                        "where S.month = ? ORDER BY T.date DESC LIMIT 2",new String[] {String.valueOf(month)});

    }

    private Cursor checkCategory(Uri uri, String[] projection, String sortOrder) {

        String query = "SELECT * FROM category ";

        return mOpenHelper.getReadableDatabase().rawQuery(
                query , null);//, String.valueOf(month)});
    }

    private Cursor getBudgetWithMonth(Uri uri, String[] projection, String sortOrder) {

        //String category = DataContract.BudgetEntry.getBudgetCategory(uri);
        int month = DataContract.BudgetEntry.getBudgetMonth(uri);

        String query = "SELECT B._ID, A.desc_default, ifnull(C.amount,0), ifnull(B.amount,0), " +
                "ifnull(C.month_1,0), B.month, B.year FROM category as A " +
                "LEFT JOIN (SELECT * from budget WHERE month ="+ month +") AS B ON A.desc_default = B.category " +
                "LEFT JOIN (SELECT substr(t.date,5,2)*1 as month_1, " +
                "sum(t.amount) as amount, t.category category FROM statement AS t " +
                "WHERE substr(t.date,5,2)*1 = "+ month + " AND trxcode >=6" +
                " group by t.category, substr(t.date,5,2)*1) AS C ON " +
                "A.desc_default = C.category " +
                "ORDER BY C.amount desc, A.desc_default";

        //Log.v(LOG_TAG, "Query is: " + query );

        return mOpenHelper.getReadableDatabase().rawQuery(
                query , null); // new String[] {String.valueOf(month)});//, String.valueOf(month)});
    }


    private Cursor getStatementByID(Uri uri, String[] projection, String sortOrder) {

        String ID = String.valueOf(DataContract.StatementEntry.getIDFromUri(uri));

        //Log.v(LOG_TAG, "ID = " +ID);

        return mStatementQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                new String[]{ DataContract.StatementEntry.COLUMN_TRANSACTION_CODE,
                DataContract.StatementEntry.COLUMN_CATEGORY_KEY,
                DataContract.StatementEntry.COLUMN_TRANSACTION_CODE,
                DataContract.StatementEntry.COLUMN_DESCRIPTION_USER,
                DataContract.StatementEntry.COLUMN_DATE,
                DataContract.StatementEntry.COLUMN_TIME,
                DataContract.StatementEntry.COLUMN_AMOUNT,
                DataContract.CategoryEntry.COLUMN_CATEGORY_USER_KEY,
                DataContract.StatementEntry.COLUMN_TRANSACTION_CODE},
                sStatementIDSelection,
                new String[]{ID},
                null,
                null,
                sortOrder
        );
    }

    private Cursor getStatementByAccount(Uri uri, String[] projection, String sortOrder) {

            String acctNumber = DataContract.StatementEntry.getAccountFromUri(uri);
            int date = DataContract.StatementEntry.getDateFromUri(uri);

            return mStatementQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                    projection,
                    sAcctnumberAndDateSelection,
                    new String[]{acctNumber, Integer.toString(date)},
                    null,
                    null,
                    sortOrder
            );
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);

        switch(match){
            case STATEMENT:
                return DataContract.StatementEntry.CONTENT_TYPE;
            case STATEMENT_WITH_ACCTNUMBER:
                return DataContract.StatementEntry.CONTENT_TYPE;
            case STATEMENT_STATS_MONTH:
                return DataContract.StatementEntry.CONTENT_TYPE;
            case CATEGORY:
                return DataContract.CategoryEntry.CONTENT_TYPE;
            case CATEGORY_WITH_ACQUIRER:
                return DataContract.CategoryEntry.CONTENT_ITEM_TYPE;
            case BUDGET:
                return DataContract.BudgetEntry.CONTENT_TYPE;
            case BUDGET_WITH_MONTH:
                return DataContract.BudgetEntry.CONTENT_ITEM_TYPE;
            case BUDGET_WIDGET:
                return DataContract.BudgetEntry.CONTENT_ITEM_TYPE;
            case CUREX:
                return DataContract.CurrencyExEntry.CONTENT_TYPE;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;


        switch(match){
            case STATEMENT:{
                //normalizeData();
                long _id = db.insert(DataContract.StatementEntry.TABLE_NAME, null, values);
                if (_id > 0)
                {
                    returnUri = DataContract.StatementEntry.buildStatementUri(_id);
                }
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case CATEGORY:{
                long _id = db.insert(DataContract.CategoryEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = DataContract.CategoryEntry.buildCategoryUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case BUDGET:{
                long _id = db.insert(DataContract.BudgetEntry.TABLE_NAME, null, values);
                if (_id > 0) {
                    returnUri = DataContract.BudgetEntry.buildBudgetUri(_id);
                }else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case CUREX:{
                long _id = db.insert(DataContract.CurrencyExEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = DataContract.CurrencyExEntry.buildCurrencyExUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        // this makes delete all rows return the number of rows deleted
        if ( null == selection ) selection = "1";
        switch (match) {
            case STATEMENT:
                rowsDeleted = db.delete(
                        DataContract.StatementEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case CATEGORY:
                rowsDeleted = db.delete(
                        DataContract.CategoryEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case BUDGET:
                rowsDeleted = db.delete(
                        DataContract.BudgetEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case CUREX:
                rowsDeleted = db.delete(
                        DataContract.CurrencyExEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Because a null deletes all rows
        if (rowsDeleted != 0)
        {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
            final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            final int match = sUriMatcher.match(uri);
            int rowsUpdated;

            switch (match) {
                case STATEMENT:
                    //normalizeDate(values);
                    rowsUpdated = db.update(DataContract.StatementEntry.TABLE_NAME, values, selection,
                            selectionArgs);
                    break;
                case CATEGORY:
                    rowsUpdated = db.update(DataContract.CategoryEntry.TABLE_NAME, values, selection,
                            selectionArgs);
                    break;
                case BUDGET:
                    rowsUpdated = db.update(DataContract.BudgetEntry.TABLE_NAME, values, selection,
                            selectionArgs);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
            if (rowsUpdated != 0)
            {
                getContext().getContentResolver().notifyChange(uri, null);
            }
            return rowsUpdated;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        switch (sUriMatcher.match(uri)) {
            case CUREX:
                db.beginTransaction();
                int returnCount = 0;
                //Log.v(LOG_TAG, "About to insert values in currencyExchange..");
                try {
                    for (ContentValues value : values) {
                        db.insert(
                                DataContract.CurrencyExEntry.TABLE_NAME,
                                null,
                                value
                        );
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }
}
