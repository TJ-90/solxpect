# Solar Panel Optimizer Web App

A modern web application for calculating optimal solar panel angles and analyzing historical solar energy production.

## Features

### 🎯 Optimal Angles Calculator
- Calculates optimal azimuth (compass direction) and tilt angles based on location
- Provides visual representations of panel orientation
- Shows required panel area based on system size and efficiency
- Includes location-specific recommendations

### 📊 Historical Analysis
- Fetches one year of historical weather data
- Calculates hourly power production
- Provides comprehensive visualizations:
  - Monthly production bar chart
  - Daily production heatmap
  - Key performance metrics
- Export data in Excel or CSV format

## Running the App

1. **Start the web app:**
   ```bash
   ./run_web_app.sh
   ```

2. **Open your browser:**
   Navigate to `http://localhost:8501`

## Using the App

### Input Parameters (Sidebar)
- **Location**: Enter latitude and longitude in decimal degrees
- **Panel Efficiency**: Select your panel efficiency (15-25%)
- **System Size**: Enter total system capacity in kW
- **System Efficiency**: Adjust for inverter and wiring losses
- **Temperature Coefficient**: Set the efficiency loss per degree

### Optimal Angles Tab
1. View calculated optimal azimuth and tilt angles
2. See visual representations of panel orientation
3. Get installation recommendations
4. Check required panel area

### Historical Analysis Tab
1. Click "Analyze Historical Data" button
2. Wait for data processing (fetches 1 year of weather data)
3. View annual/monthly/daily production metrics
4. Download detailed reports in Excel or CSV format

## Technical Details

- **Weather Data**: Open-Meteo Historical Weather API
- **Calculations**: Based on solar position algorithms and radiation models
- **Panel Assumptions**: 
  - Standard Test Conditions (STC): 1000 W/m² at 25°C
  - Default efficiency: 20-22%
  - Temperature coefficient: -0.4%/°C
  - Ground albedo: 0.2

## Modern UI/UX Features

- **Responsive Design**: Works on desktop and mobile devices
- **Interactive Visualizations**: Powered by Plotly
- **Clean Interface**: Material Design inspired
- **Real-time Updates**: Instant calculations as you adjust parameters
- **Data Export**: Download results for further analysis

## Requirements

- Python 3.x with virtual environment activated
- Dependencies installed (streamlit, pandas, plotly, requests)
- Internet connection for weather data API

## Tips

- For best results, use precise coordinates (4+ decimal places)
- The historical analysis uses actual weather data from the past year
- Export data to Excel for detailed analysis and custom calculations
- Multiply results by your actual system size for real production estimates