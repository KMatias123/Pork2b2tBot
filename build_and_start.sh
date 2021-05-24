#!/bin/bash

echo "--------------- building the mod ---------------"
./gradlew build
echo "--------------- build done ---------------"
echo "--------------- starting ---------------"
./start.sh