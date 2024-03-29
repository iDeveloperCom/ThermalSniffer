package com.ideveloper.thermalsniffer;

import static java.lang.Math.abs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.androidplot.ui.Anchor;
import com.androidplot.ui.HorizontalPositioning;
import com.androidplot.ui.VerticalPositioning;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ideveloper.thermalsniffer.data.WeatherData;
import com.ideveloper.thermalsniffer.data.WeatherDataWrapper;
import com.ideveloper.thermalsniffer.databinding.FragmentSecondBinding;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Locale;


@SuppressLint("MissingPermission")
public class SecondFragment extends Fragment implements DataPassListener, CustomSetupDialog.CustomSetupDialogListener {

    private boolean isRed = false;

    private final WeatherDataWrapper dataset = new WeatherDataWrapper();
    private FragmentSecondBinding binding;

    private LinearLayout temperatureHeatLayout;
    private View temperatureHeatCover;

    private XYPlot temperaturePlot;
    private XYPlot axisPlot;
    private XYPlot windPlot;

    private double pivotTemperature = 0;

    private int MAX_TIME_SENSITIVITY = 1200;
    private int MAX_TEMP_SENSITIVITY = 5;
    private int MIN_WIND_SENSITIVITY = 0;

    private float TIME_SENSITIVITY;
    private float TEMPERATURE_SENSITIVITY;
    private float WIND_SENSITIVITY;
    private boolean showInMinutes;
    private boolean showInFahrenheit;
    private boolean autoAdjustTemperatureRange;
    private int windCalibration;

    SimpleXYSeries temperatureSeries;
    SimpleXYSeries ambientTemperatureSeries;
    SimpleXYSeries windSeries;

    private ScaleGestureDetector mDomainScaleDetector;
    private ScaleGestureDetector mTemperatureScaleDetector;
    private ScaleGestureDetector mWindScaleDetector;

    private Button indicatorButton;

    private void writeChartPrefs() {
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("pref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putBoolean("TEMP-TYPE", showInFahrenheit);
        editor.putBoolean("TIME-TYPE", showInMinutes);
        editor.putBoolean("TEMP-AUTO", autoAdjustTemperatureRange);
        editor.putFloat("TIME-SENS", TIME_SENSITIVITY);
        editor.putFloat("TEMP-SENS", TEMPERATURE_SENSITIVITY);
        editor.putFloat("WIND-SENS", WIND_SENSITIVITY);
        editor.putInt("WIND-CALIBRATION", windCalibration);
        editor.apply();
    }

    private void readChartPrefs() {
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("pref", Context.MODE_PRIVATE);

        showInFahrenheit                = sharedPref.getBoolean("TEMP-TYPE", true);
        showInMinutes                   = sharedPref.getBoolean("TIME-TYPE", true);
        autoAdjustTemperatureRange      = sharedPref.getBoolean("TEMP-AUTO", false);
        TIME_SENSITIVITY                = sharedPref.getFloat("TIME-SENS", 360);
        TEMPERATURE_SENSITIVITY         = sharedPref.getFloat("TEMP-SENS", 2);
        WIND_SENSITIVITY                = sharedPref.getFloat("WIND-SENS", 4);
        windCalibration                 = sharedPref.getInt("WIND-CALIBRATION", 8);
    }

    private synchronized void redrawGradient(double percent) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(
                ()-> {
                    int height = temperatureHeatLayout.getHeight();
                    height = height - (int)( height * percent );
                    temperatureHeatCover.setLayoutParams(new LinearLayout.LayoutParams(temperatureHeatLayout.getWidth(), height));
                    temperatureHeatCover.invalidate();
                }
        );
    }

    private void gradientRecalculate(double deltaTemperature) {
        double percent = 0;
        if (deltaTemperature > 0) {
            if (deltaTemperature > TEMPERATURE_SENSITIVITY) {
                percent = 100;
            } else {
                percent = (deltaTemperature / TEMPERATURE_SENSITIVITY);
            }
        }
        redrawGradient(percent);
    }

    private final ScaleGestureDetector.OnScaleGestureListener mTemperatureScaleGestureListener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private float scaleFactor = 1;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            if (scaleFactor < 1) {
                scaleFactor = 1 + (1 - scaleFactor);
            } else {
                scaleFactor = (10 - scaleFactor) / 10;
            }
            TEMPERATURE_SENSITIVITY *= scaleFactor;
            TEMPERATURE_SENSITIVITY = (TEMPERATURE_SENSITIVITY < 1 ? 1 : TEMPERATURE_SENSITIVITY); // prevent our view from becoming too small //
            TEMPERATURE_SENSITIVITY = (TEMPERATURE_SENSITIVITY > MAX_TEMP_SENSITIVITY ? MAX_TEMP_SENSITIVITY : TEMPERATURE_SENSITIVITY); // prevent our view from becoming too small //
            adjustTemperatureRange(oldTempValue);
            temperaturePlot.redraw();
            writeChartPrefs();
        }
    };

    private final ScaleGestureDetector.OnScaleGestureListener mDomainScaleGestureListener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {

        private float scaleFactor = 1;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            scaleFactor = 1;
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            if (scaleFactor < 1) {
                scaleFactor = 1 + (1 - scaleFactor);
            } else {
                scaleFactor = (10 - scaleFactor) / 10;
            }

            TIME_SENSITIVITY = TIME_SENSITIVITY * scaleFactor;
            TIME_SENSITIVITY = (TIME_SENSITIVITY < 60 ? 60 : TIME_SENSITIVITY); // prevent our view from becoming too small //
            TIME_SENSITIVITY = (TIME_SENSITIVITY > MAX_TIME_SENSITIVITY ? MAX_TIME_SENSITIVITY : TIME_SENSITIVITY); // prevent our view from becoming too small //

            //noinspection IntegerDivisionInFloatingPointContext
            TIME_SENSITIVITY = ((int)TIME_SENSITIVITY / 10 ) * 10;

            adjustDomains();
            adjustSeries();
            temperaturePlot.redraw();
            windPlot.redraw();
            axisPlot.redraw();
            writeChartPrefs();
        }
    };

    private final ScaleGestureDetector.OnScaleGestureListener mWindScaleGestureListener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {

        private float scaleFactor = 1;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            if (scaleFactor < 1) {
                scaleFactor = 1 + (1 - scaleFactor);
            } else {
                scaleFactor = (10 - scaleFactor) / 10;
            }

            WIND_SENSITIVITY *= scaleFactor;
            WIND_SENSITIVITY = (WIND_SENSITIVITY < (MIN_WIND_SENSITIVITY+1) ? (MIN_WIND_SENSITIVITY+1) : WIND_SENSITIVITY); // prevent our view from becoming too small //
            int MAX_WIND_SENSITIVITY = 12;
            WIND_SENSITIVITY = (WIND_SENSITIVITY > MAX_WIND_SENSITIVITY ? MAX_WIND_SENSITIVITY : WIND_SENSITIVITY); // prevent our view from becoming too small //
            adjustWindRange();
            windPlot.redraw();
            writeChartPrefs();
        }
    };

    private void adjustTemperatureRange(double temperature) {
        double lowerBoundary, upperBoundary;
        if (!autoAdjustTemperatureRange) {
            if (temperature > pivotTemperature + TEMPERATURE_SENSITIVITY) {
                upperBoundary = Math.round(temperature + 0.5);
                lowerBoundary = temperature - (2*TEMPERATURE_SENSITIVITY);
                pivotTemperature = temperature - TEMPERATURE_SENSITIVITY;
            } else if (temperature < pivotTemperature - TEMPERATURE_SENSITIVITY) {
                lowerBoundary = temperature;
                upperBoundary = temperature + (2*TEMPERATURE_SENSITIVITY);
                pivotTemperature = temperature + TEMPERATURE_SENSITIVITY;
            } else {
                lowerBoundary = pivotTemperature - TEMPERATURE_SENSITIVITY;
                upperBoundary = pivotTemperature + TEMPERATURE_SENSITIVITY;
            }
        } else {
            TEMPERATURE_SENSITIVITY = (int) Math.round(abs(temperature - pivotTemperature) + 0.5);
            lowerBoundary = pivotTemperature - TEMPERATURE_SENSITIVITY;
            upperBoundary = pivotTemperature + TEMPERATURE_SENSITIVITY;
        }
        temperaturePlot.setRangeBoundaries(lowerBoundary, upperBoundary, BoundaryMode.FIXED);
        temperaturePlot.setRangeStep(StepMode.INCREMENT_BY_VAL, 1);
    }

    private void adjustDomains() {
        windPlot.setDomainBoundaries(0, TIME_SENSITIVITY*2, BoundaryMode.FIXED);
        temperaturePlot.setDomainBoundaries(0, TIME_SENSITIVITY*2, BoundaryMode.FIXED);
        axisPlot.setDomainBoundaries(0, TIME_SENSITIVITY, BoundaryMode.FIXED);

        temperaturePlot.setUserDomainOrigin(TIME_SENSITIVITY*2);
        windPlot.setUserDomainOrigin(TIME_SENSITIVITY*2);
        axisPlot.setUserDomainOrigin(TIME_SENSITIVITY*2);

        int steps = 20;

        if (TIME_SENSITIVITY > 1080) steps = 120; else
        if (TIME_SENSITIVITY > 960) steps = 100; else
        if (TIME_SENSITIVITY > 840) steps = 80; else
        if (TIME_SENSITIVITY > 720) steps = 70; else
        if (TIME_SENSITIVITY > 600) steps = 60; else
        if (TIME_SENSITIVITY > 480) steps = 50; else
        if (TIME_SENSITIVITY > 360) steps = 40; else
        if (TIME_SENSITIVITY > 240) steps = 30;
        axisPlot.setDomainStep(StepMode.INCREMENT_BY_VAL, steps);
    }

    private void adjustSeries() {

        while (temperatureSeries.size() < (2*TIME_SENSITIVITY) ) {
            temperatureSeries.addFirst(null, null);
            ambientTemperatureSeries.addFirst(null, null);
            windSeries.addFirst(null, null);
        }

        while (temperatureSeries.size() > (2*TIME_SENSITIVITY) ) {
            temperatureSeries.removeFirst();
            ambientTemperatureSeries.removeFirst();
            windSeries.removeFirst();
        }
    }

    private void adjustWindRange() {
        windPlot.setRangeBoundaries(MIN_WIND_SENSITIVITY, WIND_SENSITIVITY, BoundaryMode.FIXED);
        int step = 1;
        if (WIND_SENSITIVITY >= 2) {
            step = 2;
        }
        if (WIND_SENSITIVITY >= 4) {
            step = 4;
        }
        windPlot.setRangeStep(StepMode.INCREMENT_BY_VAL, step);
    }

    private void plotOptionsForTemperature() {
        temperaturePlot = requireView().findViewById(R.id.temperatureChart);
        temperatureHeatCover = requireView().findViewById(R.id.temperatureHeatChart1);
        temperatureHeatLayout = requireView().findViewById(R.id.temperatureHeatChart);

        temperaturePlot.setTitle("can");
        temperaturePlot.getGraph().setLineLabelEdges(XYGraphWidget.Edge.RIGHT);
        temperaturePlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).getPaint().setColor(Color.TRANSPARENT);
        temperaturePlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.RIGHT).getPaint().setColor(Color.RED);
        temperaturePlot.getGraph().setMargins(0, 20, 100, 0);

        DashPathEffect dashFx = new DashPathEffect(new float[] {PixelUtils.dpToPix(1), PixelUtils.dpToPix(8)}, 0);
        temperaturePlot.getGraph().getDomainGridLinePaint().setAlpha(0);
        temperaturePlot.getGraph().getRangeGridLinePaint().setPathEffect(dashFx);
        temperaturePlot.getGraph().getGridBackgroundPaint().setColor(0xFFEFD5);
        temperaturePlot.setDomainStep(StepMode.INCREMENT_BY_VAL, 0.5);

        mTemperatureScaleDetector = new ScaleGestureDetector(getContext(), mTemperatureScaleGestureListener);
        temperaturePlot.setOnTouchListener((v, event) -> mTemperatureScaleDetector.onTouchEvent(event));

        temperaturePlot.getTitle().position(150, HorizontalPositioning.ABSOLUTE_FROM_RIGHT, 5, VerticalPositioning.ABSOLUTE_FROM_TOP, Anchor.RIGHT_TOP);
    }

    private void plotOptionsForAxis() {
        axisPlot = requireView().findViewById(R.id.axisChart);
        axisPlot.setRangeBoundaries(0, 0, BoundaryMode.FIXED);
        axisPlot.getGraph().setMargins(0, 0, 25, 0);
        axisPlot.getGraph().getGridBackgroundPaint().setColor(0xFFEFD5);
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
                String strDouble = String.format(Locale.getDefault(), "%.0f", i);
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

        mDomainScaleDetector = new ScaleGestureDetector(getContext(), mDomainScaleGestureListener);
        axisPlot.setOnTouchListener((v, event) -> mDomainScaleDetector.onTouchEvent(event));
    }

    private void plotOptionsForWind() {
        windPlot = requireView().findViewById(R.id.windChart);
        windPlot.getGraph().setLineLabelEdges(XYGraphWidget.Edge.RIGHT);
        windPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).getPaint().setColor(Color.TRANSPARENT);
        windPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.RIGHT).getPaint().setColor(Color.BLUE);
        windPlot.getGraph().setMargins(0, 15, 100, 0);

        DashPathEffect dashFx = new DashPathEffect(new float[] {PixelUtils.dpToPix(1), PixelUtils.dpToPix(8)}, 0);
        windPlot.getGraph().getDomainGridLinePaint().setAlpha(0);
        windPlot.getGraph().getRangeGridLinePaint().setPathEffect(dashFx);
        windPlot.getGraph().getGridBackgroundPaint().setColor(0xFFEFD5);
        windPlot.setDomainStep(StepMode.INCREMENT_BY_VAL, 0.5);

        mWindScaleDetector = new ScaleGestureDetector(getContext(), mWindScaleGestureListener);
        windPlot.setOnTouchListener((v, event) -> mWindScaleDetector.onTouchEvent(event));
    }

    private void setupPlots() {

        plotOptionsForTemperature();
        plotOptionsForAxis();
        plotOptionsForWind ();
        adjustDomains();

        temperatureSeries = new SimpleXYSeries("Temperature");
        temperatureSeries.useImplicitXVals();
        ambientTemperatureSeries = new SimpleXYSeries("Average Temperature");
        ambientTemperatureSeries.useImplicitXVals();
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

        temperaturePlot.addSeries(ambientTemperatureSeries, ambientTemperatureSeriesFormat);
        temperaturePlot.addSeries(temperatureSeries, temperatureSeriesFormat);
        windPlot.addSeries(windSeries, windSeriesFormat);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSecondBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        readChartPrefs();
        setupPlots();

        indicatorButton = view.findViewById(R.id.indicator);
        indicatorButton.setOnClickListener(v -> {
            oldDataExist = false;

            temperaturePlot.removeSeries(temperatureSeries);
            temperaturePlot.removeSeries(ambientTemperatureSeries);
            temperatureSeries = null;
            ambientTemperatureSeries = null;
            windPlot.removeSeries(windSeries);
            windSeries = null;

            temperaturePlot.clear();
            windPlot.clear();
            setupPlots();
        });

        FloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(view1 -> {

            FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
            Fragment prev = getActivity().getSupportFragmentManager().findFragmentByTag("dialog");
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);

            // Create and show the dialog.
            CustomSetupDialog newFragment = CustomSetupDialog.newInstance(MAX_TIME_SENSITIVITY, MAX_TEMP_SENSITIVITY, MIN_WIND_SENSITIVITY, windCalibration, showInFahrenheit, showInMinutes, autoAdjustTemperatureRange);
            newFragment.setListener(SecondFragment.this);
            newFragment.show(ft, "dialog");
        });
    }

    public void onStart() {
        super.onStart();
        setMainActive();
    }

    @Override
    public void onResume() {
        super.onResume();
        writeChartPrefs();
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

    private float oldTempValue = 0;
    private float oldWindValue = 0;
    private boolean oldDataExist = false;

    @Override
    public synchronized void passData(long deviceID, float temperature, float wind) {
        if (!isRed) {
            indicatorButton.setBackgroundColor(Color.RED);

            isRed = true;
        } else {
            indicatorButton.setBackgroundColor(0x0EB277);

            isRed = false;
        }
        indicatorButton.invalidate();

        String einheit = " °F";

        if (!showInFahrenheit) {
            temperature =  (temperature - 32) * 5/9;
            einheit = " °C";
        }

        if (!oldDataExist) {
            oldDataExist = true;
            oldWindValue = wind;
            oldTempValue = temperature;
            pivotTemperature = temperature;
            adjustTemperatureRange(temperature);
            adjustWindRange();
        } else {
            if ( abs( oldTempValue - temperature ) > 2 )
                temperature = oldTempValue;
            else oldTempValue = temperature;
            if ( abs(oldWindValue - wind) > 2 )
                wind = oldWindValue;
            else
                oldWindValue = wind;
        }

        wind = wind / windCalibration;
        WeatherData data = new WeatherData((double) temperature, (double) wind);
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
            adjustTemperatureRange(temperature);
        }
        String strDouble = String.format("%.2f", temperature);
        temperaturePlot.setTitle(strDouble+einheit);
        temperaturePlot.redraw();
        windPlot.redraw();
        gradientRecalculate(data.getTemperature() - data.getMeanTemperature());
    }

    @Override
    public void onFinishSetupDialog(int maxTimeSensitivity, int maxTempSensitivity, int minWindSensitivity, int windCalibration, boolean showInFahrenheit, boolean showInMinutes, boolean autoAdjustTemperatureRange) {
        MAX_TEMP_SENSITIVITY = maxTempSensitivity;
        MAX_TIME_SENSITIVITY = maxTimeSensitivity;
        MIN_WIND_SENSITIVITY = minWindSensitivity;
        this.windCalibration = windCalibration;
        this.showInFahrenheit = showInFahrenheit;
        this.showInMinutes = showInMinutes;
        this.autoAdjustTemperatureRange = autoAdjustTemperatureRange;
        writeChartPrefs();
        axisPlot.redraw();
        adjustWindRange();
        windPlot.redraw();
    }
}

