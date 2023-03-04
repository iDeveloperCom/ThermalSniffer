package com.ideveloper.thermalsniffer;

import static java.lang.Math.abs;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.ideveloper.thermalsniffer.data.WeatherData;
import com.ideveloper.thermalsniffer.data.WeatherDataWrapper;
import com.ideveloper.thermalsniffer.databinding.FragmentSecondBinding;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Locale;

@SuppressLint("MissingPermission")
public class SecondFragment extends Fragment implements DataPassListener {

    private final WeatherDataWrapper dataset = new WeatherDataWrapper();
    private FragmentSecondBinding binding;

    XYPlot temperaturePlot;
    XYPlot axisPlot;
    XYPlot windPlot;

    private WeatherData oldData = null;
    private double pivotTemperature = 0;

    private int TIME_SENSITIVITY = 180;
    private int TEMPERATURE_SENSITIVITY = 2;
    private int WIND_SENSITIVITY = 4;
    private boolean showInMinutes = true;
    private boolean showInFahrenheit = false;
    private int windCalibration = 10;

    SimpleXYSeries temperatureSeries;
    SimpleXYSeries ambientTemperatureSeries;
    SimpleXYSeries windSeries;

    @Override
    public synchronized void passData(long deviceID, float temperature, float wind) {
        if (!showInFahrenheit) {
            temperature =  (temperature - 32) * 5/9;
        }
        wind = wind / windCalibration;
        WeatherData data = new WeatherData((double) temperature, (double) wind);
        if ( oldData == null ) {
            oldData = data;
            pivotTemperature = temperature;
            adjustTemperatureRange();
            adjustWindRange();
        }
        if ( (abs(temperature - oldData.getTemperature()) > 3) || (abs(wind - oldData.getWind()) > 5) ) { // TRASH DATA REPLACE WITH ONE PREVIOUS
            data = oldData;
        } else {
            oldData = data;
        }
        dataset.addNewData(data);

        temperatureSeries.addLast(null, temperature);
        ambientTemperatureSeries.addLast(null, data.getMeanTemperature());
        windSeries.addLast(null, wind);
        while (temperatureSeries.size() > (2*TIME_SENSITIVITY) ) {
            temperatureSeries.removeFirst();
            ambientTemperatureSeries.removeFirst();
            windSeries.removeFirst();
        }

        if ( (temperature > (pivotTemperature + TEMPERATURE_SENSITIVITY)) || (temperature < (pivotTemperature - TEMPERATURE_SENSITIVITY)) ){
            pivotTemperature = temperature;
            adjustTemperatureRange();
        }
        temperaturePlot.redraw();
        windPlot.redraw();
    }

    private void adjustTemperatureRange() {
        temperaturePlot.setRangeBoundaries(pivotTemperature - TEMPERATURE_SENSITIVITY, pivotTemperature + TEMPERATURE_SENSITIVITY, BoundaryMode.FIXED);
        temperaturePlot.setRangeStep(StepMode.INCREMENT_BY_VAL, 1);
        pivotTemperature = oldData.getTemperature();
    }

    private void adjustWindRange() {
        windPlot.setRangeBoundaries(0, WIND_SENSITIVITY, BoundaryMode.FIXED);
        windPlot.setRangeStep(StepMode.INCREMENT_BY_VAL, 2);

    }

    private void plotOptionsForTemperature() {
        temperaturePlot = requireView().findViewById(R.id.temperatureChart);
        temperaturePlot.setDomainBoundaries(0, TIME_SENSITIVITY*2, BoundaryMode.FIXED);
        temperaturePlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).getPaint().setColor(Color.TRANSPARENT);
        temperaturePlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).getPaint().setColor(Color.RED);
        temperaturePlot.getGraph().setMargins(20, 15, 5, 0);

        DashPathEffect dashFx = new DashPathEffect(new float[] {PixelUtils.dpToPix(1), PixelUtils.dpToPix(8)}, 0);
        temperaturePlot.getGraph().getDomainGridLinePaint().setAlpha(0);
        temperaturePlot.getGraph().getRangeGridLinePaint().setPathEffect(dashFx);
        temperaturePlot.getGraph().getGridBackgroundPaint().setColor(0xFFEFD5);
        temperaturePlot.setDomainStep(StepMode.INCREMENT_BY_VAL, 0.5);
    }

    private void plotOptionsForAxis() {
        axisPlot = requireView().findViewById(R.id.axisChart);
        axisPlot.setDomainBoundaries(0, TIME_SENSITIVITY, BoundaryMode.FIXED);
        axisPlot.setRangeBoundaries(0, 0, BoundaryMode.FIXED);
        axisPlot.getGraph().setMargins(20, 0, 5, 0);
        axisPlot.getGraph().getGridBackgroundPaint().setColor(0xFFEFD5);
        axisPlot.setDomainStep(StepMode.INCREMENT_BY_VAL, 20);
        axisPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).getPaint().setColor(Color.BLACK);
        axisPlot.getGraph().getDomainGridLinePaint().setAlpha(0);
        axisPlot.getGraph().getRangeOriginLinePaint().setAlpha(0);
        axisPlot.getGraph().getDomainOriginLinePaint().setAlpha(0);

        axisPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new Format() {
            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                // obj contains the raw Number value representing the position of the label being drawn.
                // customize the labeling however you want here:
                float i = (TIME_SENSITIVITY - ((Number) obj).floatValue());
                String strDouble = String.format("%.0f", i);
                if (showInMinutes) {
                    int minute = (int)i / 60;
                    int reminder = (int) i - (minute*60);
                    strDouble = String.format(Locale.getDefault(), "%d:%d", minute, reminder);
                }
                return toAppendTo.append( i == 0 ? "Now":strDouble);
            }

            @Override
            public Object parseObject(String source, ParsePosition pos) {
                // unused
                return null;
            }
        });

    }

    private void plotOptionsForWind() {
        windPlot = requireView().findViewById(R.id.windChart);
        windPlot.setDomainBoundaries(0, TIME_SENSITIVITY*2, BoundaryMode.FIXED);
        windPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).getPaint().setColor(Color.TRANSPARENT);
        windPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).getPaint().setColor(Color.BLUE);
        windPlot.getGraph().setMargins(20, 15, 5, 0);
        DashPathEffect dashFx = new DashPathEffect(new float[] {PixelUtils.dpToPix(1), PixelUtils.dpToPix(8)}, 0);
        windPlot.getGraph().getDomainGridLinePaint().setAlpha(0);
        windPlot.getGraph().getRangeGridLinePaint().setPathEffect(dashFx);
        windPlot.getGraph().getGridBackgroundPaint().setColor(0xFFEFD5);
        windPlot.setDomainStep(StepMode.INCREMENT_BY_VAL, 0.5);
    }

    private void setupPlots() {
        plotOptionsForTemperature();
        plotOptionsForAxis();
        plotOptionsForWind ();

        temperatureSeries = new SimpleXYSeries("Temperature");
        temperatureSeries.useImplicitXVals();
        ambientTemperatureSeries = new SimpleXYSeries("Average Temperature");
        ambientTemperatureSeries.useImplicitXVals();;
        windSeries = new SimpleXYSeries("Wind");
        windSeries.useImplicitXVals();
        for (int i = 0; i < TIME_SENSITIVITY*2; i++) {
            temperatureSeries.addFirst(null, null);
            ambientTemperatureSeries.addFirst(null, null);
            windSeries.addFirst(null, null);
        }
        LineAndPointFormatter temperatureSeriesFormat = new LineAndPointFormatter(Color.RED, null, null,null);
        temperatureSeriesFormat.getLinePaint().setStrokeWidth(3);
        LineAndPointFormatter ambientTemperatureSeriesFormat = new LineAndPointFormatter(Color.LTGRAY, null, null,null);
        LineAndPointFormatter windSeriesFormat = new LineAndPointFormatter(Color.BLUE, null, null,null);
        windSeriesFormat.getLinePaint().setStrokeWidth(2);
        temperaturePlot.addSeries(temperatureSeries, temperatureSeriesFormat);
        temperaturePlot.addSeries(ambientTemperatureSeries, ambientTemperatureSeriesFormat);
        windPlot.addSeries(windSeries, windSeriesFormat);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSecondBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupPlots();
    }

    public void onStart() {
        super.onStart();
        setMainActive();
    }

    @Override
    public void onResume() {
        super.onResume();
        setMainActive();
    }

    @Override
    public void onPause() {
        super.onPause();
        setMainInactive();
    }

    @Override
    public void onDestroyView() {
        setMainInactive();
        super.onDestroyView();
        binding = null;
    }

    private void setMainActive() {
        MainActivity main = (MainActivity) getActivity();
        assert main != null;
        main.setSecondFragment(this);
    }

    private void setMainInactive() {
        MainActivity main = (MainActivity) getActivity();
        assert main != null;
        main.setSecondFragment(null);
    }
}

