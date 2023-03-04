package com.ideveloper.thermalsniffer.data;

import com.ideveloper.thermalsniffer.MainActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WeatherDataWrapper {


    private final List<WeatherData> dataMap = new ArrayList<>();
    private double exponentialMovingAverage = 0;

    public synchronized void addNewData(WeatherData data) {
        dataMap.add(data);
        int pos = dataMap.size() - 1;
        double mean = doEMA(data);
        data.setMeanTemperature(mean);
        dataMap.add(pos, data);
    }

    public WeatherData getLastData() {
        if (dataMap.isEmpty())
            return null;
        return dataMap.get(dataMap.size()-1);
    }

    private synchronized double doEMA(WeatherData data) { // Exponential Moving Average
            if (Objects.requireNonNull(dataMap.size()) > 0) {
                double numberOfData = Objects.requireNonNull(dataMap.size()) + 1;
                double smoothing = 2 / numberOfData;
                double dta = (data.getTemperature());
                exponentialMovingAverage = (dta * smoothing) + ( exponentialMovingAverage * (1-smoothing));
            }
        return exponentialMovingAverage;
    }

    private synchronized double doMean() {
        double mean = 0;
        int start, end;
        int pos = dataMap.size() - 1;
        if (pos < MainActivity.MEAN_TEMP_TIME) {
            start = 0;
        } else {
            start = (pos - MainActivity.MEAN_TEMP_TIME);
        }
        end = pos;
        for (int i = start; i < end; i++ ) {
            mean += dataMap.get(i).getTemperature();
        }
        double m = mean / ( end - start);

        return m;
    }
  }
