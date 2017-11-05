#! /bin/bash
wmctrl -r :ACTIVE: -N "GPS Octave Session"
cd ../../../Code/Octave/TelemetryProcessing/GPS/
octave --silent --persist gpsOctaveSession.m ../../../../MissionTelemetry/flightgear_2015.04.16-22.11.29_mission-TheHeist_FlighGear/mission/info.txt ../../../../MissionTelemetry/flightgear_2015.04.16-22.11.29_mission-TheHeist_FlighGear/logs/gps.csv
