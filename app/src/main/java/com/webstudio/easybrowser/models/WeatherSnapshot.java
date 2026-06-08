package com.webstudio.easybrowser.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class WeatherSnapshot {
    private final String locationName;
    private final double latitude;
    private final double longitude;
    private final double temperature;
    private final double feelsLike;
    private final int humidity;
    private final double windSpeed;
    private final int weatherCode;
    private final String condition;
    private final String sunrise;
    private final String sunset;
    private final long fetchedAt;
    private final List<ForecastDay> forecastDays;

    public WeatherSnapshot(String locationName, double latitude, double longitude,
                           double temperature, double feelsLike, int humidity,
                           double windSpeed, int weatherCode, String condition,
                           String sunrise, String sunset, long fetchedAt,
                           List<ForecastDay> forecastDays) {
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.temperature = temperature;
        this.feelsLike = feelsLike;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.weatherCode = weatherCode;
        this.condition = condition;
        this.sunrise = sunrise;
        this.sunset = sunset;
        this.fetchedAt = fetchedAt;
        this.forecastDays = forecastDays == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(forecastDays));
    }

    public String getLocationName() {
        return locationName;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getFeelsLike() {
        return feelsLike;
    }

    public int getHumidity() {
        return humidity;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public int getWeatherCode() {
        return weatherCode;
    }

    public String getCondition() {
        return condition;
    }

    public String getSunrise() {
        return sunrise;
    }

    public String getSunset() {
        return sunset;
    }

    public long getFetchedAt() {
        return fetchedAt;
    }

    public List<ForecastDay> getForecastDays() {
        return forecastDays;
    }

    public String formatTemperature(String units) {
        return String.format(Locale.US, "%.0f%s", temperature, unitSuffix(units));
    }

    public String formatFeelsLike(String units) {
        return String.format(Locale.US, "%.0f%s", feelsLike, unitSuffix(units));
    }

    public static String unitSuffix(String units) {
        return "fahrenheit".equals(units) ? "\u00B0F" : "\u00B0C";
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("locationName", locationName);
        json.put("latitude", latitude);
        json.put("longitude", longitude);
        json.put("temperature", temperature);
        json.put("feelsLike", feelsLike);
        json.put("humidity", humidity);
        json.put("windSpeed", windSpeed);
        json.put("weatherCode", weatherCode);
        json.put("condition", condition);
        json.put("sunrise", sunrise);
        json.put("sunset", sunset);
        json.put("fetchedAt", fetchedAt);
        JSONArray forecast = new JSONArray();
        for (ForecastDay day : forecastDays) {
            forecast.put(day.toJson());
        }
        json.put("forecastDays", forecast);
        return json;
    }

    public static WeatherSnapshot fromJson(String raw) throws JSONException {
        JSONObject json = new JSONObject(raw);
        JSONArray forecast = json.optJSONArray("forecastDays");
        List<ForecastDay> days = new ArrayList<>();
        if (forecast != null) {
            for (int i = 0; i < forecast.length(); i++) {
                days.add(ForecastDay.fromJson(forecast.getJSONObject(i)));
            }
        }
        return new WeatherSnapshot(
                json.optString("locationName", ""),
                json.optDouble("latitude", 0),
                json.optDouble("longitude", 0),
                json.optDouble("temperature", 0),
                json.optDouble("feelsLike", 0),
                json.optInt("humidity", 0),
                json.optDouble("windSpeed", 0),
                json.optInt("weatherCode", 0),
                json.optString("condition", ""),
                json.optString("sunrise", ""),
                json.optString("sunset", ""),
                json.optLong("fetchedAt", 0),
                days);
    }

    public static final class ForecastDay {
        private final String date;
        private final int weatherCode;
        private final String condition;
        private final double minTemperature;
        private final double maxTemperature;
        private final int precipitationProbability;

        public ForecastDay(String date, int weatherCode, String condition,
                           double minTemperature, double maxTemperature,
                           int precipitationProbability) {
            this.date = date;
            this.weatherCode = weatherCode;
            this.condition = condition;
            this.minTemperature = minTemperature;
            this.maxTemperature = maxTemperature;
            this.precipitationProbability = precipitationProbability;
        }

        public String getDate() {
            return date;
        }

        public int getWeatherCode() {
            return weatherCode;
        }

        public String getCondition() {
            return condition;
        }

        public double getMinTemperature() {
            return minTemperature;
        }

        public double getMaxTemperature() {
            return maxTemperature;
        }

        public int getPrecipitationProbability() {
            return precipitationProbability;
        }

        JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("date", date);
            json.put("weatherCode", weatherCode);
            json.put("condition", condition);
            json.put("minTemperature", minTemperature);
            json.put("maxTemperature", maxTemperature);
            json.put("precipitationProbability", precipitationProbability);
            return json;
        }

        static ForecastDay fromJson(JSONObject json) {
            return new ForecastDay(
                    json.optString("date", ""),
                    json.optInt("weatherCode", 0),
                    json.optString("condition", ""),
                    json.optDouble("minTemperature", 0),
                    json.optDouble("maxTemperature", 0),
                    json.optInt("precipitationProbability", 0));
        }
    }
}
