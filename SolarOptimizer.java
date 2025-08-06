import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.json.*;

public class SolarOptimizer {
    
    private static final String ARCHIVE_BASE_URL = "https://archive-api.open-meteo.com/v1/archive";
    private static final double PANEL_EFFICIENCY = 0.20; // 20% efficiency
    private static final double INVERTER_EFFICIENCY = 0.95; // 95% efficiency
    private static final double TEMP_COEFFICIENT = -0.004; // -0.4%/K
    private static final double PANEL_AREA = 25.0; // 25 m² (5kW system)
    private static final double ALBEDO = 0.2; // grass/soil
    
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== Solar Panel Optimizer ===");
        System.out.print("Enter latitude: ");
        double latitude = scanner.nextDouble();
        
        System.out.print("Enter longitude: ");
        double longitude = scanner.nextDouble();
        
        System.out.println("\nCalculating optimal angles...");
        
        // Find optimal angles
        OptimalAngles optimal = findOptimalAngles(latitude, longitude);
        
        System.out.println("\n=== OPTIMAL CONFIGURATION ===");
        System.out.printf("Latitude: %.4f°\n", latitude);
        System.out.printf("Longitude: %.4f°\n", longitude);
        System.out.printf("Optimal Azimuth: %.1f° (%s)\n", optimal.azimuth, getDirection(optimal.azimuth));
        System.out.printf("Optimal Tilt: %.1f°\n", optimal.tilt);
        
        // Fetch last year's data and calculate production
        System.out.println("\nFetching historical weather data...");
        double annualProduction = calculateAnnualProduction(latitude, longitude, optimal.azimuth, optimal.tilt);
        
        System.out.println("\n=== EXPECTED PRODUCTION ===");
        System.out.printf("Annual Energy Production: %.0f kWh\n", annualProduction / 1000);
        System.out.printf("Daily Average: %.1f kWh\n", annualProduction / 1000 / 365);
        System.out.printf("Monthly Average: %.0f kWh\n", annualProduction / 1000 / 12);
        
        // Calculate monthly breakdown
        System.out.println("\n=== MONTHLY BREAKDOWN ===");
        Map<Integer, Double> monthlyProduction = calculateMonthlyProduction(latitude, longitude, optimal.azimuth, optimal.tilt);
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        for (int i = 1; i <= 12; i++) {
            System.out.printf("%s: %.0f kWh\n", months[i-1], monthlyProduction.get(i) / 1000);
        }
        
        scanner.close();
    }
    
    static class OptimalAngles {
        double azimuth;
        double tilt;
        
        OptimalAngles(double azimuth, double tilt) {
            this.azimuth = azimuth;
            this.tilt = tilt;
        }
    }
    
    private static OptimalAngles findOptimalAngles(double latitude, double longitude) throws Exception {
        double bestAzimuth = 180; // Default south
        double bestTilt = Math.abs(latitude); // Default tilt = latitude
        double maxEnergy = 0;
        
        // For southern hemisphere, face north (azimuth = 0)
        if (latitude < 0) {
            bestAzimuth = 0;
        }
        
        // Test different tilt angles around the latitude
        for (double tilt = Math.max(0, Math.abs(latitude) - 15); 
             tilt <= Math.min(90, Math.abs(latitude) + 15); 
             tilt += 5) {
            
            double energy = estimateAnnualEnergy(latitude, longitude, bestAzimuth, tilt);
            if (energy > maxEnergy) {
                maxEnergy = energy;
                bestTilt = tilt;
            }
        }
        
        // For locations far from equator, test some azimuth variation
        if (Math.abs(latitude) > 20) {
            double baseAzimuth = latitude < 0 ? 0 : 180;
            for (double az = baseAzimuth - 30; az <= baseAzimuth + 30; az += 10) {
                double energy = estimateAnnualEnergy(latitude, longitude, az, bestTilt);
                if (energy > maxEnergy) {
                    maxEnergy = energy;
                    bestAzimuth = az;
                }
            }
        }
        
        return new OptimalAngles(bestAzimuth, bestTilt);
    }
    
    private static double estimateAnnualEnergy(double latitude, double longitude, double azimuth, double tilt) {
        // Simple estimation based on latitude and panel orientation
        double latRad = Math.toRadians(latitude);
        double tiltRad = Math.toRadians(tilt);
        
        // Base solar radiation (simplified model)
        double baseRadiation = 1000 * (1 - 0.5 * Math.abs(latRad) / (Math.PI/2));
        
        // Tilt factor (optimal when tilt ≈ latitude)
        double tiltFactor = Math.cos(Math.abs(tiltRad - Math.abs(latRad)));
        
        // Azimuth factor (best when facing equator)
        double azimuthRad = Math.toRadians(azimuth);
        double targetAzimuth = latitude < 0 ? 0 : Math.PI;
        double azimuthFactor = 0.9 + 0.1 * Math.cos(azimuthRad - targetAzimuth);
        
        // Annual energy estimate (simplified)
        return baseRadiation * tiltFactor * azimuthFactor * 1825; // 1825 = 365 days * 5 peak sun hours
    }
    
    private static double calculateAnnualProduction(double latitude, double longitude, double azimuth, double tilt) throws Exception {
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate = endDate.minusYears(1);
        
        String url = String.format(
            "%s?latitude=%.6f&longitude=%.6f&start_date=%s&end_date=%s" +
            "&hourly=temperature_2m,diffuse_radiation,direct_normal_irradiance,shortwave_radiation" +
            "&timezone=auto",
            ARCHIVE_BASE_URL, latitude, longitude, startDate, endDate
        );
        
        // Fetch weather data
        String response = fetchData(url);
        JSONObject data = new JSONObject(response);
        JSONObject hourly = data.getJSONObject("hourly");
        
        JSONArray times = hourly.getJSONArray("time");
        JSONArray temperatures = hourly.getJSONArray("temperature_2m");
        JSONArray diffuseRadiation = hourly.getJSONArray("diffuse_radiation");
        JSONArray directNormal = hourly.getJSONArray("direct_normal_irradiance");
        JSONArray shortwave = hourly.getJSONArray("shortwave_radiation");
        
        double totalEnergy = 0;
        
        for (int i = 0; i < times.length(); i++) {
            double temp = temperatures.optDouble(i, 20);
            double diffuse = diffuseRadiation.optDouble(i, 0);
            double direct = directNormal.optDouble(i, 0);
            double sw = shortwave.optDouble(i, 0);
            
            long epochTime = times.getLong(i);
            
            double power = calculatePower(latitude, longitude, azimuth, tilt, 
                                        direct, diffuse, sw, epochTime, temp);
            totalEnergy += power; // Wh
        }
        
        return totalEnergy;
    }
    
    private static Map<Integer, Double> calculateMonthlyProduction(double latitude, double longitude, 
                                                                  double azimuth, double tilt) throws Exception {
        Map<Integer, Double> monthlyTotals = new HashMap<>();
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate = endDate.minusYears(1);
        
        String url = String.format(
            "%s?latitude=%.6f&longitude=%.6f&start_date=%s&end_date=%s" +
            "&hourly=temperature_2m,diffuse_radiation,direct_normal_irradiance,shortwave_radiation" +
            "&timezone=auto",
            ARCHIVE_BASE_URL, latitude, longitude, startDate, endDate
        );
        
        String response = fetchData(url);
        JSONObject data = new JSONObject(response);
        JSONObject hourly = data.getJSONObject("hourly");
        
        JSONArray times = hourly.getJSONArray("time");
        JSONArray temperatures = hourly.getJSONArray("temperature_2m");
        JSONArray diffuseRadiation = hourly.getJSONArray("diffuse_radiation");
        JSONArray directNormal = hourly.getJSONArray("direct_normal_irradiance");
        JSONArray shortwave = hourly.getJSONArray("shortwave_radiation");
        
        for (int i = 1; i <= 12; i++) {
            monthlyTotals.put(i, 0.0);
        }
        
        for (int i = 0; i < times.length(); i++) {
            double temp = temperatures.optDouble(i, 20);
            double diffuse = diffuseRadiation.optDouble(i, 0);
            double direct = directNormal.optDouble(i, 0);
            double sw = shortwave.optDouble(i, 0);
            
            long epochTime = times.getLong(i);
            LocalDateTime dateTime = LocalDateTime.ofEpochSecond(epochTime, 0, ZoneOffset.UTC);
            int month = dateTime.getMonthValue();
            
            double power = calculatePower(latitude, longitude, azimuth, tilt, 
                                        direct, diffuse, sw, epochTime, temp);
            
            monthlyTotals.put(month, monthlyTotals.get(month) + power);
        }
        
        return monthlyTotals;
    }
    
    private static double calculatePower(double latitude, double longitude, double azimuth, double tilt,
                                       double directNormal, double diffuse, double shortwave, 
                                       long epochTime, double temperature) {
        // Calculate sun position
        LocalDateTime dateTime = LocalDateTime.ofEpochSecond(epochTime, 0, ZoneOffset.UTC);
        double[] sunPos = calculateSunPosition(latitude, longitude, dateTime);
        double sunAzimuth = sunPos[0];
        double sunElevation = sunPos[1];
        
        // Calculate angle between sun and panel normal
        double efficiency = calculateIncidenceAngle(sunAzimuth, sunElevation, azimuth, tilt);
        
        // Calculate radiation on panel
        double directOnPanel = directNormal * efficiency;
        double diffuseOnPanel = diffuse * 0.85; // 85% diffuse efficiency for tilted panel
        double reflectedOnPanel = shortwave * ALBEDO * (1 - Math.cos(Math.toRadians(tilt))) / 2;
        
        double totalRadiation = directOnPanel + diffuseOnPanel + reflectedOnPanel;
        
        // Temperature effect
        double cellTemp = temperature + 0.035 * totalRadiation;
        double tempEffect = 1 + (cellTemp - 25) * TEMP_COEFFICIENT;
        
        // Calculate DC power
        double dcPower = totalRadiation * PANEL_EFFICIENCY * PANEL_AREA * tempEffect;
        
        // AC power after inverter
        return dcPower * INVERTER_EFFICIENCY;
    }
    
    private static double[] calculateSunPosition(double lat, double lon, LocalDateTime dateTime) {
        // Simplified sun position calculation
        int dayOfYear = dateTime.getDayOfYear();
        double hour = dateTime.getHour() + dateTime.getMinute() / 60.0;
        
        // Solar declination
        double declination = 23.45 * Math.sin(Math.toRadians((284 + dayOfYear) * 360.0 / 365));
        double decRad = Math.toRadians(declination);
        double latRad = Math.toRadians(lat);
        
        // Hour angle
        double hourAngle = (hour - 12) * 15;
        double hourRad = Math.toRadians(hourAngle);
        
        // Solar elevation
        double elevation = Math.toDegrees(Math.asin(
            Math.sin(decRad) * Math.sin(latRad) +
            Math.cos(decRad) * Math.cos(latRad) * Math.cos(hourRad)
        ));
        
        // Solar azimuth
        double azimuth = Math.toDegrees(Math.atan2(
            -Math.sin(hourRad),
            Math.tan(decRad) * Math.cos(latRad) - Math.sin(latRad) * Math.cos(hourRad)
        )) + 180;
        
        return new double[]{azimuth, Math.max(0, elevation)};
    }
    
    private static double calculateIncidenceAngle(double sunAzimuth, double sunElevation, 
                                                double panelAzimuth, double panelTilt) {
        if (sunElevation <= 0) return 0;
        
        double sunAzRad = Math.toRadians(sunAzimuth);
        double sunElRad = Math.toRadians(sunElevation);
        double panelAzRad = Math.toRadians(panelAzimuth);
        double panelTiltRad = Math.toRadians(panelTilt);
        
        // Calculate angle between sun ray and panel normal
        double cosIncidence = Math.sin(sunElRad) * Math.cos(panelTiltRad) +
                            Math.cos(sunElRad) * Math.sin(panelTiltRad) * 
                            Math.cos(sunAzRad - panelAzRad);
        
        return Math.max(0, cosIncidence);
    }
    
    private static String fetchData(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        return response.toString();
    }
    
    private static String getDirection(double azimuth) {
        if (azimuth >= 337.5 || azimuth < 22.5) return "North";
        if (azimuth >= 22.5 && azimuth < 67.5) return "Northeast";
        if (azimuth >= 67.5 && azimuth < 112.5) return "East";
        if (azimuth >= 112.5 && azimuth < 157.5) return "Southeast";
        if (azimuth >= 157.5 && azimuth < 202.5) return "South";
        if (azimuth >= 202.5 && azimuth < 247.5) return "Southwest";
        if (azimuth >= 247.5 && azimuth < 292.5) return "West";
        if (azimuth >= 292.5 && azimuth < 337.5) return "Northwest";
        return "Unknown";
    }
}