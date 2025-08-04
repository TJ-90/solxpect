#!/bin/bash

# Activate virtual environment
source venv/bin/activate

# Run the solar optimizer with example coordinates
# You can change these values to your location
printf "37.7749\n-122.4194\n" | python solar_optimizer.py