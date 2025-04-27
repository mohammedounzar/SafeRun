package com.example.saferun.ui.charts;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.saferun.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HeartRateSpeedChartView extends FrameLayout {

    private LineChart chart;
    private List<Entry> heartRateEntries;
    private List<Entry> speedEntries;
    private SimpleDateFormat timeFormat;

    public HeartRateSpeedChartView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public HeartRateSpeedChartView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HeartRateSpeedChartView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_heart_rate_speed_chart, this, true);
        chart = findViewById(R.id.heart_rate_speed_chart);

        heartRateEntries = new ArrayList<>();
        speedEntries = new ArrayList<>();
        timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    }

    public void initChart() {
        // Setup chart
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.WHITE);

        // Setup X axis (time)
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                // Convert timestamp to readable time
                return timeFormat.format(new Date((long) value));
            }
        });

        // Setup left Y axis (heart rate)
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(Color.RED);
        leftAxis.setAxisMinimum(40f);
        leftAxis.setAxisMaximum(200f);
        leftAxis.setDrawGridLines(false);

        // Setup right Y axis (speed)
        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setTextColor(Color.BLUE);
        rightAxis.setAxisMinimum(0f);
        rightAxis.setAxisMaximum(25f);
        rightAxis.setDrawGridLines(false);

        // Setup legend
        Legend legend = chart.getLegend();
        legend.setForm(Legend.LegendForm.LINE);

        // Create empty data
        LineData data = new LineData();
        data.setValueTextColor(Color.BLACK);
        chart.setData(data);

        // Refresh chart
        chart.invalidate();
    }

    public void addDataPoint(long timestamp, int heartRate, double speed) {
        LineData data = chart.getData();

        if (data == null) {
            data = new LineData();
            chart.setData(data);
        }

        // Add heart rate entry
        heartRateEntries.add(new Entry(timestamp, heartRate));
        if (data.getDataSetByIndex(0) == null) {
            // Create heart rate dataset if it doesn't exist
            LineDataSet heartRateSet = createHeartRateDataSet();
            data.addDataSet(heartRateSet);
        } else {
            // Update existing dataset
            data.getDataSetByIndex(0).clear();
            for (Entry entry : heartRateEntries) {
                data.getDataSetByIndex(0).addEntry(entry);
            }
        }

        // Add speed entry
        speedEntries.add(new Entry(timestamp, (float) speed));
        if (data.getDataSetByIndex(1) == null) {
            // Create speed dataset if it doesn't exist
            LineDataSet speedSet = createSpeedDataSet();
            data.addDataSet(speedSet);
        } else {
            // Update existing dataset
            data.getDataSetByIndex(1).clear();
            for (Entry entry : speedEntries) {
                data.getDataSetByIndex(1).addEntry(entry);
            }
        }

        // Limit data points to keep performance optimal
//        if (heartRateEntries.size() > 100) {  // 100
//            heartRateEntries.remove(0);
//        }
//        if (speedEntries.size() > 100) {  // 100
//            speedEntries.remove(0);
//        }

        // Notify data changed
        data.notifyDataChanged();
        chart.notifyDataSetChanged();

        // Scroll to end to show latest data
        chart.moveViewToX(timestamp);

        // Refresh chart
        chart.invalidate();
    }

    private LineDataSet createHeartRateDataSet() {
        LineDataSet set = new LineDataSet(heartRateEntries, "Heart Rate (bpm)");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(Color.RED);
        set.setCircleColor(Color.RED);
        set.setLineWidth(2f);
        set.setCircleRadius(3f);
        set.setFillAlpha(65);
        set.setFillColor(Color.RED);
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setDrawCircleHole(false);
        return set;
    }

    private LineDataSet createSpeedDataSet() {
        LineDataSet set = new LineDataSet(speedEntries, "Speed (km/h)");
        set.setAxisDependency(YAxis.AxisDependency.RIGHT);
        set.setColor(Color.BLUE);
        set.setCircleColor(Color.BLUE);
        set.setLineWidth(2f);
        set.setCircleRadius(3f);
        set.setFillAlpha(65);
        set.setFillColor(Color.BLUE);
        set.setHighLightColor(Color.rgb(117, 117, 244));
        set.setDrawCircleHole(false);
        return set;
    }

    public void clearData() {
        heartRateEntries.clear();
        speedEntries.clear();
        chart.clear();
        chart.invalidate();
    }
}