import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONObject;

public class SimpleSolarOptimizer {
    
    private static final String ARCHIVE_API_URL = "https://archive-api.open-meteo.com/v1/archive";
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== Solar Panel Optimizer ===");
        System.out.print("Enter latitude: ");
        double latitude = scanner.nextDouble();
        
        System.out.print("Enter longitude: ");
        double longitude = scanner.nextDouble();
        
        System.out.println("\nCalculating optimal angles for your location...\n");
        
        // Calculate optimal angles
        OptimalAngles optimal = calculateOptimalAngles(latitude, longitude);
        
        System.out.println("OPTIMAL CONFIGURATION:");
        System.out.println("=====================");
        System.out.printf("Azimuth: %.1f° (%s-facing)\n", optimal.azimuth, getDirection(optimal.azimuth));
        System.out.printf("Tilt: %.1f° from horizontal\n", optimal.tilt);
        System.out.println("\nRECOMMENDATIONS:");
        System.out.println("- Face panels " + getDirection(optimal.azimuth));
        System.out.println("- Tilt at " + Math.round(optimal.tilt) + "° angle");
        System.out.println("- This maximizes year-round energy production");
        
        System.out.println("\nFetching last year's weather data...");
        
        // Get last year's data
        try {
            analyzeLastYearProduction(latitude, longitude, optimal.azimuth, optimal.tilt);
        } catch (Exception e) {
            System.err.println("Error fetching weather data: " + e.getMessage());
        }
        
        scanner.close();
    }
    
    private static class OptimalAngles {
        double azimuth;
        double tilt;
        
        OptimalAngles(double azimuth, double tilt) {
            this.azimuth = azimuth;
            this.tilt = tilt;
        }
    }
    
    private static OptimalAngles calculateOptimalAngles(double latitude, double longitude) {
        // For solar panels, optimal configuration depends on hemisphere and latitude
        
        double optimalAzimuth;
        double optimalTilt;
        
        // Azimuth: Point towards equator
        if (latitude >= 0) {
            // Northern hemisphere - face south
            optimalAzimuth = 180.0;
        } else {
            // Southern hemisphere - face north
            optimalAzimuth = 0.0;
        }
        
        // Tilt angle calculation
        // General rule: Tilt = Latitude for year-round optimization
        // Adjust slightly for seasonal bias
        double absLatitude = Math.abs(latitude);
        
        if (absLatitude < 25) {
            // Tropical regions - smaller tilt
            optimalTilt = absLatitude * 0.87;
        } else if (absLatitude < 50) {
            // Temperate regions - tilt approximately equals latitude
            optimalTilt = absLatitude;
        } else {
            // High latitudes - slightly less than latitude for better summer production
            optimalTilt = absLatitude * 0.9;
        }
        
        return new OptimalAngles(optimalAzimuth, optimalTilt);
    }
    
    private static void analyzeLastYearProduction(double latitude, double longitude, 
                                                  double azimuth, double tilt) throws Exception {
        // Get last year's date range
        LocalDate endDate = LocalDate.now().minusDays(2); // API has 2-day delay
        LocalDate startDate = endDate.minusYears(1);
        
        // Build API URL
        String url = String.format(
            "%s?latitude=%.6f&longitude=%.6f&start_date=%s&end_date=%s" +
            "&hourly=temperature_2m,diffuse_radiation,direct_normal_irradiance,shortwave_radiation" +
            "&timezone=auto",
            ARCHIVE_API_URL, latitude, longitude, startDate, endDate
        );
        
        // Fetch data
        String response = fetchData(url);
        JSONObject data = new JSONObject(response);
        
        // Process the data
        JSONObject hourly = data.getJSONObject("hourly");
        JSONArray times = hourly.getJSONArray("time");
        JSONArray temperatures = hourly.getJSONArray("temperature_2m");
        JSONArray diffuseRadiation = hourly.getJSONArray("diffuse_radiation");
        JSONArray directNormal = hourly.getJSONArray("direct_normal_irradiance");
        JSONArray shortwave = hourly.getJSONArray("shortwave_radiation");
        
        // Calculate production
        double totalEnergy = 0;
        double[] monthlyEnergy = new double[12];
        double peakDailyEnergy = 0;
        String peakDay = "";
        
        // Process hourly data
        for (int i = 0; i < times.length(); i++) {
            long epochTime = times.getLong(i);
            double temp = getDoubleOrZero(temperatures, i);
            double diffuse = getDoubleOrZero(diffuseRadiation, i);
            double direct = getDoubleOrZero(directNormal, i);
            double sw = getDoubleOrZero(shortwave, i);
            
            // Calculate power using simplified model
            double power = calculatePower(latitude, longitude, azimuth, tilt, 
                                        direct, diffuse, sw, epochTime, temp);
            
            totalEnergy += power;
            
            // Track monthly
            LocalDateTime dateTime = LocalDateTime.ofEpochSecond(epochTime, 0, ZoneOffset.UTC);
            int month = dateTime.getMonthValue() - 1;
            monthlyEnergy[month] += power;
        }
        
        // Display results
        System.out.println("\nPRODUCTION ANALYSIS (Last 12 Months):");
        System.out.println("=====================================");
        System.out.printf("Total Annual Production: %.0f kWh\n", totalEnergy / 1000);
        System.out.printf("Average Daily Production: %.1f kWh\n", totalEnergy / 1000 / 365);
        
        System.out.println("\nMonthly Breakdown (kWh):");
        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                              "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        for (int i = 0; i < 12; i++) {
            System.out.printf("%s: %.0f kWh\n", monthNames[i], monthlyEnergy[i] / 1000);
        }
        
        System.out.println("\nNOTE: Based on 1kW panel capacity at standard conditions");
        System.out.println("Multiply by your actual system size for real production estimates");
    }
    
    private static double calculatePower(double lat, double lon, double azimuth, double tilt,
                                       double directNormal, double diffuse, double shortwave,
                                       long epochSeconds, double temperature) {
        // Simplified power calculation
        
        // Calculate sun position
        LocalDateTime dateTime = LocalDateTime.ofEpochSecond(epochSeconds, 0, ZoneOffset.UTC);
        double dayOfYear = dateTime.getDayOfYear();
        double hour = dateTime.getHour() + dateTime.getMinute() / 60.0;
        
        // Solar declination
        double declination = 23.45 * Math.sin(Math.toRadians((284 + dayOfYear) * 360.0 / 365));
        
        // Hour angle
        double hourAngle = (hour - 12) * 15;
        
        // Solar elevation
        double elevation = Math.toDegrees(Math.asin(
            Math.sin(Math.toRadians(declination)) * Math.sin(Math.toRadians(lat)) +
            Math.cos(Math.toRadians(declination)) * Math.cos(Math.toRadians(lat)) * 
            Math.cos(Math.toRadians(hourAngle))
        ));
        
        if (elevation <= 0) return 0; // Sun below horizon
        
        // Solar azimuth (simplified)
        double solarAzimuth = Math.toDegrees(Math.atan2(
            -Math.sin(Math.toRadians(hourAngle)),
            Math.tan(Math.toRadians(declination)) * Math.cos(Math.toRadians(lat)) -
            Math.sin(Math.toRadians(lat)) * Math.cos(Math.toRadians(hourAngle))
        ));
        if (solarAzimuth < 0) solarAzimuth += 360;
        
        // Calculate angle between sun and panel normal
        double angleOfIncidence = calculateAngleOfIncidence(
            solarAzimuth, elevation, azimuth, tilt
        );
        
        double cosineFactor = Math.max(0, Math.cos(Math.toRadians(angleOfIncidence)));
        
        // Total radiation on panel
        double totalRadiation = directNormal * cosineFactor + 
                               diffuse * (1 + Math.cos(Math.toRadians(tilt))) / 2 +
                               shortwave * 0.2 * (1 - Math.cos(Math.toRadians(tilt))) / 2;
        
        // Temperature effect (simplified)
        double tempEffect = 1 + (temperature - 25) * (-0.004);
        
        // Power output (W) - assuming 1kW panel at STC
        double power = totalRadiation / 1000 * tempEffect * 0.95; // 95% system efficiency
        
        return Math.max(0, power);
    }
    
    private static double calculateAngleOfIncidence(double sunAzimuth, double sunElevation,
                                                   double panelAzimuth, double panelTilt) {
        // Convert to radians
        double sa = Math.toRadians(sunAzimuth);
        double se = Math.toRadians(sunElevation);
        double pa = Math.toRadians(panelAzimuth);
        double pt = Math.toRadians(panelTilt);
        
        // Calculate angle between sun vector and panel normal
        double cosIncidence = Math.sin(se) * Math.cos(pt) +
                             Math.cos(se) * Math.sin(pt) * Math.cos(sa - pa);
        
        return Math.toDegrees(Math.acos(Math.min(1, Math.max(-1, cosIncidence))));
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
    
    private static double getDoubleOrZero(JSONArray array, int index) {
        try {
            return array.getDouble(index);
        } catch (Exception e) {
            return 0.0;
        }
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