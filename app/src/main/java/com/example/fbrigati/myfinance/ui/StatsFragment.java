package com.example.fbrigati.myfinance.ui;

import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayout;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.fbrigati.myfinance.R;

import com.example.fbrigati.myfinance.Utility;
import com.example.fbrigati.myfinance.data.DataContract;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.Calendar;

import static android.R.attr.textColor;
import static com.example.fbrigati.myfinance.Utility.getStatsNavYear;
import static com.example.fbrigati.myfinance.Utility.getStatsTrimester;

/**
 * Created by FBrigati on 07/05/2017.
 */

public class StatsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        SeekBar.OnSeekBarChangeListener{

    final static String LOG_TAG = StatsFragment.class.getSimpleName();

    public final static String ID_MESSAGE = "com.example.fbrigati.myfinance.ui.StatsFragment.MESSAGE";

    public static final int PIECHART_LOADER = 6;

    public static final int LINECHART_LOADER = 0;

    int maxCount;

    private int mTrimester;

    private Uri mUri;

    private AdView mAdView;

    private PieChart mpieChart;
    private LineChart mlineChart;
    private Toolbar toolbarView;
    private GridLayout masterGrid;
    private SeekBar seekBar;
    LinearLayout mSeekLin;

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        getLoaderManager().initLoader(PIECHART_LOADER, null, this);
        //getLoaderManager().initLoader(LINECHART_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        Bundle arguments = getArguments();
        if(arguments != null){
            mUri = arguments.getParcelable(ID_MESSAGE);
        }

        View rootView = inflater.inflate(R.layout.fragment_stats, container, false);

        //Get the pieschart view
        mpieChart = (PieChart) rootView.findViewById(R.id.piechart);

        //seekBarLabel = (TextView) rootView.findViewById(R.id.seekBarLabel);

        seekBar = (SeekBar) rootView.findViewById(R.id.seekBarpie);

        seekBar.setOnSeekBarChangeListener(this);

        mSeekLin = (LinearLayout) rootView.findViewById(R.id.seekBarLabelLayout);

        maxCount = 4;

        seekBar.setMax(maxCount - 1);

        mSeekLin.setOrientation(LinearLayout.HORIZONTAL);

        mSeekLin.setPadding(10, 0, 10, 0);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        params.setMargins(35, 0, 35, 0);

        mSeekLin.setLayoutParams(params);

        addLabelsBelowSeekBar();

        setupPieChart();

        //Get the linechart view
        mlineChart = (LineChart) rootView.findViewById(R.id.linechart);

        toolbarView = (Toolbar) rootView.findViewById(R.id.toolbar);

        toolbarView.setTitle(R.string.toolbar_stats_title);

        setupLineChart();

        //masterGrid = (GridLayout) rootView.findViewById(R.id.master_grid);

        return rootView;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        switch (loader.getId()){
            case PIECHART_LOADER:
                loader = null;
                break;
        }
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        Log.v(LOG_TAG, "Pie Data selected: " + seekBar.getProgress());
        Utility.setStatsPieTrimester(getActivity(), seekBar.getProgress()+1);
        //mTrimester = seekBar.getProgress()+1;
        getLoaderManager().restartLoader(PIECHART_LOADER, null, this);
        //setPieData(seekBar.getProgress());
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private void addLabelsBelowSeekBar() {
        String[] trimester = getResources().getStringArray(R.array.trimesters);
        for (int count = 0; count < maxCount; count++) {
            TextView textView = new TextView(getContext());
            textView.setText(trimester[count]);
            textView.setTextColor(getResources().getColor(R.color.colorPrimary));
            textView.setGravity(Gravity.LEFT);
            mSeekLin.addView(textView);
            textView.setLayoutParams((count == maxCount - 1) ? getLayoutParams(0.0f) : getLayoutParams(1.0f));
        }
    }

    LinearLayout.LayoutParams getLayoutParams(float weight) {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, weight);
    }

    private void setupPieChart() {

        //Pie Chart setup
        mpieChart.setUsePercentValues(true);
        mpieChart.setExtraOffsets(5, 10, 5, 5);

        mpieChart.setDragDecelerationFrictionCoef(0.95f);

        mpieChart.setDrawHoleEnabled(true);
        mpieChart.setHoleColor(Color.WHITE);

        mpieChart.setTransparentCircleColor(Color.WHITE);
        mpieChart.setTransparentCircleAlpha(110);

        mpieChart.setHoleRadius(38f);
        mpieChart.setTransparentCircleRadius(51f);

        mpieChart.setDrawCenterText(true);

        mpieChart.setRotationAngle(0);
        // enable rotation of the chart by touch
        mpieChart.setRotationEnabled(true);
        mpieChart.setHighlightPerTapEnabled(true);


        Legend l = mpieChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        l.setOrientation(Legend.LegendOrientation.VERTICAL);
        l.setDrawInside(false);
        l.setXEntrySpace(7f);
        l.setYEntrySpace(0f);
        l.setYOffset(0f);

    }

    private void setPieData(Cursor data) {

        mpieChart.animateY(1400, Easing.EasingOption.EaseInOutQuad);

        ArrayList<PieEntry> entries = new ArrayList<PieEntry>();

        Double totalAmount = 0.0;

        //first get the total value of spendings..
        for(int i=0; i< data.getCount(); i++){
        totalAmount = totalAmount + data.getDouble(1);
            data.moveToNext();
        }

        Log.v(LOG_TAG, "Total category amounts: " + totalAmount);

        data.moveToFirst();

        // NOTE: The order of the entries when being added to the entries array determines their position around the center of
        // the chart.
        for (int i = 0; i < data.getCount() ; i++) {
            entries.add(new PieEntry((float) (data.getDouble(1) / totalAmount), data.getString(0) ));
            data.moveToNext();
        }

        PieDataSet dataSet = new PieDataSet(entries, getResources().getString(R.string.piechartLegedTitle));
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        // add a lot of colors

        ArrayList<Integer> colors = new ArrayList<Integer>();

        for (int c : ColorTemplate.MATERIAL_COLORS)
            colors.add(c);
        /*
        for (int c : ColorTemplate.VORDIPLOM_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.PASTEL_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.LIBERTY_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.PASTEL_COLORS)
            colors.add(c); */

        colors.add(ColorTemplate.getHoloBlue());

        dataSet.setColors(colors);
        //dataSet.setSelectionShift(0f);

        PieData piedata = new PieData(dataSet);
        piedata.setValueFormatter(new PercentFormatter());
        piedata.setValueTextSize(11f);
        piedata.setValueTextColor(Color.WHITE);
        mpieChart.setData(piedata);

        // undo all highlights
        mpieChart.highlightValues(null);

        mpieChart.invalidate();

        data.close();

    }

    private SpannableString generateCenterSpannableText() {

        SpannableString s = new SpannableString(getString(R.string.piechartspannabletext));
        s.setSpan(new RelativeSizeSpan(1.7f), 0, 14, 0);
        s.setSpan(new StyleSpan(Typeface.NORMAL), 14, s.length() - 15, 0);
        s.setSpan(new ForegroundColorSpan(Color.GRAY), 14, s.length() - 15, 0);
        s.setSpan(new RelativeSizeSpan(.8f), 14, s.length() - 15, 0);
        s.setSpan(new StyleSpan(Typeface.ITALIC), s.length() - 14, s.length(), 0);
        s.setSpan(new ForegroundColorSpan(ColorTemplate.getHoloBlue()), s.length() - 14, s.length(), 0);
        return s;
    }

    private void setupLineChart() {

        mlineChart.setDrawGridBackground(false);
        // enable touch gestures
        mlineChart.setTouchEnabled(true);

        // enable scaling and dragging
        mlineChart.setDragEnabled(true);
        mlineChart.setScaleEnabled(true);

        mlineChart.setPinchZoom(true);

        mlineChart.getAxisRight().setEnabled(false);

        mlineChart.animateX(2500);

        // get the legend (only possible after setting data)
        Legend l = mlineChart.getLegend();

    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        //Calendar c = Calendar.getInstance();

        //int month = c.get(Calendar.MONTH)+1;

        switch (id){
            case PIECHART_LOADER:

                Log.v(LOG_TAG, "getting linechart info for trimester: " + mTrimester);
                return new CursorLoader(
                        getActivity(),
                        DataContract.StatementEntry.buildStatsTrimUri(getStatsTrimester(getActivity())),
                        null,
                        null,
                        null,
                        null);

            case LINECHART_LOADER:
            /*
                Log.v(LOG_TAG, "getting piechart info for month: " + mTrimester);
                return new CursorLoader(
                        getActivity(),
                        DataContract.BudgetEntry.buildBudgetMonth(mTrimester),
                        null,
                        null,
                        null,
                        null); */
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        switch (loader.getId()) {
            case LINECHART_LOADER:

                Log.v(LOG_TAG, "onLoadFinish for pieChart loader called. data with: " + data.getCount());

                if (data != null && data.moveToFirst() && data.getCount() > 0) {
                    //Load piechart data
                    setLineCharData(data);
                }

                break;

            case PIECHART_LOADER:

                Log.v(LOG_TAG, "onLoadFinish for pieChart loader called. data with: " + data.getCount());

                if (data != null && data.moveToFirst() && data.getCount() > 0) {
                    //Load piechart data
                    setPieData(data);
                }

                break;
        }
    }

    private void setLineCharData(Cursor data) {

        ArrayList<Entry> values = new ArrayList<Entry>();

        String dateRaw = "";

        for (int i = 0; i < data.getCount(); i++) {

            dateRaw = String.valueOf(data.getInt(1));
            Log.v(LOG_TAG, "Value for i: " + i + " , " + Integer.parseInt(dateRaw.substring(6/8)));
            float val = (float) data.getDouble(2);
            values.add(new Entry(Integer.parseInt(dateRaw.substring(6/8)), val));
            data.moveToNext();
        }

        LineDataSet set1;

        if (mlineChart.getData() != null &&
                mlineChart.getData().getDataSetCount() > 0) {
            Log.v(LOG_TAG,"mlineChart is not null and bigger than 0");
            set1 = (LineDataSet) mlineChart.getData().getDataSetByIndex(0);
            set1.setValues(values);
            mlineChart.getData().notifyDataChanged();
            mlineChart.notifyDataSetChanged();
        } else {
            Log.v(LOG_TAG,"mlineChart is null creating dataset...");
            // create a dataset and give it a type
            set1 = new LineDataSet(values, "Spendings in June");

            // set the line to be drawn like this "- - - - - -"
            set1.enableDashedLine(10f, 5f, 0f);
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setColor(Color.BLACK);
            set1.setCircleColor(Color.BLACK);
            set1.setLineWidth(1f);
            set1.setCircleRadius(3f);
            set1.setDrawCircleHole(false);
            set1.setValueTextSize(9f);
            set1.setDrawFilled(true);

            set1.setFillColor(Color.BLACK);

            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            dataSets.add(set1); // add the datasets

            set1.setValues(values);

            // create a data object with the datasets
            LineData datav = new LineData(dataSets);

            // set data
            mlineChart.setData(datav);

        }
    }


}
