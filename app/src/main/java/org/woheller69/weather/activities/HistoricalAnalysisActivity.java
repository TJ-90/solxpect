package org.woheller69.weather.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.db.chart.model.LineSet;
import com.db.chart.view.LineChartView;

import org.woheller69.weather.R;
import org.woheller69.weather.optimization.HistoricalSolarAnalyzer;

import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class HistoricalAnalysisActivity extends AppCompatActivity {
    
    private EditText etLatitude;
    private EditText etLongitude;
    private EditText etAzimuth;
    private EditText etTilt;
    private EditText etPeakPower;
    private EditText etPanelArea;
    private EditText etEfficiency;
    private EditText etTempCoeff;
    private EditText etDiffuseEfficiency;
    private EditText etInverterPower;
    private EditText etInverterEfficiency;
    private EditText etAlbedo;
    private EditText etStartYear;
    private EditText etEndYear;
    private Button btnAnalyze;
    private ProgressBar progressBar;
    private TextView tvProgress;
    private TextView tvResults;
    private LineChartView monthlyChart;
    private LineChartView hourlyChart;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historical_analysis);
        
        initializeViews();
        setDefaultValues();
        
        btnAnalyze.setOnClickListener(v -> analyzeHistoricalData());
    }
    
    private void initializeViews() {
        etLatitude = findViewById(R.id.et_latitude);
        etLongitude = findViewById(R.id.et_longitude);
        etAzimuth = findViewById(R.id.et_azimuth);
        etTilt = findViewById(R.id.et_tilt);
        etPeakPower = findViewById(R.id.et_peak_power);
        etPanelArea = findViewById(R.id.et_panel_area);
        etEfficiency = findViewById(R.id.et_efficiency);
        etTempCoeff = findViewById(R.id.et_temp_coeff);
        etDiffuseEfficiency = findViewById(R.id.et_diffuse_efficiency);
        etInverterPower = findViewById(R.id.et_inverter_power);
        etInverterEfficiency = findViewById(R.id.et_inverter_efficiency);
        etAlbedo = findViewById(R.id.et_albedo);
        etStartYear = findViewById(R.id.et_start_year);
        etEndYear = findViewById(R.id.et_end_year);
        btnAnalyze = findViewById(R.id.btn_analyze);
        progressBar = findViewById(R.id.progress_bar);
        tvProgress = findViewById(R.id.tv_progress);
        tvResults = findViewById(R.id.tv_results);
        monthlyChart = findViewById(R.id.monthly_chart);
        hourlyChart = findViewById(R.id.hourly_chart);
    }
    
    private void setDefaultValues() {
        // Set reasonable defaults
        etAzimuth.setText("180"); // South
        etTilt.setText("30"); // 30 degrees
        etPeakPower.setText("5000"); // 5kW
        etPanelArea.setText("25"); // 25 m²
        etEfficiency.setText("20"); // 20%
        etTempCoeff.setText("-0.4"); // -0.4%/K
        etDiffuseEfficiency.setText("85"); // 85%
        etInverterPower.setText("5000"); // 5kW
        etInverterEfficiency.setText("95"); // 95%
        etAlbedo.setText("0.2"); // 0.2 (grass/soil)
        
        // Default to last 3 years
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        etStartYear.setText(String.valueOf(currentYear - 3));
        etEndYear.setText(String.valueOf(currentYear - 1));
    }
    
    private void analyzeHistoricalData() {
        try {
            // Get input values
            double latitude = Double.parseDouble(etLatitude.getText().toString());
            double longitude = Double.parseDouble(etLongitude.getText().toString());
            double azimuth = Double.parseDouble(etAzimuth.getText().toString());
            double tilt = Double.parseDouble(etTilt.getText().toString());
            double peakPower = Double.parseDouble(etPeakPower.getText().toString());
            double panelArea = Double.parseDouble(etPanelArea.getText().toString());
            double efficiency = Double.parseDouble(etEfficiency.getText().toString());
            double tempCoeff = Double.parseDouble(etTempCoeff.getText().toString());
            double diffuseEfficiency = Double.parseDouble(etDiffuseEfficiency.getText().toString());
            double inverterPower = Double.parseDouble(etInverterPower.getText().toString());
            double inverterEfficiency = Double.parseDouble(etInverterEfficiency.getText().toString());
            double albedo = Double.parseDouble(etAlbedo.getText().toString());
            int startYear = Integer.parseInt(etStartYear.getText().toString());
            int endYear = Integer.parseInt(etEndYear.getText().toString());
            
            // Validate inputs
            if (latitude < -90 || latitude > 90) {
                Toast.makeText(this, "Latitude must be between -90 and 90", Toast.LENGTH_SHORT).show();
                return;
            }
            if (longitude < -180 || longitude > 180) {
                Toast.makeText(this, "Longitude must be between -180 and 180", Toast.LENGTH_SHORT).show();
                return;
            }
            if (startYear < 1940 || endYear > Calendar.getInstance().get(Calendar.YEAR)) {
                Toast.makeText(this, "Invalid year range", Toast.LENGTH_SHORT).show();
                return;
            }
            if (startYear > endYear) {
                Toast.makeText(this, "Start year must be before end year", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Show progress
            progressBar.setVisibility(View.VISIBLE);
            tvProgress.setVisibility(View.VISIBLE);
            tvResults.setVisibility(View.GONE);
            monthlyChart.setVisibility(View.GONE);
            hourlyChart.setVisibility(View.GONE);
            btnAnalyze.setEnabled(false);
            
            // Create analyzer
            HistoricalSolarAnalyzer analyzer = new HistoricalSolarAnalyzer(
                this, latitude, longitude, azimuth, tilt, peakPower, panelArea,
                efficiency, tempCoeff, diffuseEfficiency, inverterPower,
                inverterEfficiency, albedo
            );
            
            // Start analysis
            analyzer.analyzeHistoricalData(startYear, endYear, new HistoricalSolarAnalyzer.HistoricalDataCallback() {
                @Override
                public void onDataReceived(HistoricalSolarAnalyzer.HistoricalAnalysisResult result) {
                    runOnUiThread(() -> displayResults(result));
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        tvProgress.setVisibility(View.GONE);
                        btnAnalyze.setEnabled(true);
                        Toast.makeText(HistoricalAnalysisActivity.this,
                            "Analysis failed: " + error, Toast.LENGTH_LONG).show();
                    });
                }
                
                @Override
                public void onProgress(String message) {
                    runOnUiThread(() -> tvProgress.setText(message));
                }
            });
            
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void displayResults(HistoricalSolarAnalyzer.HistoricalAnalysisResult result) {
        progressBar.setVisibility(View.GONE);
        tvProgress.setVisibility(View.GONE);
        tvResults.setVisibility(View.VISIBLE);
        monthlyChart.setVisibility(View.VISIBLE);
        hourlyChart.setVisibility(View.VISIBLE);
        btnAnalyze.setEnabled(true);
        
        // Display text results
        StringBuilder sb = new StringBuilder();
        sb.append("Historical Analysis Results\n\n");
        sb.append(String.format(Locale.getDefault(),
            "Average Annual Production: %.0f kWh\n", result.averageAnnualProduction));
        sb.append(String.format(Locale.getDefault(),
            "Peak Daily Production: %.1f kWh on %s\n\n", result.peakDailyProduction, result.peakProductionDate));
        
        sb.append("Yearly Totals:\n");
        for (Map.Entry<Integer, Double> entry : result.yearlyTotals.entrySet()) {
            sb.append(String.format(Locale.getDefault(),
                "%d: %.0f kWh\n", entry.getKey(), entry.getValue()));
        }
        
        tvResults.setText(sb.toString());
        
        // Display monthly chart
        displayMonthlyChart(result.monthlyAverages);
        
        // Display hourly pattern chart
        displayHourlyChart(result.typicalDayPattern);
    }
    
    private void displayMonthlyChart(Map<String, Double> monthlyAverages) {
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                          "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        float[] values = new float[12];
        
        for (int i = 0; i < 12; i++) {
            String monthKey = String.format("%02d", i + 1);
            values[i] = monthlyAverages.getOrDefault(monthKey, 0.0).floatValue();
        }
        
        LineSet dataset = new LineSet(months, values);
        dataset.setColor(getResources().getColor(R.color.colorPrimary));
        dataset.setThickness(4);
        
        monthlyChart.addData(dataset);
        monthlyChart.setYLabels(com.db.chart.model.ChartSet.AxisController.LABEL_NONE);
        monthlyChart.setXLabels(com.db.chart.model.ChartSet.AxisController.LabelPosition.OUTSIDE);
        monthlyChart.show();
    }
    
    private void displayHourlyChart(java.util.List<HistoricalSolarAnalyzer.HourlyProductionPattern> hourlyPattern) {
        String[] hours = new String[24];
        float[] values = new float[24];
        
        for (int i = 0; i < 24; i++) {
            hours[i] = String.valueOf(i);
            values[i] = (float) hourlyPattern.get(i).averagePower;
        }
        
        LineSet dataset = new LineSet(hours, values);
        dataset.setColor(getResources().getColor(R.color.colorAccent));
        dataset.setThickness(4);
        
        hourlyChart.addData(dataset);
        hourlyChart.setYLabels(com.db.chart.model.ChartSet.AxisController.LABEL_NONE);
        hourlyChart.setXLabels(com.db.chart.model.ChartSet.AxisController.LabelPosition.OUTSIDE);
        hourlyChart.show();
    }
}