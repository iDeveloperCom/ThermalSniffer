package com.ideveloper.thermalsniffer;

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

import java.util.Objects;

@SuppressLint("MissingPermission")
public class SecondFragment extends Fragment implements DataPassListener {

    private final WeatherDataWrapper dataset = new WeatherDataWrapper();
    private FragmentSecondBinding binding;

    XYPlot temperaturePlot;
    XYPlot axisPlot;
    XYPlot windPlot;

    int TIME_SENSITIVITY = 100;

    SimpleXYSeries temperatureSeries;
    SimpleXYSeries ambientTemperatureSeries;
    SimpleXYSeries windSeries;


    private void plotOptionsForTemperature() {
        temperaturePlot = requireView().findViewById(R.id.temperatureChart);
        temperaturePlot.setDomainBoundaries(0, TIME_SENSITIVITY, BoundaryMode.FIXED);
        temperaturePlot.setRangeBoundaries(0, 100, BoundaryMode.FIXED);
        temperaturePlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).getPaint().setColor(Color.TRANSPARENT);
        temperaturePlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).getPaint().setColor(Color.RED);
        temperaturePlot.getGraph().setMargins(20, 15, 5, 0);

        DashPathEffect dashFx = new DashPathEffect(new float[] {PixelUtils.dpToPix(1), PixelUtils.dpToPix(8)}, 0);
        temperaturePlot.getGraph().getDomainGridLinePaint().setPathEffect(dashFx);
        temperaturePlot.getGraph().getRangeGridLinePaint().setPathEffect(dashFx);
        temperaturePlot.getGraph().getGridBackgroundPaint().setColor(0xFFEFD5);

        temperaturePlot.setRangeStep(StepMode.INCREMENT_BY_VAL, 10);
        temperaturePlot.setDomainStep(StepMode.INCREMENT_BY_VAL, 20);

    }

    private void plotOptionsForAxis() {
        axisPlot = requireView().findViewById(R.id.axisChart);
        axisPlot.setDomainBoundaries(0, TIME_SENSITIVITY, BoundaryMode.FIXED);
        axisPlot.setRangeBoundaries(0, 0, BoundaryMode.FIXED);
        axisPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).getPaint().setColor(Color.BLACK);
        axisPlot.getGraph().setMargins(20, 0, 5, 0);
        axisPlot.getGraph().getGridBackgroundPaint().setColor(0xFFEFD5);

        axisPlot.setDomainStep(StepMode.INCREMENT_BY_VAL, 20);
    }

    private void plotOptionsForWind() {
        windPlot = requireView().findViewById(R.id.windChart);
        windPlot.setDomainBoundaries(0, TIME_SENSITIVITY, BoundaryMode.FIXED);
        windPlot.setRangeBoundaries(0, 8, BoundaryMode.FIXED);
        windPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).getPaint().setColor(Color.TRANSPARENT);
        windPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).getPaint().setColor(Color.BLUE);
        windPlot.getGraph().setMargins(20, 15, 5, 0);
        DashPathEffect dashFx = new DashPathEffect(new float[] {PixelUtils.dpToPix(1), PixelUtils.dpToPix(8)}, 0);
        windPlot.getGraph().getDomainGridLinePaint().setPathEffect(dashFx);
        windPlot.getGraph().getRangeGridLinePaint().setPathEffect(dashFx);
        windPlot.getGraph().getGridBackgroundPaint().setColor(0xFFEFD5);

        windPlot.setRangeStep(StepMode.INCREMENT_BY_VAL, 2);
        windPlot.setDomainStep(StepMode.INCREMENT_BY_VAL, 20);
    }

    private void setupPlots() {
        plotOptionsForTemperature();
        plotOptionsForAxis();
        plotOptionsForWind ();

        temperatureSeries = new SimpleXYSeries("Temperature");
        temperatureSeries.useImplicitXVals();

        for (int i = 0; i < TIME_SENSITIVITY; i++) {
            temperatureSeries.addFirst(null, null);
        }
        LineAndPointFormatter seriesFormat = new LineAndPointFormatter(
                Color.RED,
                null,
                null,
                null
        );
        temperaturePlot.addSeries(temperatureSeries, seriesFormat);
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

    @Override
    public synchronized void passData(long deviceID, float temperature, float wind) {
        WeatherData data = new WeatherData((double) temperature, (double) wind);
        dataset.addNewData(data);

        temperatureSeries.addLast(null, temperature);

        if (temperatureSeries.size() > TIME_SENSITIVITY) {
            temperatureSeries.removeFirst();
        }
        temperaturePlot.redraw();


        System.out.print(" Average Temperature = ");
        System.out.print(dataset.getLastData().getMeanTemperature());
        System.out.print(" Temperature = ");
        System.out.print(temperature);
        System.out.print(" Wind = ");
        System.out.println(wind);
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

