package org.woheller69.weather.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.woheller69.weather.R;
import org.woheller69.weather.optimization.SolarAngleOptimizer;

import java.util.Locale;

public class OptimalAngleActivity extends AppCompatActivity {
    
    private EditText etLatitude;
    private EditText etLongitude;
    private EditText etPeakPower;
    private EditText etPanelArea;
    private EditText etEfficiency;
    private EditText etTempCoeff;
    private EditText etInverterPower;
    private EditText etInverterEfficiency;
    private EditText etAlbedo;
    private Button btnCalculate;
    private ProgressBar progressBar;
    private TextView tvProgress;
    private TextView tvResults;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_optimal_angle);
        
        // Initialize views
        initializeViews();
        
        // Set default values
        setDefaultValues();
        
        // Set up button click listener
        btnCalculate.setOnClickListener(v -> calculateOptimalAngles());
    }
    
    private void initializeViews() {
        etLatitude = findViewById(R.id.et_latitude);
        etLongitude = findViewById(R.id.et_longitude);
        etPeakPower = findViewById(R.id.et_peak_power);
        etPanelArea = findViewById(R.id.et_panel_area);
        etEfficiency = findViewById(R.id.et_efficiency);
        etTempCoeff = findViewById(R.id.et_temp_coeff);
        etInverterPower = findViewById(R.id.et_inverter_power);
        etInverterEfficiency = findViewById(R.id.et_inverter_efficiency);
        etAlbedo = findViewById(R.id.et_albedo);
        btnCalculate = findViewById(R.id.btn_calculate);
        progressBar = findViewById(R.id.progress_bar);
        tvProgress = findViewById(R.id.tv_progress);
        tvResults = findViewById(R.id.tv_results);
    }
    
    private void setDefaultValues() {
        // Set some reasonable defaults
        etPeakPower.setText("5000"); // 5kW
        etPanelArea.setText("25"); // 25 m²
        etEfficiency.setText("20"); // 20%
        etTempCoeff.setText("-0.4"); // -0.4%/K
        etInverterPower.setText("5000"); // 5kW
        etInverterEfficiency.setText("95"); // 95%
        etAlbedo.setText("0.2"); // 0.2 (grass/soil)
    }
    
    private void calculateOptimalAngles() {
        try {
            // Get input values
            double latitude = Double.parseDouble(etLatitude.getText().toString());
            double longitude = Double.parseDouble(etLongitude.getText().toString());
            double peakPower = Double.parseDouble(etPeakPower.getText().toString());
            double panelArea = Double.parseDouble(etPanelArea.getText().toString());
            double efficiency = Double.parseDouble(etEfficiency.getText().toString());
            double tempCoeff = Double.parseDouble(etTempCoeff.getText().toString());
            double inverterPower = Double.parseDouble(etInverterPower.getText().toString());
            double inverterEfficiency = Double.parseDouble(etInverterEfficiency.getText().toString());
            double albedo = Double.parseDouble(etAlbedo.getText().toString());
            
            // Validate inputs
            if (latitude < -90 || latitude > 90) {
                Toast.makeText(this, "Latitude must be between -90 and 90", Toast.LENGTH_SHORT).show();
                return;
            }
            if (longitude < -180 || longitude > 180) {
                Toast.makeText(this, "Longitude must be between -180 and 180", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Show progress
            progressBar.setVisibility(View.VISIBLE);
            tvProgress.setVisibility(View.VISIBLE);
            tvResults.setVisibility(View.GONE);
            btnCalculate.setEnabled(false);
            
            // Create optimizer
            SolarAngleOptimizer optimizer = new SolarAngleOptimizer(
                latitude, longitude, peakPower, panelArea, efficiency, tempCoeff,
                inverterPower, inverterEfficiency, albedo
            );
            
            // Start optimization
            optimizer.optimizeAsync(new SolarAngleOptimizer.OptimizationCallback() {
                @Override
                public void onOptimizationComplete(SolarAngleOptimizer.OptimizationResult result) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        tvProgress.setVisibility(View.GONE);
                        tvResults.setVisibility(View.VISIBLE);
                        btnCalculate.setEnabled(true);
                        
                        // Display results
                        String resultText = String.format(Locale.getDefault(),
                            "Optimal Configuration:\n\n" +
                            "Azimuth: %.1f° (%s)\n" +
                            "Tilt: %.1f°\n\n" +
                            "Expected Annual Production: %.0f kWh\n" +
                            "Improvement vs Horizontal: %.1f%%\n\n" +
                            "Recommendations:\n" +
                            "• Face panels %s\n" +
                            "• Tilt at %.0f° from horizontal\n" +
                            "• For latitude %.2f°, this maximizes year-round production",
                            result.optimalAzimuth,
                            getDirectionName(result.optimalAzimuth),
                            result.optimalTilt,
                            result.annualEnergyOutput,
                            result.percentageImprovement,
                            getDirectionName(result.optimalAzimuth),
                            result.optimalTilt,
                            latitude
                        );
                        
                        tvResults.setText(resultText);
                    });
                }
                
                @Override
                public void onOptimizationError(String error) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        tvProgress.setVisibility(View.GONE);
                        btnCalculate.setEnabled(true);
                        Toast.makeText(OptimalAngleActivity.this, 
                            "Optimization failed: " + error, Toast.LENGTH_LONG).show();
                    });
                }
                
                @Override
                public void onProgressUpdate(int progress, int total) {
                    runOnUiThread(() -> {
                        int percentage = (progress * 100) / total;
                        tvProgress.setText(String.format(Locale.getDefault(),
                            "Calculating... %d%%", percentage));
                    });
                }
            });
            
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }
    
    private String getDirectionName(double azimuth) {
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