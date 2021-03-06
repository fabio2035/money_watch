package com.prod.fbrigati.myfinance.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayout;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.prod.fbrigati.myfinance.adapters.StatementAdapter;
import com.prod.fbrigati.myfinance.data.DataContract;

import com.prod.fbrigati.myfinance.R;

import com.prod.fbrigati.myfinance.data.StatementLoader;
import com.google.android.gms.ads.AdView;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;


/**
 * Created by FBrigati on 03/05/2017.
 */

public class StatementFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    final static String LOG_TAG = StatementFragment.class.getSimpleName();

    public static final String ARG_ITEM_ID = "item_id";
    private int uMonth;

    public final static String ID_MESSAGE = "com.prod.fbrigati.myfinance.ui.StatementFragment.MESSAGE";
    public static final int STATEMENT_LOADER = 0;
    private Cursor mCursor;

    static final String STATEMENT_URI = "URI";
    private boolean onResumeflag = false;

    StatementAdapter statementAdapter;

    Intent intent;

    @Override
    public void onStart(){
        super.onStart();
    }

    private View rootView;
    private TextView textSequence;
    private TextView textDate;
    private TextView textDescription;
    private TextView textPayee;
    private TextView textCategory;
    private TextView textAmount;
    private TextView textBalance;
    private ListView statement_details;
    private TextView empty_view;
    private View header_view;
    private ViewGroup headerView;
    private Toolbar toolbarView;
    private ImageButton bakBtn;
    private ImageButton fwdBtn;
    private TextView monthLabel;
    private AdView advertising;
    private RelativeLayout empty_viewLL;
    private GridLayout balanceGrid;

    private final DecimalFormat currencyFormatWithPlus;
    private final DecimalFormat currencyFormatWithMinus;


    public StatementFragment(){
        currencyFormatWithMinus = (DecimalFormat) NumberFormat.getInstance(Locale.US);
        currencyFormatWithPlus = (DecimalFormat) NumberFormat.getInstance(Locale.US);
        currencyFormatWithPlus.setPositivePrefix(" + ");
        currencyFormatWithMinus.setPositivePrefix(" - ");
    }


    static StatementFragment newInstance(int month) {
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_ITEM_ID, month);
        StatementFragment fragment = new StatementFragment();
        fragment.setArguments(arguments);
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){

        Bundle arguments = getArguments();
        if (arguments != null){
            uMonth = arguments.getInt(ARG_ITEM_ID);
        }

        rootView = inflater.inflate(R.layout.fragment_statement_main_bkp, container, false);

        //transaction date
        textDate = (TextView) rootView.findViewById(R.id.row_date);

        //user transaction notes
        textDescription = (TextView) rootView.findViewById(R.id.row_description);

        //transaction amount
        textAmount = (TextView) rootView.findViewById(R.id.row_amt);

        statement_details = (ListView) rootView.findViewById(R.id.item_statement_container);

        statement_details.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showTransactionEditDialog(id);
            }
        });

        statement_details.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id) {
                final Long identifier = id;

                AlertDialog.Builder alert = new AlertDialog.Builder(
                        getActivity());
                alert.setTitle(R.string.dialog_delete_record_title);
                alert.setMessage(R.string.dialog_delete_record_message);
                alert.setPositiveButton(R.string.dialog_delete_record_yes, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteRecord(identifier);
                        //do your work here
                        dialog.dismiss();

                    }
                });
                alert.setNegativeButton(R.string.dialog_delete_record_no, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                alert.show();

                return true;
            }
        });

        //headerView = (ViewGroup) inflater.inflate(R.layout.item_statement_header, statement_details, false);

        //statement_details.addHeaderView(headerView);

        statementAdapter = new StatementAdapter(getActivity(), null, 0);

        statement_details.setAdapter(statementAdapter);

        empty_view = (TextView) rootView.findViewById(R.id.empty_statement);

        textBalance = (TextView) rootView.findViewById(R.id.balance_value);

        balanceGrid = (GridLayout) rootView.findViewById(R.id.grid_balance);

        advertising = (AdView) rootView.findViewById(R.id.ad_view_statement);

        AdRequest adRequest = new AdRequest.Builder().build();
        advertising.loadAd(adRequest);

        //        .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)        // All emulators
        //        .addTestDevice("53F4B94474E00A7E14FD516F7AD2ACDF")  // My Galaxy Nexus test phone
        //       .build();

        //advertising.loadAd(adRequest);

        //      advertising = (AdView) rootView.findViewById(R.id.ad_view_statement);

        empty_viewLL = (RelativeLayout) rootView.findViewById(R.id.empty_view);

        return rootView;

    }


    private void deleteRecord(Long id) {

        int i = getActivity().getContentResolver().delete(
                DataContract.StatementEntry.CONTENT_URI,
                DataContract.StatementEntry._ID + " = ?",
                new String[] {String.valueOf(id)});

        if(i>0){
            Toast.makeText(getContext(), R.string.toast_deleterecord_success, Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(getContext(), R.string.toast_deleterecord_nosuccess, Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onResume(){
        super.onResume();

        /*rootView.setVisibility(View.GONE);

        mShortAnimationDuration = getResources().getInteger(
                android.R.integer.config_shortAnimTime); */
        /*
        //run alpha opacity animation only when screen is first loaded
        if(onResumeflag == false){
        //Initially hide the content view
        rootView.setVisibility(View.GONE);

        // Retrieve and cache the system's default "short" animation time.
        } */

//        navigateMonth(2);
    }


    private void showTransactionEditDialog(Long id) {
        intent = new Intent(getActivity(), StatementActEditTrxDialog.class);
        intent.setData(DataContract.StatementEntry.buildStatementUri(id));
        getActivity().startActivity(intent);
    }



    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(STATEMENT_LOADER, null, this);
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
       return StatementLoader.newInstance(getContext(), uMonth);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursor = data;
        switch (loader.getId()) {
            case STATEMENT_LOADER:

                if (mCursor != null && mCursor.moveToFirst() && mCursor.getCount() > 0) {
                    statementAdapter.swapCursor(mCursor);
                    calculateBalance(mCursor);
                    updateView(1);
                }else{
                    updateView(0);
                    String positiveValue = currencyFormatWithPlus.format(0);
                    textBalance.setText(String.valueOf(positiveValue));
                    textBalance.setTextColor(getResources().getColor(R.color.positive));}
                break;
        }
    }

    private void calculateBalance(Cursor data) {

        //Calculate the monthly balance..

        Double balance = 0.0;
        Double sum = 0.0;
        int trxType = 0;

        data.moveToFirst();
        sum = Double.valueOf(data.getString(DataContract.StatementEntry.COL_AMOUNT));

        trxType = data.getInt(DataContract.StatementEntry.COL_TRANSACTION_CODE);

        if (trxType < 5){
            balance += sum;
        }else{
            balance -= sum;
        }

        while(data.moveToNext()){
            sum = Double.valueOf(data.getString(DataContract.StatementEntry.COL_AMOUNT));
            trxType = data.getInt(DataContract.StatementEntry.COL_TRANSACTION_CODE);
            if (trxType < 5){
                balance += sum;
            }else{
                balance -= sum;
            }
        }

        //set the colors..

        if(balance > 0.0){
            String positiveValue = currencyFormatWithPlus.format(balance);
            textBalance.setText(String.valueOf(positiveValue));
            textBalance.setTextColor(getResources().getColor(R.color.positive));
        }else{
            String negativeValue = currencyFormatWithMinus.format(balance);
            textBalance.setText(String.valueOf(negativeValue));
            textBalance.setTextColor(getResources().getColor(R.color.negative));
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        statementAdapter.swapCursor(null);
        mCursor = null;
    }

    private void updateView(int flag) {

        //Data found
        if(flag == 1){
            onResumeflag =true;
            statement_details.setVisibility(View.VISIBLE);
            empty_viewLL.setVisibility(View.GONE);
            balanceGrid.setVisibility(View.VISIBLE);
        //No data found to be displayed
        }else{ //if(flag == 0){
            statement_details.setVisibility(View.GONE);
            empty_viewLL.setVisibility(View.VISIBLE);
            balanceGrid.setVisibility(View.GONE);
        }
    }


}
