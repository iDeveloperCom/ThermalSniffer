package com.ideveloper.thermalsniffer.data;

public class WeatherData {

    private final Double temperature;
    private final Double wind;

   private Double meanTemperature;

    public WeatherData(Double temperature, Double wind) {
        this.temperature = temperature;
        this.wind = wind;
    }

    public Double getTemperature() {
        return temperature;
    }

    public Double getWind() {
        return wind;
    }

    public Double getMeanTemperature() {
        return meanTemperature;
    }

    public void setMeanTemperature(Double meanTemperature) {
        this.meanTemperature = meanTemperature;
    }

}
