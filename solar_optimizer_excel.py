#!/usr/bin/env python3
"""
Solar Panel Optimizer
====================

This script calculates optimal solar panel orientation (azimuth and tilt angles)
for maximum energy production at any given location on Earth.

Key Features:
- Calculates optimal panel angles based on latitude
- Fetches historical weather data from Open-Meteo API
- Simulates hourly energy production for the past year
- Provides monthly and annual energy production estimates

Assumptions:
- 1kW panel capacity at Standard Test Conditions (STC: 1000 W/m² irradiance, 25°C)
- Panel area: 8 m² (typical for 1kW system with 20-22% efficiency)
- System efficiency: 95% (includes inverter losses, wiring losses, etc.)
- Temperature coefficient: -0.4%/°C (typical for silicon panels)
"""

import requests
import json
import math
import pandas as pd
from datetime import datetime, timedelta

def main():
    print("=== Solar Panel Optimizer ===")
    latitude = float(input("Enter latitude: "))
    longitude = float(input("Enter longitude: "))
    
    print("\nCalculating optimal angles for your location...\n")
    
    # Calculate optimal angles
    optimal_azimuth, optimal_tilt = calculate_optimal_angles(latitude, longitude)
    
    print("OPTIMAL CONFIGURATION:")
    print("=====================")
    print(f"Azimuth: {optimal_azimuth:.1f}° ({get_direction(optimal_azimuth)}-facing)")
    print(f"Tilt: {optimal_tilt:.1f}° from horizontal")
    print("\nRECOMMENDATIONS:")
    print(f"- Face panels {get_direction(optimal_azimuth)}")
    print(f"- Tilt at {round(optimal_tilt)}° angle")
    print("- This maximizes year-round energy production")
    
    print("\nFetching last year's weather data...")
    
    try:
        analyze_last_year_production(latitude, longitude, optimal_azimuth, optimal_tilt)
    except Exception as e:
        print(f"Error fetching weather data: {e}")

def calculate_optimal_angles(latitude, longitude):
    """
    Calculate optimal azimuth and tilt angles for solar panels.
    
    The optimization is based on maximizing year-round energy production.
    
    Parameters:
    -----------
    latitude : float
        Location latitude in decimal degrees (-90 to 90)
    longitude : float
        Location longitude in decimal degrees (-180 to 180)
        
    Returns:
    --------
    tuple : (optimal_azimuth, optimal_tilt)
        - optimal_azimuth: Direction panels should face (0=North, 180=South)
        - optimal_tilt: Angle from horizontal (0=flat, 90=vertical)
    """
    
    # Azimuth calculation: Panels should face the equator
    # This maximizes exposure to the sun's path across the sky
    if latitude >= 0:
        # Northern hemisphere - face south (180°)
        optimal_azimuth = 180.0
    else:
        # Southern hemisphere - face north (0°)
        optimal_azimuth = 0.0
    
    # Tilt angle calculation based on latitude zones
    # The optimal tilt balances summer vs winter sun angles
    abs_latitude = abs(latitude)
    
    if abs_latitude < 25:
        # Tropical regions (0-25°): Sun is nearly overhead year-round
        # Use factor of 0.87 to optimize for high sun angles
        optimal_tilt = abs_latitude * 0.87
    elif abs_latitude < 50:
        # Temperate regions (25-50°): Standard rule applies
        # Tilt = Latitude optimizes year-round production
        optimal_tilt = abs_latitude
    else:
        # High latitudes (50-90°): Long summer days, low winter sun
        # Reduce tilt slightly (factor 0.9) to capture more summer sun
        optimal_tilt = abs_latitude * 0.9
    
    return optimal_azimuth, optimal_tilt

def analyze_last_year_production(latitude, longitude, azimuth, tilt):
    """
    Fetch and analyze last year's solar production data.
    Also exports hourly data to Excel file.
    """
    """Fetch and analyze last year's solar production data"""
    
    # Get last year's date range
    end_date = datetime.now() - timedelta(days=2)  # API has 2-day delay
    start_date = end_date - timedelta(days=365)
    
    # Build API URL
    url = (f"https://archive-api.open-meteo.com/v1/archive?"
           f"latitude={latitude:.6f}&longitude={longitude:.6f}"
           f"&start_date={start_date.strftime('%Y-%m-%d')}"
           f"&end_date={end_date.strftime('%Y-%m-%d')}"
           f"&hourly=temperature_2m,diffuse_radiation,direct_normal_irradiance,shortwave_radiation"
           f"&timezone=auto")
    
    # Fetch data
    response = requests.get(url)
    if response.status_code != 200:
        raise Exception(f"API request failed with status {response.status_code}")
    
    data = response.json()
    hourly = data['hourly']
    
    # Process the data
    times = hourly['time']
    temperatures = hourly['temperature_2m']
    diffuse_radiation = hourly['diffuse_radiation']
    direct_normal = hourly['direct_normal_irradiance']
    shortwave = hourly['shortwave_radiation']
    
    # Calculate production
    total_energy = 0
    monthly_energy = [0] * 12
    daily_energy = {}
    debug_counter = 0
    
    # Lists to store data for Excel export
    excel_data = []
    
    for i in range(len(times)):
        # Parse timestamp
        timestamp = datetime.fromisoformat(times[i].replace('T', ' '))
        
        # Get values (handle None values)
        temp = temperatures[i] if temperatures[i] is not None else 20
        diffuse = diffuse_radiation[i] if diffuse_radiation[i] is not None else 0
        direct = direct_normal[i] if direct_normal[i] is not None else 0
        sw = shortwave[i] if shortwave[i] is not None else 0
        
        # Calculate power
        power = calculate_power(latitude, longitude, azimuth, tilt,
                              direct, diffuse, sw, timestamp, temp)
        
        # Store data for Excel export
        excel_data.append({
            'Timestamp': timestamp,
            'Date': timestamp.date(),
            'Hour': timestamp.hour,
            'Temperature (°C)': temp,
            'Direct Normal Irradiance (W/m²)': direct,
            'Diffuse Radiation (W/m²)': diffuse,
            'Global Horizontal Irradiance (W/m²)': sw,
            'Power Output (W)': power,
            'Energy (Wh)': power  # Hourly data, so W = Wh
        })
        
        total_energy += power
        monthly_energy[timestamp.month - 1] += power
        
        # Track daily totals
        date_key = timestamp.date()
        daily_energy[date_key] = daily_energy.get(date_key, 0) + power
    
    # Find peak day
    peak_day = max(daily_energy, key=daily_energy.get)
    peak_energy = daily_energy[peak_day]
    
    # Display results
    print("\nPRODUCTION ANALYSIS (Last 12 Months):")
    print("=====================================")
    print(f"Total Annual Production: {total_energy/1000:.0f} kWh")
    print(f"Average Daily Production: {total_energy/1000/365:.1f} kWh")
    print(f"Peak Daily Production: {peak_energy/1000:.1f} kWh on {peak_day}")
    
    print("\nMonthly Breakdown (kWh):")
    global month_names
    month_names = ["Jan", "Feb", "Mar", "Apr", "May", "Jun",
                   "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
    for i in range(12):
        print(f"{month_names[i]}: {monthly_energy[i]/1000:.0f} kWh")
    
    print("\nNOTE: Based on 1kW panel capacity at standard conditions")
    print("Multiply by your actual system size for real production estimates")
    
    # Create DataFrame and export to Excel
    df = pd.DataFrame(excel_data)
    
    # Create daily summary
    daily_df = df.groupby('Date').agg({
        'Energy (Wh)': 'sum',
        'Temperature (°C)': 'mean',
        'Direct Normal Irradiance (W/m²)': 'mean',
        'Diffuse Radiation (W/m²)': 'mean',
        'Global Horizontal Irradiance (W/m²)': 'mean'
    }).round(2)
    daily_df.rename(columns={'Energy (Wh)': 'Daily Energy (Wh)'}, inplace=True)
    daily_df['Daily Energy (kWh)'] = (daily_df['Daily Energy (Wh)'] / 1000).round(3)
    
    # Save to Excel with multiple sheets
    filename = f"solar_production_{latitude}_{longitude}_{datetime.now().strftime('%Y%m%d_%H%M%S')}.xlsx"
    with pd.ExcelWriter(filename, engine='openpyxl') as writer:
        # Hourly data sheet
        df.to_excel(writer, sheet_name='Hourly Data', index=False)
        
        # Daily summary sheet
        daily_df.to_excel(writer, sheet_name='Daily Summary')
        
        # Monthly summary sheet
        monthly_df = pd.DataFrame({
            'Month': month_names,
            'Energy (kWh)': [energy/1000 for energy in monthly_energy]
        })
        monthly_df.to_excel(writer, sheet_name='Monthly Summary', index=False)
        
        # System parameters sheet
        params_df = pd.DataFrame({
            'Parameter': ['Latitude', 'Longitude', 'Panel Azimuth', 'Panel Tilt', 
                         'System Capacity', 'System Efficiency', 'Temperature Coefficient'],
            'Value': [latitude, longitude, azimuth, tilt, '1 kW', '95%', '-0.4%/°C'],
            'Unit': ['degrees', 'degrees', 'degrees', 'degrees', '', '', '']
        })
        params_df.to_excel(writer, sheet_name='System Parameters', index=False)
    
    print(f"\nData exported to: {filename}")

def calculate_power(lat, lon, azimuth, tilt, direct_normal, diffuse, shortwave, 
                   timestamp, temperature):
    """
    Calculate instantaneous solar panel power output.
    
    This function simulates the power generation of a solar panel based on:
    - Sun position (calculated from location and time)
    - Panel orientation (azimuth and tilt)
    - Solar irradiance components (direct, diffuse, reflected)
    - Temperature effects on panel efficiency
    
    Parameters:
    -----------
    lat, lon : float
        Location coordinates in decimal degrees
    azimuth : float
        Panel azimuth angle (0=North, 180=South)
    tilt : float
        Panel tilt angle from horizontal (0=flat, 90=vertical)
    direct_normal : float
        Direct Normal Irradiance (DNI) in W/m²
    diffuse : float
        Diffuse Horizontal Irradiance (DHI) in W/m²
    shortwave : float
        Global Horizontal Irradiance (GHI) in W/m²
    timestamp : datetime
        Current time for sun position calculation
    temperature : float
        Ambient temperature in °C
        
    Returns:
    --------
    float : Power output in Watts
    """
    
    # Calculate sun position
    day_of_year = timestamp.timetuple().tm_yday
    hour = timestamp.hour + timestamp.minute / 60.0
    
    # Solar declination: Earth's tilt relative to sun
    # Varies between +23.45° (summer solstice) and -23.45° (winter solstice)
    declination = 23.45 * math.sin(math.radians((284 + day_of_year) * 360.0 / 365))
    
    # Hour angle: Sun's position relative to solar noon
    # Each hour = 15° of Earth's rotation (360°/24h = 15°/h)
    hour_angle = (hour - 12) * 15
    
    # Solar elevation
    elevation = math.degrees(math.asin(
        math.sin(math.radians(declination)) * math.sin(math.radians(lat)) +
        math.cos(math.radians(declination)) * math.cos(math.radians(lat)) * 
        math.cos(math.radians(hour_angle))
    ))
    
    if elevation <= 0:
        return 0  # Sun below horizon
    
    # Solar azimuth (simplified)
    solar_azimuth = math.degrees(math.atan2(
        -math.sin(math.radians(hour_angle)),
        math.tan(math.radians(declination)) * math.cos(math.radians(lat)) -
        math.sin(math.radians(lat)) * math.cos(math.radians(hour_angle))
    ))
    if solar_azimuth < 0:
        solar_azimuth += 360
    
    # Calculate angle of incidence
    angle_of_incidence = calculate_angle_of_incidence(
        solar_azimuth, elevation, azimuth, tilt
    )
    
    cosine_factor = max(0, math.cos(math.radians(angle_of_incidence)))
    
    # Total radiation on tilted panel surface
    # Three components of solar radiation:
    # 1. Direct beam radiation: DNI * cos(angle_of_incidence)
    # 2. Diffuse sky radiation: DHI * view_factor_sky
    # 3. Ground reflected radiation: GHI * albedo * view_factor_ground
    total_radiation = (
        direct_normal * cosine_factor +                              # Direct component
        diffuse * (1 + math.cos(math.radians(tilt))) / 2 +         # Diffuse component
        shortwave * 0.2 * (1 - math.cos(math.radians(tilt))) / 2   # Reflected (albedo=0.2)
    )
    
    # Temperature effect on panel efficiency
    # Solar panels lose efficiency as temperature increases
    # Typical temperature coefficient: -0.4%/°C (relative to 25°C STC)
    temp_effect = 1 + (temperature - 25) * (-0.004)
    
    # Power output calculation
    # Assumptions:
    # - 1kW rated panel capacity at STC (1000 W/m² irradiance, 25°C)
    # - Panel area: 8 m² (based on typical 20-22% panel efficiency)
    # - System efficiency: 95% (accounts for inverter, wiring, soiling losses)
    # Note: total_radiation already accounts for the actual irradiance on the tilted surface
    power = total_radiation * temp_effect * 0.95  # Output in Watts
    
    return max(0, power)

def calculate_angle_of_incidence(sun_azimuth, sun_elevation, panel_azimuth, panel_tilt):
    """Calculate angle between sun vector and panel normal"""
    
    # Convert to radians
    sa = math.radians(sun_azimuth)
    se = math.radians(sun_elevation)
    pa = math.radians(panel_azimuth)
    pt = math.radians(panel_tilt)
    
    # Calculate angle between sun vector and panel normal
    cos_incidence = (math.sin(se) * math.cos(pt) +
                    math.cos(se) * math.sin(pt) * math.cos(sa - pa))
    
    # Clamp to [-1, 1] to avoid numerical errors
    cos_incidence = max(-1, min(1, cos_incidence))
    
    return math.degrees(math.acos(cos_incidence))

def get_direction(azimuth):
    """Convert azimuth to compass direction"""
    if azimuth >= 337.5 or azimuth < 22.5:
        return "North"
    elif azimuth >= 22.5 and azimuth < 67.5:
        return "Northeast"
    elif azimuth >= 67.5 and azimuth < 112.5:
        return "East"
    elif azimuth >= 112.5 and azimuth < 157.5:
        return "Southeast"
    elif azimuth >= 157.5 and azimuth < 202.5:
        return "South"
    elif azimuth >= 202.5 and azimuth < 247.5:
        return "Southwest"
    elif azimuth >= 247.5 and azimuth < 292.5:
        return "West"
    elif azimuth >= 292.5 and azimuth < 337.5:
        return "Northwest"
    else:
        return "Unknown"

if __name__ == "__main__":
    main()