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

public class TemperatureChartView extends FrameLayout {

    private LineChart chart;
    private List<Entry> temperatureEntries;
    private SimpleDateFormat timeFormat;

    public TemperatureChartView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public TemperatureChartView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TemperatureChartView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_temperature_chart, this, true);

        chart = findViewById(R.id.temperature_chart);
        temperatureEntries = new ArrayList<>();
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

        // Setup Y axis (temperature)
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(Color.GREEN);
        leftAxis.setAxisMinimum(34f);
        leftAxis.setAxisMaximum(41f);
        leftAxis.setDrawGridLines(true);

        // Disable right Y axis
        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        // Setup legend
        Legend legend = chart.getLegend();
        legend.setForm(Legend.LegendForm.LINE);

        // Create empty data
        LineData data = new LineData();
        data.setValueTextColor(Color.BLACK);
        chart.setData(data);

        // Add danger zones for temperature (horizontal lines at 35°C and 39°C)
        // This would be implemented with limit lines in a real app

        // Refresh chart
        chart.invalidate();
    }

    public void addDataPoint(long timestamp, double temperature) {
        LineData data = chart.getData();

        if (data == null) {
            data = new LineData();
            chart.setData(data);
        }

        // Add temperature entry
        temperatureEntries.add(new Entry(timestamp, (float) temperature));
        if (data.getDataSetByIndex(0) == null) {
            // Create temperature dataset if it doesn't exist
            LineDataSet temperatureSet = createTemperatureDataSet();
            data.addDataSet(temperatureSet);
        } else {
            // Update existing dataset
            data.getDataSetByIndex(0).clear();
            for (Entry entry : temperatureEntries) {
                data.getDataSetByIndex(0).addEntry(entry);
            }
        }

        // Limit data points to keep performance optimal
        if (temperatureEntries.size() > 100) {
            temperatureEntries.remove(0);
        }

        // Notify data changed
        data.notifyDataChanged();
        chart.notifyDataSetChanged();

        // Scroll to end to show latest data
        chart.moveViewToX(timestamp);

        // Refresh chart
        chart.invalidate();
    }

    private LineDataSet createTemperatureDataSet() {
        LineDataSet set = new LineDataSet(temperatureEntries, "Temperature (°C)");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(Color.GREEN);
        set.setCircleColor(Color.GREEN);
        set.setLineWidth(2f);
        set.setCircleRadius(3f);
        set.setFillAlpha(65);
        set.setFillColor(Color.GREEN);
        set.setHighLightColor(Color.rgb(117, 244, 117));
        set.setDrawCircleHole(false);
        return set;
    }

    public void clearData() {
        temperatureEntries.clear();
        chart.clear();
        chart.invalidate();
    }
}