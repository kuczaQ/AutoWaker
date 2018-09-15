#!/bin/bash

echo "Pulling from git..."
git pull

echo "Compling..."
javac ./src/com/adam/*/*.java -cp libs/*.jar -d ./build

