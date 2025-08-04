# Solar Panel Calculations Explained

## Overview
The solar optimizer calculates optimal panel orientation and estimates energy production using physics-based models and real weather data.

## Key Assumptions

### Panel Specifications
- **Rated Power**: 1kW at Standard Test Conditions (STC)
- **STC Definition**: 1000 W/m² irradiance, 25°C cell temperature, AM1.5 spectrum
- **Panel Area**: 8 m²
  - Based on typical commercial panel efficiency of 20-22%
  - This accounts for real-world panel sizing with appropriate spacing
- **System Efficiency**: 95%
  - Accounts for inverter losses (~2-3%)
  - Wiring losses (~1-2%)
  - Other system losses

### Environmental Parameters
- **Temperature Coefficient**: -0.4%/°C
  - Typical for crystalline silicon panels
  - Power decreases 0.4% for each degree above 25°C
- **Ground Albedo**: 0.2 (20%)
  - Typical for grass, soil, or concrete
  - Affects ground-reflected radiation

## Optimal Angle Calculations

### Azimuth (Compass Direction)
The optimal azimuth is simple:
- **Northern Hemisphere**: Face South (180°)
- **Southern Hemisphere**: Face North (0°)

This ensures panels face the equator where the sun's path is concentrated.

### Tilt Angle
The optimal tilt balances summer and winter sun angles:

1. **Tropical Regions (0-25° latitude)**
   - Formula: Tilt = Latitude × 0.87
   - Example: 10° latitude → 8.7° tilt
   - Reasoning: Sun is nearly overhead year-round

2. **Temperate Regions (25-50° latitude)**
   - Formula: Tilt = Latitude
   - Example: 40° latitude → 40° tilt
   - Reasoning: Standard rule for balanced production

3. **High Latitudes (50-90°)**
   - Formula: Tilt = Latitude × 0.9
   - Example: 60° latitude → 54° tilt
   - Reasoning: Favor summer production with long days

## Power Calculation Details

### Solar Radiation Components

1. **Direct Beam Radiation (DNI)**
   - Sunlight traveling directly from sun to panel
   - Depends on angle between sun and panel normal
   - Formula: DNI × cos(angle_of_incidence)

2. **Diffuse Sky Radiation (DHI)**
   - Scattered light from the sky dome
   - View factor: (1 + cos(tilt)) / 2
   - More important on cloudy days

3. **Ground-Reflected Radiation**
   - Light bouncing off the ground
   - Formula: GHI × albedo × (1 - cos(tilt)) / 2
   - GHI = Global Horizontal Irradiance

### Temperature Effects
Power output decreases with temperature:
```
Temperature Factor = 1 + (T - 25°C) × (-0.004)
```

Example: At 35°C:
- Temperature Factor = 1 + (35 - 25) × (-0.004) = 0.96
- Power reduced by 4%

### Final Power Equation
```
Power (W) = Total_Radiation × Temperature_Factor × System_Efficiency

Where:
Total_Radiation = Direct + Diffuse + Reflected components
```

## Example Calculation

For your Kerala location (8.5°N, 77°E):
- **Optimal Azimuth**: 180° (South-facing)
- **Optimal Tilt**: 7.4° (8.5 × 0.87)
- **Annual Production**: ~1786 kWh/year per kW installed
- **Daily Average**: ~4.9 kWh/day per kW installed

### Scaling to Your System
If you have a 5kW system:
- Annual Production: 1786 × 5 = 8,930 kWh
- Daily Average: 4.9 × 5 = 24.5 kWh

## Validation
The model's accuracy depends on:
1. Weather data quality from Open-Meteo
2. Simplified sun position calculations
3. Assumed system parameters

For professional installations, consider:
- Local shading analysis
- Actual panel specifications
- Detailed system design software