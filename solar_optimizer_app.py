#!/usr/bin/env python3
"""
Solar Panel Optimizer Web App
============================

A modern Streamlit web application for calculating optimal solar panel angles
and analyzing historical solar production data.
"""

import streamlit as st
import pandas as pd
import plotly.graph_objects as go
import plotly.express as px
import requests
import math
from datetime import datetime, timedelta
import io
import numpy as np

# Page config
st.set_page_config(
    page_title="Solar Panel Optimizer",
    page_icon="☀️",
    layout="wide",
    initial_sidebar_state="expanded"
)

# Custom CSS for modern UI/UX
st.markdown("""
<style>
    /* Main container styling */
    .stApp {
        background-color: #f8f9fa;
    }
    
    /* Header styling */
    .main-header {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        padding: 2rem;
        border-radius: 10px;
        margin-bottom: 2rem;
        text-align: center;
    }
    
    /* Card styling */
    .metric-card {
        background: white;
        padding: 1.5rem;
        border-radius: 10px;
        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        margin-bottom: 1rem;
    }
    
    /* Button styling */
    .stButton > button {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        border: none;
        padding: 0.5rem 2rem;
        border-radius: 25px;
        font-weight: 600;
        transition: all 0.3s ease;
    }
    
    .stButton > button:hover {
        transform: translateY(-2px);
        box-shadow: 0 5px 15px rgba(0,0,0,0.2);
    }
    
    /* Info box styling */
    .info-box {
        background: #e3f2fd;
        border-left: 4px solid #2196f3;
        padding: 1rem;
        border-radius: 5px;
        margin: 1rem 0;
        color: #000000 !important;
    }
    
    .info-box h4 {
        color: #000000 !important;
        margin-bottom: 0.5rem;
    }
    
    .info-box ul {
        color: #000000 !important;
        margin: 0;
    }
    
    .info-box li {
        color: #000000 !important;
    }
    
    /* Success box styling */
    .success-box {
        background: #e8f5e9;
        border-left: 4px solid #4caf50;
        padding: 1rem;
        border-radius: 5px;
        margin: 1rem 0;
        color: #000000 !important;
    }
    
    .success-box h4 {
        color: #000000 !important;
        margin-bottom: 0.5rem;
    }
    
    .success-box ul {
        color: #000000 !important;
        margin: 0;
    }
    
    .success-box li {
        color: #000000 !important;
    }
    
    /* Force all text to be black in custom divs */
    .info-box *, .success-box * {
        color: #000000 !important;
    }
    
    /* Tab styling */
    .stTabs [data-baseweb="tab-list"] {
        gap: 8px;
    }
    
    .stTabs [data-baseweb="tab"] {
        color: #000000 !important;
        font-weight: 600;
    }
    
    .stTabs [data-baseweb="tab"]:hover {
        color: #667eea !important;
    }
    
    .stTabs [aria-selected="true"] {
        color: #ffffff !important;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
        border-radius: 4px;
    }
    
    .stTabs [aria-selected="true"] span {
        color: #ffffff !important;
    }
    
    /* Tab underline color */
    .stTabs [data-baseweb="tab-highlight"] {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
    }
    
    /* Sidebar styling - force dark background with white text */
    [data-testid="stSidebar"] {
        background-color: #1e293b !important;
    }
    
    [data-testid="stSidebar"] > div {
        background-color: #1e293b !important;
    }
    
    [data-testid="stSidebar"] [data-testid="stSidebarContent"] {
        background-color: #1e293b !important;
    }
    
    /* All sidebar text should be white */
    [data-testid="stSidebar"] * {
        color: #f8fafc !important;
    }
    
    [data-testid="stSidebar"] h1, [data-testid="stSidebar"] h2,
    [data-testid="stSidebar"] h3, [data-testid="stSidebar"] h4,
    [data-testid="stSidebar"] h5, [data-testid="stSidebar"] h6 {
        color: #ffffff !important;
    }
    
    [data-testid="stSidebar"] p, [data-testid="stSidebar"] label,
    [data-testid="stSidebar"] .stMarkdown {
        color: #f8fafc !important;
    }
    
    /* Input fields in sidebar - dark background */
    [data-testid="stSidebar"] input[type="number"],
    [data-testid="stSidebar"] input[type="text"] {
        background-color: #334155 !important;
        color: #ffffff !important;
        border-color: #475569 !important;
    }
    
    /* Slider labels and values */
    [data-testid="stSidebar"] .stSlider label {
        color: #f8fafc !important;
    }
    
    [data-testid="stSidebar"] .stSlider div[data-testid="stTickBarMax"],
    [data-testid="stSidebar"] .stSlider div[data-testid="stTickBarMin"] {
        color: #cbd5e1 !important;
    }
    
    /* Expander in sidebar */
    [data-testid="stSidebar"] .streamlit-expanderHeader {
        background-color: #334155 !important;
        color: #f8fafc !important;
    }
    
    [data-testid="stSidebar"] .streamlit-expanderContent {
        background-color: #2d3748 !important;
    }
    
    /* Number input +/- buttons in sidebar - match slider text color */
    [data-testid="stSidebar"] .stNumberInput button,
    [data-testid="stSidebar"] button[title="Increment value"],
    [data-testid="stSidebar"] button[title="Decrement value"] {
        background-color: transparent !important;
        color: #cbd5e1 !important;
        border: 1px solid #475569 !important;
    }
    
    [data-testid="stSidebar"] .stNumberInput button:hover,
    [data-testid="stSidebar"] button[title="Increment value"]:hover,
    [data-testid="stSidebar"] button[title="Decrement value"]:hover {
        background-color: #334155 !important;
        color: #f8fafc !important;
    }
    
    /* Force the +/- text to match slider value color */
    [data-testid="stSidebar"] .stNumberInput button span,
    [data-testid="stSidebar"] .stNumberInput button div {
        color: inherit !important;
    }
    
    /* Info/Success/Error messages in sidebar - white text */
    [data-testid="stSidebar"] .stAlert,
    [data-testid="stSidebar"] [data-testid="stAlert"],
    [data-testid="stSidebar"] .stInfo,
    [data-testid="stSidebar"] .stSuccess,
    [data-testid="stSidebar"] .stWarning,
    [data-testid="stSidebar"] .stError {
        color: #ffffff !important;
    }
    
    [data-testid="stSidebar"] .stAlert p,
    [data-testid="stSidebar"] [data-testid="stAlert"] p {
        color: #ffffff !important;
    }
    
    /* Main content area - keep black text */
    .main .element-container {
        color: #000000;
    }
    
    /* Ensure all headings in main area are black */
    .main h1, .main h2, .main h3, .main h4, .main h5, .main h6 {
        color: #000000 !important;
    }
    
    /* Ensure all paragraphs and lists in main area are black */
    .main p, .main li, .main span {
        color: #000000;
    }
    
    /* Success/Error/Info/Warning messages - Alert components */
    .stAlert {
        color: #000000 !important;
    }
    
    .stAlert > div {
        color: #000000 !important;
    }
    
    div[data-testid="stAlert"] {
        color: #000000 !important;
    }
    
    div[data-testid="stAlert"] * {
        color: #000000 !important;
    }
    
    /* Metric components - all variations */
    [data-testid="metric-container"] {
        color: #000000 !important;
    }
    
    [data-testid="metric-container"] > div {
        color: #000000 !important;
    }
    
    [data-testid="metric-container"] label {
        color: #000000 !important;
    }
    
    [data-testid="metric-container"] [data-testid="stMetricValue"] {
        color: #000000 !important;
    }
    
    [data-testid="metric-container"] [data-testid="stMetricDelta"] svg {
        display: none;
    }
    
    [data-testid="stMetricDelta"] > div {
        color: #000000 !important;
    }
    
    /* Force all text in metric containers to be black */
    div[data-testid="metric-container"] * {
        color: #000000 !important;
    }
    
    /* Additional targeting for metric text */
    [data-testid="stMetricLabel"] {
        color: #000000 !important;
    }
    
    div[data-testid="stMetricValue"] > div {
        color: #000000 !important;
    }
    
    /* Target the specific metric delta container */
    [data-testid="stMetricDeltaIcon-Up"], 
    [data-testid="stMetricDeltaIcon-Down"] {
        color: #000000 !important;
    }
    
    /* Make sure the text after icons is black */
    [data-testid="stMetricDelta"] span {
        color: #000000 !important;
    }
</style>
""", unsafe_allow_html=True)

# Header
st.markdown("""
<div class="main-header">
    <h1>☀️ Solar Panel Optimizer</h1>
    <p>Calculate optimal panel angles and analyze solar energy production</p>
</div>
""", unsafe_allow_html=True)

# Sidebar for inputs
with st.sidebar:
    st.header("📍 Location & Settings")
    
    # Location inputs
    col1, col2 = st.columns(2)
    with col1:
        latitude = st.number_input(
            "Latitude",
            min_value=-90.0,
            max_value=90.0,
            value=37.7749,
            step=0.0001,
            format="%.4f",
            help="Enter latitude in decimal degrees"
        )
    
    with col2:
        longitude = st.number_input(
            "Longitude",
            min_value=-180.0,
            max_value=180.0,
            value=-122.4194,
            step=0.0001,
            format="%.4f",
            help="Enter longitude in decimal degrees"
        )
    
    # Panel specifications
    st.subheader("🔧 Panel Specifications")
    
    panel_efficiency = st.slider(
        "Panel Efficiency (%)",
        min_value=15,
        max_value=25,
        value=21,
        step=1,
        help="Typical modern panels: 20-22%"
    )
    
    system_size = st.number_input(
        "System Size (kW)",
        min_value=0.1,
        max_value=100.0,
        value=5.0,
        step=0.1,
        help="Total installed capacity"
    )
    
    # Additional settings
    st.subheader("⚙️ Advanced Settings")
    
    system_efficiency = st.slider(
        "System Efficiency (%)",
        min_value=85,
        max_value=98,
        value=95,
        step=1,
        help="Includes inverter and wiring losses"
    )
    
    # Electricity bill section
    with st.expander("💰 Electricity Rate Calculator"):
        st.markdown("Calculate your actual electricity rate from your bill")
        
        col1, col2 = st.columns(2)
        with col1:
            last_bill = st.number_input(
                "Last Month's Bill ($)",
                min_value=0.0,
                max_value=10000.0,
                value=0.0,
                step=0.01,
                help="Total amount in dollars"
            )
        
        with col2:
            last_kwh = st.number_input(
                "Last Month's Usage (kWh)",
                min_value=0.0,
                max_value=50000.0,
                value=0.0,
                step=1.0,
                help="Total kWh consumed"
            )
        
        # Calculate rate if both values provided
        if last_bill > 0 and last_kwh > 0:
            calculated_rate = last_bill / last_kwh
            st.success(f"📊 Your electricity rate: ${calculated_rate:.4f}/kWh")
            electricity_rate = calculated_rate
        else:
            electricity_rate = 0.12

# Main content area
tab1, tab2 = st.tabs(["🎯 Optimal Angles", "📊 Historical Analysis"])

# Helper functions
def calculate_optimal_angles(latitude):
    """Calculate optimal azimuth and tilt angles"""
    if latitude >= 0:
        optimal_azimuth = 180.0
        hemisphere = "Northern"
        direction = "South"
    else:
        optimal_azimuth = 0.0
        hemisphere = "Southern"
        direction = "North"
    
    abs_latitude = abs(latitude)
    
    if abs_latitude < 25:
        optimal_tilt = abs_latitude * 0.87
        zone = "Tropical"
    elif abs_latitude < 50:
        optimal_tilt = abs_latitude
        zone = "Temperate"
    else:
        optimal_tilt = abs_latitude * 0.9
        zone = "High Latitude"
    
    return optimal_azimuth, optimal_tilt, hemisphere, direction, zone

def calculate_temperature_coefficient(latitude):
    """
    Calculate temperature coefficient based on latitude.
    Hotter climates (lower latitudes) have worse temperature coefficients.
    """
    abs_latitude = abs(latitude)
    
    if abs_latitude < 15:
        # Equatorial/Tropical - highest temperatures, worst coefficient
        return -0.45
    elif abs_latitude < 30:
        # Subtropical - hot climates
        return -0.42
    elif abs_latitude < 45:
        # Temperate - moderate climates
        return -0.40
    elif abs_latitude < 60:
        # Cool temperate
        return -0.38
    else:
        # Cold/Arctic - best coefficient due to cooler temperatures
        return -0.35

def calculate_panel_area(system_size_kw, efficiency_percent):
    """Calculate required panel area"""
    # 1000 W/m² is standard test condition irradiance
    # Area = Power / (Irradiance × Efficiency)
    area = (system_size_kw * 1000) / (1000 * (efficiency_percent / 100))
    return area

def fetch_weather_data(latitude, longitude):
    """Fetch historical weather data"""
    end_date = datetime.now() - timedelta(days=2)
    start_date = end_date - timedelta(days=365)
    
    url = (f"https://archive-api.open-meteo.com/v1/archive?"
           f"latitude={latitude:.6f}&longitude={longitude:.6f}"
           f"&start_date={start_date.strftime('%Y-%m-%d')}"
           f"&end_date={end_date.strftime('%Y-%m-%d')}"
           f"&hourly=temperature_2m,diffuse_radiation,direct_normal_irradiance,shortwave_radiation"
           f"&timezone=auto")
    
    response = requests.get(url)
    if response.status_code != 200:
        raise Exception(f"API request failed with status {response.status_code}")
    
    return response.json()

def calculate_solar_power(lat, lon, azimuth, tilt, direct_normal, diffuse, shortwave, 
                         timestamp, temperature, temp_coefficient):
    """Calculate solar panel power output"""
    # Solar position calculations
    day_of_year = timestamp.timetuple().tm_yday
    hour = timestamp.hour + timestamp.minute / 60.0
    
    declination = 23.45 * math.sin(math.radians((284 + day_of_year) * 360.0 / 365))
    hour_angle = (hour - 12) * 15
    
    elevation = math.degrees(math.asin(
        math.sin(math.radians(declination)) * math.sin(math.radians(lat)) +
        math.cos(math.radians(declination)) * math.cos(math.radians(lat)) * 
        math.cos(math.radians(hour_angle))
    ))
    
    if elevation <= 0:
        return 0
    
    # Simplified solar azimuth
    solar_azimuth = math.degrees(math.atan2(
        -math.sin(math.radians(hour_angle)),
        math.tan(math.radians(declination)) * math.cos(math.radians(lat)) -
        math.sin(math.radians(lat)) * math.cos(math.radians(hour_angle))
    ))
    if solar_azimuth < 0:
        solar_azimuth += 360
    
    # Angle of incidence calculations
    sa = math.radians(solar_azimuth)
    se = math.radians(elevation)
    pa = math.radians(azimuth)
    pt = math.radians(tilt)
    
    cos_incidence = (math.sin(se) * math.cos(pt) +
                    math.cos(se) * math.sin(pt) * math.cos(sa - pa))
    cos_incidence = max(-1, min(1, cos_incidence))
    angle_of_incidence = math.degrees(math.acos(cos_incidence))
    
    cosine_factor = max(0, math.cos(math.radians(angle_of_incidence)))
    
    # Total radiation on panel
    total_radiation = (
        direct_normal * cosine_factor +
        diffuse * (1 + math.cos(math.radians(tilt))) / 2 +
        shortwave * 0.2 * (1 - math.cos(math.radians(tilt))) / 2
    )
    
    # Temperature effect
    temp_effect = 1 + (temperature - 25) * (temp_coefficient / 100)
    
    # Power output
    power = total_radiation * temp_effect
    
    return max(0, power)

# Tab 1: Optimal Angles
with tab1:
    col1, col2 = st.columns([2, 1])
    
    with col1:
        st.header("Optimal Panel Configuration")
        
        # Calculate optimal angles and temperature coefficient
        azimuth, tilt, hemisphere, direction, zone = calculate_optimal_angles(latitude)
        temp_coefficient = calculate_temperature_coefficient(latitude)
        panel_area = calculate_panel_area(system_size, panel_efficiency)
        
        # Display results in metric cards
        col_a, col_b, col_c = st.columns(3)
        
        with col_a:
            st.metric(
                label="Optimal Azimuth",
                value=f"{azimuth:.0f}°",
                delta=f"{direction}-facing"
            )
        
        with col_b:
            st.metric(
                label="Optimal Tilt",
                value=f"{tilt:.1f}°",
                delta="from horizontal"
            )
        
        with col_c:
            st.metric(
                label="Required Area",
                value=f"{panel_area:.1f} m²",
                delta=f"for {system_size} kW"
            )
        
        # Location info
        st.markdown(f"""
        <div class="info-box">
            <h4>📍 Location Analysis</h4>
            <ul>
                <li><strong>Coordinates:</strong> {latitude:.4f}°, {longitude:.4f}°</li>
                <li><strong>Hemisphere:</strong> {hemisphere}</li>
                <li><strong>Climate Zone:</strong> {zone}</li>
                <li><strong>Panel Direction:</strong> Face panels toward the {direction}</li>
                <li><strong>Temperature Coefficient:</strong> {temp_coefficient:.2f}%/°C (auto-calculated)</li>
            </ul>
        </div>
        """, unsafe_allow_html=True)
        
        # Recommendations
        st.markdown(f"""
        <div class="success-box">
            <h4>✅ Installation Recommendations</h4>
            <ul>
                <li>Orient panels {azimuth:.0f}° from North ({direction}-facing)</li>
                <li>Tilt panels at {tilt:.0f}° angle from horizontal</li>
                <li>Install {panel_area:.1f} m² of panels for {system_size} kW capacity</li>
                <li>This configuration maximizes year-round energy production</li>
            </ul>
        </div>
        """, unsafe_allow_html=True)
    
    with col2:
        # Visual representation
        st.subheader("Panel Orientation Diagram")
        
        # Create a 3D visualization of panel orientation
        fig = go.Figure()
        
        # Add compass directions
        fig.add_trace(go.Scatterpolar(
            r=[1, 1, 1, 1],
            theta=[0, 90, 180, 270],
            mode='text',
            text=['N', 'E', 'S', 'W'],
            textfont=dict(size=16, color='gray'),
            showlegend=False
        ))
        
        # Add panel direction arrow
        fig.add_trace(go.Scatterpolar(
            r=[0, 0.8],
            theta=[azimuth, azimuth],
            mode='lines+markers',
            line=dict(color='purple', width=4),
            marker=dict(size=15, symbol='arrow', angleref='previous'),
            name='Panel Direction'
        ))
        
        fig.update_layout(
            polar=dict(
                radialaxis=dict(visible=False, range=[0, 1]),
                angularaxis=dict(direction='clockwise', rotation=90)
            ),
            showlegend=False,
            height=300,
            margin=dict(l=20, r=20, t=20, b=20)
        )
        
        st.plotly_chart(fig, use_container_width=True)
        
        # Tilt angle visualization
        st.subheader("Tilt Angle")
        
        # Create a simple side-view diagram
        x = [0, 1, 1, 0, 0]
        y = [0, 0, math.tan(math.radians(tilt)), 0, 0]
        
        fig2 = go.Figure()
        fig2.add_trace(go.Scatter(
            x=x, y=y,
            mode='lines',
            fill='toself',
            fillcolor='rgba(102, 126, 234, 0.3)',
            line=dict(color='purple', width=3),
            showlegend=False
        ))
        
        # Add angle arc
        angle_x = [0.2 * math.cos(math.radians(i)) for i in range(0, int(tilt)+1)]
        angle_y = [0.2 * math.sin(math.radians(i)) for i in range(0, int(tilt)+1)]
        
        fig2.add_trace(go.Scatter(
            x=angle_x, y=angle_y,
            mode='lines',
            line=dict(color='red', width=2, dash='dash'),
            showlegend=False
        ))
        
        # Add text
        fig2.add_annotation(
            x=0.3, y=0.1,
            text=f"{tilt:.1f}°",
            showarrow=False,
            font=dict(size=16, color='red')
        )
        
        fig2.update_layout(
            xaxis=dict(visible=False, range=[-0.1, 1.2]),
            yaxis=dict(visible=False, range=[-0.1, 0.5], scaleanchor="x"),
            height=200,
            margin=dict(l=0, r=0, t=0, b=0)
        )
        
        st.plotly_chart(fig2, use_container_width=True)

# Tab 2: Historical Analysis
with tab2:
    st.header("Historical Solar Production Analysis")
    
    if st.button("🔄 Analyze Historical Data", type="primary"):
        with st.spinner("Fetching weather data and calculating production..."):
            try:
                # Fetch weather data
                data = fetch_weather_data(latitude, longitude)
                hourly = data['hourly']
                
                # Process data
                times = hourly['time']
                temperatures = hourly['temperature_2m']
                diffuse_radiation = hourly['diffuse_radiation']
                direct_normal = hourly['direct_normal_irradiance']
                shortwave = hourly['shortwave_radiation']
                
                # Calculate production
                azimuth, tilt, _, _, _ = calculate_optimal_angles(latitude)
                temp_coefficient = calculate_temperature_coefficient(latitude)
                
                excel_data = []
                for i in range(len(times)):
                    timestamp = datetime.fromisoformat(times[i].replace('T', ' '))
                    
                    temp = temperatures[i] if temperatures[i] is not None else 20
                    diffuse = diffuse_radiation[i] if diffuse_radiation[i] is not None else 0
                    direct = direct_normal[i] if direct_normal[i] is not None else 0
                    sw = shortwave[i] if shortwave[i] is not None else 0
                    
                    power = calculate_solar_power(
                        latitude, longitude, azimuth, tilt,
                        direct, diffuse, sw, timestamp, temp, temp_coefficient
                    )
                    
                    # Scale by system size and efficiency
                    scaled_power = power * system_size * (system_efficiency / 100)
                    
                    excel_data.append({
                        'Timestamp': timestamp,
                        'Date': timestamp.date(),
                        'Hour': timestamp.hour,
                        'Temperature (°C)': temp,
                        'Direct Normal Irradiance (W/m²)': direct,
                        'Diffuse Radiation (W/m²)': diffuse,
                        'Global Horizontal Irradiance (W/m²)': sw,
                        'Power Output (W)': scaled_power,
                        'Energy (Wh)': scaled_power
                    })
                
                # Create DataFrame
                df = pd.DataFrame(excel_data)
                
                # Store in session state
                st.session_state['solar_data'] = df
                st.session_state['system_params'] = {
                    'latitude': latitude,
                    'longitude': longitude,
                    'azimuth': azimuth,
                    'tilt': tilt,
                    'system_size': system_size,
                    'panel_efficiency': panel_efficiency,
                    'system_efficiency': system_efficiency,
                    'temp_coefficient': temp_coefficient,
                    'electricity_rate': electricity_rate
                }
                
                st.markdown('<div style="background-color: #d4edda; border: 1px solid #c3e6cb; border-radius: 4px; padding: 12px; margin: 8px 0; color: #000000 !important;"><strong style="color: #000000;">✅ Analysis complete! Data is ready for visualization and download.</strong></div>', unsafe_allow_html=True)
                
            except Exception as e:
                st.error(f"❌ Error: {str(e)}")
    
    # Display results if data exists
    if 'solar_data' in st.session_state:
        df = st.session_state['solar_data']
        params = st.session_state['system_params']
        electricity_rate = params.get('electricity_rate', 0.12)  # Use stored rate or default
        
        # Show electricity rate being used
        st.info(f"💡 Using electricity rate: ${electricity_rate:.4f}/kWh")
        
        # Summary metrics
        col1, col2, col3, col4 = st.columns(4)
        
        total_energy = df['Energy (Wh)'].sum() / 1000  # Convert to kWh
        daily_avg = total_energy / 365
        
        # Create daily summary
        daily_df = df.groupby('Date')['Energy (Wh)'].sum().reset_index()
        daily_df['Energy (kWh)'] = daily_df['Energy (Wh)'] / 1000
        peak_day = daily_df.loc[daily_df['Energy (kWh)'].idxmax()]
        
        with col1:
            st.markdown(f"""
            <div style="background-color: #ffffff; padding: 1rem; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                <p style="color: #666666; margin: 0; font-size: 14px;">Annual Production</p>
                <h2 style="color: #000000; margin: 0;">{total_energy:,.0f} kWh</h2>
                <p style="color: #4caf50; margin: 0; font-size: 14px;">↑ ${total_energy * electricity_rate:,.0f}/year</p>
            </div>
            """, unsafe_allow_html=True)
        
        with col2:
            st.markdown(f"""
            <div style="background-color: #ffffff; padding: 1rem; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                <p style="color: #666666; margin: 0; font-size: 14px;">Daily Average</p>
                <h2 style="color: #000000; margin: 0;">{daily_avg:.1f} kWh</h2>
                <p style="color: #4caf50; margin: 0; font-size: 14px;">↑ ${daily_avg * electricity_rate:.2f}/day</p>
            </div>
            """, unsafe_allow_html=True)
        
        with col3:
            st.markdown(f"""
            <div style="background-color: #ffffff; padding: 1rem; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                <p style="color: #666666; margin: 0; font-size: 14px;">Peak Day</p>
                <h2 style="color: #000000; margin: 0;">{peak_day['Energy (kWh)']:.1f} kWh</h2>
                <p style="color: #4caf50; margin: 0; font-size: 14px;">↑ {peak_day['Date'].strftime('%Y-%m-%d')}</p>
            </div>
            """, unsafe_allow_html=True)
        
        with col4:
            st.markdown(f"""
            <div style="background-color: #ffffff; padding: 1rem; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                <p style="color: #666666; margin: 0; font-size: 14px;">Capacity Factor</p>
                <h2 style="color: #000000; margin: 0;">{(total_energy / (params['system_size'] * 8760)) * 100:.1f}%</h2>
                <p style="color: #4caf50; margin: 0; font-size: 14px;">↑ System utilization</p>
            </div>
            """, unsafe_allow_html=True)
        
        # Visualizations
        st.subheader("📈 Production Analysis")
        
        # Monthly production chart
        monthly_df = df.copy()
        monthly_df['Month'] = pd.to_datetime(monthly_df['Date']).dt.to_period('M')
        monthly_summary = monthly_df.groupby('Month')['Energy (Wh)'].sum().reset_index()
        monthly_summary['Energy (kWh)'] = monthly_summary['Energy (Wh)'] / 1000
        monthly_summary['Month'] = monthly_summary['Month'].astype(str)
        
        fig_monthly = px.bar(
            monthly_summary,
            x='Month',
            y='Energy (kWh)',
            title='Monthly Energy Production',
            labels={'Energy (kWh)': 'Energy (kWh)', 'Month': 'Month'},
            color='Energy (kWh)',
            color_continuous_scale='viridis'
        )
        
        fig_monthly.update_layout(
            height=400,
            showlegend=False,
            xaxis_tickangle=-45
        )
        
        st.plotly_chart(fig_monthly, use_container_width=True)
        
        # Add cost savings table section
        st.divider()
        st.subheader("💰 Cost Savings Analysis")
        
        # Calculate various financial metrics
        annual_production = total_energy  # Already calculated above
        annual_savings = annual_production * electricity_rate
        
        # Typical system costs (rough estimates)
        cost_per_kw = 1000  # $1000 per kW installed
        system_cost = params['system_size'] * cost_per_kw
        
        # Payback period
        if annual_savings > 0:
            payback_years = system_cost / annual_savings
        else:
            payback_years = float('inf')
        
        # 25-year lifetime savings
        lifetime_savings = annual_savings * 25 - system_cost
        
        # Monthly average
        monthly_savings = annual_savings / 12
        
        # Create the savings table
        savings_data = {
            'Metric': [
                'System Size',
                'Estimated System Cost',
                'Annual Energy Production',
                'Annual Savings',
                'Monthly Savings',
                'Simple Payback Period',
                '25-Year Net Savings',
                'CO₂ Avoided Annually'
            ],
            'Value': [
                f"{params['system_size']} kW",
                f'${system_cost:,.0f}',
                f'{annual_production:,.0f} kWh',
                f'${annual_savings:,.2f}',
                f'${monthly_savings:.2f}',
                f'{payback_years:.1f} years' if payback_years < 100 else 'N/A',
                f'${lifetime_savings:,.0f}',
                f'{annual_production * 0.0004:,.0f} metric tons'  # EPA average
            ],
            'Notes': [
                'Installed capacity',
                f'At ${cost_per_kw}/kW (estimate)',
                'Based on historical data',
                f'At ${electricity_rate:.3f}/kWh',
                'Average monthly savings',
                'Without incentives',
                'After system cost',
                'EPA average: 0.4 kg CO₂/kWh'
            ]
        }
        
        df_savings = pd.DataFrame(savings_data)
        
        # Style the dataframe
        st.dataframe(
            df_savings,
            use_container_width=True,
            hide_index=True,
            column_config={
                "Metric": st.column_config.TextColumn("Metric", width="medium"),
                "Value": st.column_config.TextColumn("Value", width="small"),
                "Notes": st.column_config.TextColumn("Notes", width="medium")
            }
        )
        
        # Add disclaimer
        st.info("""
        ℹ️ **Disclaimer**: These calculations are estimates based on:
        - Historical weather data for your location
        - Current electricity rate (adjustable in sidebar)
        - Simplified system cost of $1,000/kW
        - No incentives, tax credits, or rebates included
        - No panel degradation or inflation considered
        
        Actual costs and savings will vary. Consult local installers for accurate quotes.
        """)
        
        # Download section
        st.subheader("📥 Download Data")
        
        col1, col2 = st.columns(2)
        
        with col1:
            # Prepare Excel file
            output = io.BytesIO()
            
            with pd.ExcelWriter(output, engine='openpyxl') as writer:
                # Hourly data
                df.to_excel(writer, sheet_name='Hourly Data', index=False)
                
                # Daily summary
                daily_df.to_excel(writer, sheet_name='Daily Summary', index=False)
                
                # Monthly summary
                monthly_summary.to_excel(writer, sheet_name='Monthly Summary', index=False)
                
                # System parameters
                params_df = pd.DataFrame([params])
                params_df.to_excel(writer, sheet_name='System Parameters', index=False)
            
            excel_data = output.getvalue()
            
            st.download_button(
                label="📊 Download Excel Report",
                data=excel_data,
                file_name=f"solar_analysis_{params['latitude']}_{params['longitude']}_{datetime.now().strftime('%Y%m%d')}.xlsx",
                mime="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            )
        
        with col2:
            # CSV download
            csv = df.to_csv(index=False)
            st.download_button(
                label="📄 Download CSV Data",
                data=csv,
                file_name=f"solar_data_{params['latitude']}_{params['longitude']}_{datetime.now().strftime('%Y%m%d')}.csv",
                mime="text/csv"
            )

# Footer
st.markdown("---")
st.markdown("""
<div style='text-align: center; color: gray;'>
    <p>Solar Panel Optimizer v1.0 | Powered by Open-Meteo API</p>
    <p>Created with ❤️ using Streamlit</p>
</div>
""", unsafe_allow_html=True)