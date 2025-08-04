#!/bin/bash

echo "Compiling Solar Optimizer..."

# Download JSON library if not present
if [ ! -f "json-20231013.jar" ]; then
    echo "Downloading JSON library..."
    curl -o json-20231013.jar https://repo1.maven.org/maven2/org/json/json/20231013/json-20231013.jar
fi

# Compile
javac -cp ".:json-20231013.jar" SimpleSolarOptimizer.java

if [ $? -eq 0 ]; then
    echo "Running Solar Optimizer..."
    echo ""
    java -cp ".:json-20231013.jar" SimpleSolarOptimizer
else
    echo "Compilation failed!"
fi