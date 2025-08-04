#!/bin/bash

# Activate virtual environment
source venv/bin/activate

# Run Streamlit app
streamlit run solar_optimizer_app.py --server.port 8501 --server.headless true