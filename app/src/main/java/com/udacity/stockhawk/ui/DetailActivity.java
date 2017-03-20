package com.udacity.stockhawk.ui;

import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.google.common.collect.Lists;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;

import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import au.com.bytecode.opencsv.CSVReader;
import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;

public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    @BindView(R.id.stock_history_chart)
    CombinedChart mChart;
    @BindColor(R.color.material_green_700)
    int colorGreen;
    @BindColor(R.color.material_red_700)
    int colorRed;

    public static final String EXTRA_SYMBOL = "EXTRA_SYMBOL";

    private static final int ITEM_COUNT = 10;

    private String mSymbol;

    private Typeface mTfLight;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_detail);
        ButterKnife.bind(this);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mSymbol = getIntent().getStringExtra(EXTRA_SYMBOL);
        setTitle(mSymbol);

        mChart.getDescription().setEnabled(false);
        mChart.setBackgroundColor(ContextCompat.getColor(this, R.color.material_grey_850_copy));
        mChart.setDrawGridBackground(false);
        mChart.setDrawBarShadow(false);
        mChart.setHighlightFullBarEnabled(false);

        // draw bars behind lines
        mChart.setDrawOrder(new CombinedChart.DrawOrder[]{
                CombinedChart.DrawOrder.BAR, CombinedChart.DrawOrder.LINE
        });

        mChart.getLegend().setEnabled(false);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setDrawGridLines(false);
        rightAxis.setAxisMinimum(0f);
        rightAxis.setTextColor(Color.WHITE);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(Color.WHITE);

        XAxis xAxis = mChart.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        xAxis.setPosition(XAxis.XAxisPosition.BOTH_SIDED);
        xAxis.setAxisMinimum(0f);
        xAxis.setGranularity(1f);
        xAxis.setAxisMaximum(ITEM_COUNT);

        mTfLight = Typeface.createFromAsset(getAssets(), "OpenSans-Light.ttf");

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private BarData generateBarData(final List<String> values) {
        ArrayList<BarEntry> entries = new ArrayList<>();

        for (int index = 0; index < values.size(); index++) {
            entries.add(new BarEntry(index + 0.5f, Float.valueOf(values.get(index))));
        }

        BarDataSet set = new BarDataSet(entries, null);
        set.setColor(colorGreen);
        set.setDrawValues(false);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);

        BarData d = new BarData(set);
        d.setValueTextSize(10f);
        d.setBarWidth(0.45f);

        return d;
    }

    private LineData generateLineData(final List<String> values) {
        LineData d = new LineData();

        ArrayList<Entry> entries = new ArrayList<>();

        for (int index = 0; index < values.size(); index++) {
            entries.add(new Entry(index + 0.5f, Float.valueOf(values.get(index))));
        }

        LineDataSet set = new LineDataSet(entries, null);
        set.setColor(colorRed);
        set.setLineWidth(2.5f);
        set.setCircleColor(colorRed);
        set.setCircleRadius(5f);
        set.setFillColor(colorRed);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawValues(true);
        set.setValueTextSize(10f);
        set.setValueTextColor(colorGreen);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);

        d.addDataSet(set);

        return d;
    }

    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        return new CursorLoader(this,
                Contract.Quote.makeUriForStock(mSymbol),
                new String[]{Contract.Quote.COLUMN_HISTORY},
                null, null, null);
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
        if (data != null && data.moveToFirst()) {
            String rawHistoryCsv = data.getString(data.getColumnIndex(Contract.Quote.COLUMN_HISTORY));
            CSVReader csvReader = new CSVReader(new StringReader(rawHistoryCsv));
            List<Date> dates = new ArrayList<>();
            List<String> values = new ArrayList<>();
            for (int i = 0; i < ITEM_COUNT; i++) {
                try {
                    String[] nextLine = csvReader.readNext();
                    if (nextLine != null) {
                        dates.add(new Date(Long.valueOf(nextLine[0])));
                        values.add(nextLine[1]);
                    }
                } catch (IOException | IndexOutOfBoundsException e) {
                    // parse data error, do nothing
                    return;
                }
            }

            final List<Date> orderedDates = Lists.reverse(dates);
            final List<String> orderedValues = Lists.reverse(values);

            XAxis xAxis = mChart.getXAxis();
            xAxis.setValueFormatter(new IAxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    if (value < orderedDates.size()) {
                        SimpleDateFormat df = new SimpleDateFormat("MMM d", Locale.US);
                        return df.format(orderedDates.get((int) value % orderedDates.size()));
                    } else {
                        return "";
                    }
                }
            });

            CombinedData combinedData = new CombinedData();

            combinedData.setData(generateLineData(orderedValues));
            combinedData.setData(generateBarData(orderedValues));

            combinedData.setValueTypeface(mTfLight);

            mChart.setData(combinedData);
            mChart.invalidate();
        }
    }

    @Override
    public void onLoaderReset(final Loader<Cursor> loader) {
        // do nothing
    }
}
