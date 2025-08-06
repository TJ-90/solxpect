package org.woheller69.weather.optimization;

import android.util.Log;

import org.woheller69.weather.SolarPowerPlant;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.List;

public class SolarAngleOptimizer {
    private static final String TAG = "SolarAngleOptimizer";
    
    // Optimization parameters
    private static final int AZIMUTH_STEP = 10; // Test every 10 degrees
    private static final int TILT_STEP = 5;     // Test every 5 degrees
    private static final int DAYS_TO_SAMPLE = 12; // Sample one day per month
    
    private final double latitude;
    private final double longitude;
    private final double cellsMaxPower;
    private final double cellsArea;
    private final double cellsEfficiency;
    private final double cellsTempCoeff;
    private final double inverterPowerLimit;
    private final double inverterEfficiency;
    private final double albedo;
    
    // Callback interface for optimization results
    public interface OptimizationCallback {
        void onOptimizationComplete(OptimizationResult result);
        void onOptimizationError(String error);
        void onProgressUpdate(int progress, int total);
    }
    
    // Result class
    public static class OptimizationResult {
        public final double optimalAzimuth;
        public final double optimalTilt;
        public final double annualEnergyOutput; // in kWh
        public final double percentageImprovement; // compared to flat horizontal
        
        public OptimizationResult(double azimuth, double tilt, double energy, double improvement) {
            this.optimalAzimuth = azimuth;
            this.optimalTilt = tilt;
            this.annualEnergyOutput = energy;
            this.percentageImprovement = improvement;
        }
    }
    
    public SolarAngleOptimizer(double latitude, double longitude, double cellsMaxPower, 
                              double cellsArea, double cellsEfficiency, double cellsTempCoeff,
                              double inverterPowerLimit, double inverterEfficiency, double albedo) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.cellsMaxPower = cellsMaxPower;
        this.cellsArea = cellsArea;
        this.cellsEfficiency = cellsEfficiency;
        this.cellsTempCoeff = cellsTempCoeff;
        this.inverterPowerLimit = inverterPowerLimit;
        this.inverterEfficiency = inverterEfficiency;
        this.albedo = albedo;
    }
    
    public void optimizeAsync(OptimizationCallback callback) {
        CompletableFuture.runAsync(() -> {
            try {
                OptimizationResult result = optimize(callback);
                callback.onOptimizationComplete(result);
            } catch (Exception e) {
                Log.e(TAG, "Optimization failed", e);
                callback.onOptimizationError(e.getMessage());
            }
        });
    }
    
    private OptimizationResult optimize(OptimizationCallback callback) {
        double bestAzimuth = 180; // Default south-facing
        double bestTilt = latitude; // Default tilt = latitude
        double maxEnergy = 0;
        
        // Calculate baseline (horizontal panel)
        double baselineEnergy = calculateAnnualEnergy(180, 0);
        
        int totalCombinations = (360 / AZIMUTH_STEP) * (90 / TILT_STEP);
        int currentProgress = 0;
        
        // Test different angle combinations
        for (int azimuth = 0; azimuth < 360; azimuth += AZIMUTH_STEP) {
            for (int tilt = 0; tilt <= 90; tilt += TILT_STEP) {
                double energy = calculateAnnualEnergy(azimuth, tilt);
                
                if (energy > maxEnergy) {
                    maxEnergy = energy;
                    bestAzimuth = azimuth;
                    bestTilt = tilt;
                }
                
                currentProgress++;
                if (callback != null && currentProgress % 10 == 0) {
                    callback.onProgressUpdate(currentProgress, totalCombinations);
                }
            }
        }
        
        double improvement = ((maxEnergy - baselineEnergy) / baselineEnergy) * 100;
        
        return new OptimizationResult(bestAzimuth, bestTilt, maxEnergy / 1000, improvement);
    }
    
    private double calculateAnnualEnergy(double azimuth, double tilt) {
        double totalEnergy = 0;
        
        // Sample days throughout the year (15th of each month)
        for (int month = 1; month <= 12; month++) {
            LocalDateTime sampleDate = LocalDateTime.of(2023, month, 15, 0, 0);
            double dailyEnergy = calculateDailyEnergy(azimuth, tilt, sampleDate);
            
            // Multiply by days in month for monthly estimate
            int daysInMonth = sampleDate.toLocalDate().lengthOfMonth();
            totalEnergy += dailyEnergy * daysInMonth;
        }
        
        return totalEnergy;
    }
    
    private double calculateDailyEnergy(double azimuth, double tilt, LocalDateTime date) {
        // Create a solar power plant instance with test angles
        SolarPowerPlant plant = new SolarPowerPlant(
            latitude, longitude, cellsMaxPower, cellsArea, cellsEfficiency,
            cellsTempCoeff, 100, // diffuseEfficiency = 100% for optimization
            inverterPowerLimit, inverterEfficiency, false,
            azimuth, tilt,
            new int[36], // No shading for optimization
            new int[36], // No shading for optimization
            albedo
        );
        
        double dailyEnergy = 0;
        
        // Calculate hourly energy for the day
        for (int hour = 0; hour < 24; hour++) {
            LocalDateTime hourTime = date.plusHours(hour);
            long epochSeconds = hourTime.toEpochSecond(ZoneOffset.UTC);
            
            // Use typical clear-sky radiation values for optimization
            double solarElevation = calculateSolarElevation(epochSeconds);
            double directNormal = calculateClearSkyDNI(solarElevation);
            double diffuse = calculateClearSkyDiffuse(solarElevation);
            double shortwave = directNormal * Math.sin(Math.toRadians(solarElevation)) + diffuse;
            
            float power = plant.getPower(directNormal, diffuse, shortwave, epochSeconds, 20); // 20°C ambient temp
            dailyEnergy += power; // Wh
        }
        
        return dailyEnergy;
    }
    
    private double calculateSolarElevation(long epochSeconds) {
        // Simplified solar elevation calculation
        // In real implementation, use the Grena3 library
        LocalDateTime time = LocalDateTime.ofEpochSecond(epochSeconds, 0, ZoneOffset.UTC);
        double hourAngle = (time.getHour() - 12) * 15; // degrees
        double declination = 23.45 * Math.sin(Math.toRadians((284 + time.getDayOfYear()) * 360.0 / 365));
        
        double elevation = Math.toDegrees(Math.asin(
            Math.sin(Math.toRadians(declination)) * Math.sin(Math.toRadians(latitude)) +
            Math.cos(Math.toRadians(declination)) * Math.cos(Math.toRadians(latitude)) * 
            Math.cos(Math.toRadians(hourAngle))
        ));
        
        return Math.max(0, elevation);
    }
    
    private double calculateClearSkyDNI(double solarElevation) {
        if (solarElevation <= 0) return 0;
        
        // Simple clear-sky model
        double airMass = 1 / Math.sin(Math.toRadians(solarElevation));
        double dni = 900 * Math.exp(-0.13 * airMass); // W/m²
        
        return Math.max(0, dni);
    }
    
    private double calculateClearSkyDiffuse(double solarElevation) {
        if (solarElevation <= 0) return 0;
        
        // Simple diffuse radiation model
        return 100 * Math.sin(Math.toRadians(solarElevation)); // W/m²
    }
}