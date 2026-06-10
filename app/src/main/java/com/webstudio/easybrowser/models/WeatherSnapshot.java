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
    private final List<HourlyCondition> hourlyConditions;
    private final int airQualityIndex;
    private final String airQualityDescription;

    public WeatherSnapshot(String locationName, double latitude, double longitude,
                           double temperature, double feelsLike, int humidity,
                           double windSpeed, int weatherCode, String condition,
                           String sunrise, String sunset, long fetchedAt,
                           List<ForecastDay> forecastDays) {
        this(locationName, latitude, longitude, temperature, feelsLike, humidity,
                windSpeed, weatherCode, condition, sunrise, sunset, fetchedAt, forecastDays,
                Collections.emptyList(), -1, "");
    }

    public WeatherSnapshot(String locationName, double latitude, double longitude,
                           double temperature, double feelsLike, int humidity,
                           double windSpeed, int weatherCode, String condition,
                           String sunrise, String sunset, long fetchedAt,
                           List<ForecastDay> forecastDays,
                           List<HourlyCondition> hourlyConditions,
                           int airQualityIndex,
                           String airQualityDescription) {
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
        this.hourlyConditions = hourlyConditions == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(hourlyConditions));
        this.airQualityIndex = airQualityIndex;
        this.airQualityDescription = airQualityDescription == null ? "" : airQualityDescription;
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

    public List<HourlyCondition> getHourlyConditions() {
        return hourlyConditions;
    }

    public int getAirQualityIndex() {
        return airQualityIndex;
    }

    public String getAirQualityDescription() {
        return airQualityDescription;
    }

    public boolean hasAirQuality() {
        return airQualityIndex >= 0;
    }

    public WeatherSnapshot withAirQuality(int index, String description) {
        return new WeatherSnapshot(locationName, latitude, longitude, temperature, feelsLike,
                humidity, windSpeed, weatherCode, condition, sunrise, sunset, fetchedAt,
                forecastDays, hourlyConditions, index, description);
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
        JSONArray hourly = new JSONArray();
        for (HourlyCondition condition : hourlyConditions) {
            hourly.put(condition.toJson());
        }
        json.put("hourlyConditions", hourly);
        json.put("airQualityIndex", airQualityIndex);
        json.put("airQualityDescription", airQualityDescription);
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
        JSONArray hourly = json.optJSONArray("hourlyConditions");
        List<HourlyCondition> hours = new ArrayList<>();
        if (hourly != null) {
            for (int i = 0; i < hourly.length(); i++) {
                hours.add(HourlyCondition.fromJson(hourly.getJSONObject(i)));
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
                days,
                hours,
                json.optInt("airQualityIndex", -1),
                json.optString("airQualityDescription", ""));
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

    public static final class HourlyCondition {
        private final String label;
        private final int weatherCode;
        private final String condition;
        private final double temperature;

        public HourlyCondition(String label, int weatherCode, String condition,
                               double temperature) {
            this.label = label == null ? "" : label;
            this.weatherCode = weatherCode;
            this.condition = condition == null ? "" : condition;
            this.temperature = temperature;
        }

        public String getLabel() {
            return label;
        }

        public int getWeatherCode() {
            return weatherCode;
        }

        public String getCondition() {
            return condition;
        }

        public double getTemperature() {
            return temperature;
        }

        JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("label", label);
            json.put("weatherCode", weatherCode);
            json.put("condition", condition);
            json.put("temperature", temperature);
            return json;
        }

        static HourlyCondition fromJson(JSONObject json) {
            return new HourlyCondition(
                    json.optString("label", ""),
                    json.optInt("weatherCode", 0),
                    json.optString("condition", ""),
                    json.optDouble("temperature", 0));
        }
    }
}
