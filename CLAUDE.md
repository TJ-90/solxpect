# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Build
```bash
./gradlew build
```

### Clean
```bash
./gradlew clean
```

### Run app (install on connected device/emulator)
```bash
./gradlew installDebug
```

## Architecture

### Overview
solXpect is an Android application that forecasts solar power plant output using radiation data from Open-Meteo.com. The app calculates solar panel energy production based on panel orientation, location, and environmental factors.

### Key Components

1. **Core Calculation Engine** (`SolarPowerPlant.java`)
   - Calculates power output based on solar position, panel orientation, and shading
   - Uses the solarpositioning library for accurate sun position calculations
   - Accounts for direct, diffuse, and reflected radiation components

2. **Data Architecture**
   - SQLite database for storing locations, forecasts, and power plant parameters
   - Weather data fetched from Open-Meteo API using Volley
   - Database models: City, CityToWatch, HourlyForecast, WeekForecast

3. **UI Structure**
   - Main navigation through `NavigationActivity` with ViewPager
   - Weather city fragments display forecast data
   - Settings allow configuration of multiple solar panel installations
   - Support for "show sum" mode to aggregate multiple panels at same location

4. **API Integration**
   - Weather data: Open-Meteo API (forecast endpoint)
   - Geocoding: Open-Meteo Geocoding API
   - Map tiles: OpenStreetMap

### Important Technical Details

- Minimum SDK: 26 (Android 8.0)
- Target SDK: 35 (Android 15)
- Uses AndroidX libraries and Material Design components
- Power calculations consider:
  - Panel azimuth and tilt angles
  - Shading profiles (elevation-based with opacity)
  - Temperature coefficients
  - Inverter efficiency and power limits
  - Albedo for ground reflections

## Solar Optimizer Tools

### Python Solar Optimizer (`solar_optimizer.py`)
A standalone Python script that calculates optimal solar panel orientation and estimates energy production based on historical weather data.

#### Running the Optimizer
```bash
# Interactive mode (enter coordinates manually)
./run_optimizer_interactive.sh

# Automated mode with example coordinates (San Francisco)
./run_solar_optimizer.sh
```

#### Dependencies
- Python 3.x
- requests library (installed in virtual environment)

#### Key Features
- Calculates optimal azimuth (compass direction) and tilt angles
- Fetches one year of historical weather data from Open-Meteo API
- Simulates hourly power production
- Provides monthly and annual energy estimates

#### Technical Details
- **Panel Assumptions**: 1kW rated capacity at STC (Standard Test Conditions)
- **Panel Area**: 8 m² (typical for 1kW system with 20-22% efficiency)
- **System Efficiency**: 95% (includes inverter and wiring losses)
- **Temperature Coefficient**: -0.4%/°C (efficiency loss per degree above 25°C)
- **Ground Albedo**: 0.2 (20% reflectance for typical ground surfaces)

#### Calculation Methods
1. **Optimal Azimuth**: Points panels toward equator (180° in Northern hemisphere, 0° in Southern)
2. **Optimal Tilt**: 
   - Tropical (0-25° latitude): Latitude × 0.87
   - Temperate (25-50° latitude): Equal to latitude
   - High latitude (50-90°): Latitude × 0.9
3. **Power Calculation**: Considers direct beam, diffuse sky, and ground-reflected radiation