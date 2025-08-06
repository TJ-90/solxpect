package org.woheller69.weather.optimization;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.woheller69.weather.SolarPowerPlant;
import org.woheller69.weather.http.HttpRequestType;
import org.woheller69.weather.http.IHttpRequest;
import org.woheller69.weather.http.IProcessHttpRequest;
import org.woheller69.weather.http.VolleyHttpRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoricalSolarAnalyzer {
    private static final String TAG = "HistoricalSolarAnalyzer";
    private static final String ARCHIVE_BASE_URL = "https://archive-api.open-meteo.com/v1/archive";
    
    private final Context context;
    private final double latitude;
    private final double longitude;
    private final SolarPowerPlant powerPlant;
    
    // Callback interfaces
    public interface HistoricalDataCallback {
        void onDataReceived(HistoricalAnalysisResult result);
        void onError(String error);
        void onProgress(String message);
    }
    
    // Result classes
    public static class HistoricalAnalysisResult {
        public final Map<String, Double> monthlyAverages; // kWh per month
        public final Map<Integer, Double> yearlyTotals; // kWh per year
        public final double averageAnnualProduction; // kWh
        public final double peakDailyProduction; // kWh
        public final String peakProductionDate;
        public final List<HourlyProductionPattern> typicalDayPattern;
        
        public HistoricalAnalysisResult(Map<String, Double> monthlyAverages,
                                       Map<Integer, Double> yearlyTotals,
                                       double averageAnnualProduction,
                                       double peakDailyProduction,
                                       String peakProductionDate,
                                       List<HourlyProductionPattern> typicalDayPattern) {
            this.monthlyAverages = monthlyAverages;
            this.yearlyTotals = yearlyTotals;
            this.averageAnnualProduction = averageAnnualProduction;
            this.peakDailyProduction = peakDailyProduction;
            this.peakProductionDate = peakProductionDate;
            this.typicalDayPattern = typicalDayPattern;
        }
    }
    
    public static class HourlyProductionPattern {
        public final int hour;
        public final double averagePower; // W
        
        public HourlyProductionPattern(int hour, double averagePower) {
            this.hour = hour;
            this.averagePower = averagePower;
        }
    }
    
    public HistoricalSolarAnalyzer(Context context, double latitude, double longitude,
                                   double azimuth, double tilt, double cellsMaxPower,
                                   double cellsArea, double cellsEfficiency, double cellsTempCoeff,
                                   double diffuseEfficiency, double inverterPowerLimit,
                                   double inverterEfficiency, double albedo) {
        this.context = context;
        this.latitude = latitude;
        this.longitude = longitude;
        
        // Create power plant with optimal or specified angles
        this.powerPlant = new SolarPowerPlant(
            latitude, longitude, cellsMaxPower, cellsArea, cellsEfficiency,
            cellsTempCoeff, diffuseEfficiency, inverterPowerLimit, inverterEfficiency,
            false, azimuth, tilt,
            new int[36], // No shading for historical analysis
            new int[36], // No shading for historical analysis
            albedo
        );
    }
    
    public void analyzeHistoricalData(int startYear, int endYear, HistoricalDataCallback callback) {
        callback.onProgress("Fetching historical solar radiation data...");
        
        // Fetch data year by year to avoid overwhelming the API
        List<JSONObject> allYearData = new ArrayList<>();
        fetchYearData(startYear, endYear, startYear, allYearData, callback);
    }
    
    private void fetchYearData(int currentYear, int endYear, int startYear,
                              List<JSONObject> allYearData, HistoricalDataCallback callback) {
        if (currentYear > endYear) {
            // All data fetched, now analyze
            analyzeCollectedData(allYearData, callback);
            return;
        }
        
        String url = buildHistoricalUrl(currentYear);
        callback.onProgress("Fetching data for year " + currentYear + "...");
        
        IHttpRequest httpRequest = new VolleyHttpRequest(context, 0);
        httpRequest.make(url, HttpRequestType.GET, new IProcessHttpRequest() {
            @Override
            public void processHttpRequest(String response, int cityId) {
                try {
                    JSONObject yearData = new JSONObject(response);
                    allYearData.add(yearData);
                    
                    // Fetch next year
                    fetchYearData(currentYear + 1, endYear, startYear, allYearData, callback);
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing JSON for year " + currentYear, e);
                    callback.onError("Failed to parse data for year " + currentYear);
                }
            }
            
            @Override
            public void processHttpRequestOnError(String error) {
                Log.e(TAG, "Error fetching data for year " + currentYear + ": " + error);
                callback.onError("Failed to fetch data for year " + currentYear);
            }
        });
    }
    
    private String buildHistoricalUrl(int year) {
        return String.format(
            "%s?latitude=%.6f&longitude=%.6f&start_date=%d-01-01&end_date=%d-12-31" +
            "&hourly=temperature_2m,diffuse_radiation,direct_normal_irradiance,shortwave_radiation" +
            "&timezone=auto",
            ARCHIVE_BASE_URL, latitude, longitude, year, year
        );
    }
    
    private void analyzeCollectedData(List<JSONObject> allYearData, HistoricalDataCallback callback) {
        callback.onProgress("Analyzing historical data...");
        
        try {
            Map<String, Double> monthlyAverages = new HashMap<>();
            Map<Integer, Double> yearlyTotals = new HashMap<>();
            Map<String, List<Double>> monthlyData = new HashMap<>();
            double peakDailyProduction = 0;
            String peakProductionDate = "";
            Map<Integer, List<Double>> hourlyData = new HashMap<>();
            
            // Initialize monthly data map
            for (int month = 1; month <= 12; month++) {
                String monthKey = String.format("%02d", month);
                monthlyData.put(monthKey, new ArrayList<>());
            }
            
            // Initialize hourly data map
            for (int hour = 0; hour < 24; hour++) {
                hourlyData.put(hour, new ArrayList<>());
            }
            
            // Process each year's data
            for (JSONObject yearData : allYearData) {
                JSONObject hourly = yearData.getJSONObject("hourly");
                JSONArray times = hourly.getJSONArray("time");
                JSONArray temperatures = hourly.getJSONArray("temperature_2m");
                JSONArray diffuseRadiation = hourly.getJSONArray("diffuse_radiation");
                JSONArray directNormal = hourly.getJSONArray("direct_normal_irradiance");
                JSONArray shortwave = hourly.getJSONArray("shortwave_radiation");
                
                Map<String, Double> dailyProduction = new HashMap<>();
                double yearTotal = 0;
                
                // Process hourly data
                for (int i = 0; i < times.length(); i++) {
                    long epochTime = times.getLong(i);
                    double temp = temperatures.optDouble(i, 20);
                    double diffuse = diffuseRadiation.optDouble(i, 0);
                    double direct = directNormal.optDouble(i, 0);
                    double sw = shortwave.optDouble(i, 0);
                    
                    // Calculate power for this hour
                    float power = powerPlant.getPower(direct, diffuse, sw, epochTime, temp);
                    
                    // Extract date and hour info
                    LocalDate date = LocalDate.ofEpochDay(epochTime / 86400);
                    String dateStr = date.toString();
                    String monthKey = String.format("%02d", date.getMonthValue());
                    int hour = (int) ((epochTime % 86400) / 3600);
                    
                    // Accumulate daily production
                    dailyProduction.put(dateStr, dailyProduction.getOrDefault(dateStr, 0.0) + power);
                    
                    // Accumulate hourly data for pattern analysis
                    hourlyData.get(hour).add((double) power);
                    
                    // Accumulate monthly data
                    monthlyData.get(monthKey).add((double) power);
                    
                    yearTotal += power;
                }
                
                // Find peak daily production
                for (Map.Entry<String, Double> entry : dailyProduction.entrySet()) {
                    if (entry.getValue() > peakDailyProduction) {
                        peakDailyProduction = entry.getValue();
                        peakProductionDate = entry.getKey();
                    }
                }
                
                // Extract year from first timestamp
                int year = LocalDate.ofEpochDay(times.getLong(0) / 86400).getYear();
                yearlyTotals.put(year, yearTotal / 1000); // Convert to kWh
            }
            
            // Calculate monthly averages
            for (Map.Entry<String, List<Double>> entry : monthlyData.entrySet()) {
                List<Double> values = entry.getValue();
                double sum = values.stream().mapToDouble(Double::doubleValue).sum();
                monthlyAverages.put(entry.getKey(), sum / values.size() * 730 / 1000); // Average kWh per month
            }
            
            // Calculate average annual production
            double averageAnnualProduction = yearlyTotals.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);
            
            // Calculate typical day pattern
            List<HourlyProductionPattern> typicalDayPattern = new ArrayList<>();
            for (int hour = 0; hour < 24; hour++) {
                List<Double> hourValues = hourlyData.get(hour);
                double avgPower = hourValues.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0);
                typicalDayPattern.add(new HourlyProductionPattern(hour, avgPower));
            }
            
            HistoricalAnalysisResult result = new HistoricalAnalysisResult(
                monthlyAverages,
                yearlyTotals,
                averageAnnualProduction,
                peakDailyProduction / 1000, // Convert to kWh
                peakProductionDate,
                typicalDayPattern
            );
            
            callback.onDataReceived(result);
            
        } catch (Exception e) {
            Log.e(TAG, "Error analyzing historical data", e);
            callback.onError("Failed to analyze historical data: " + e.getMessage());
        }
    }
}