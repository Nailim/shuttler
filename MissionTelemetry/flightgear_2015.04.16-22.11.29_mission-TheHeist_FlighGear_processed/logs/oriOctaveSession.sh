#! /bin/bash
wmctrl -r :ACTIVE: -N "Orientation Octave Session"
cd ../../../Code/Octave/TelemetryProcessing/ORI/
octave --silent --persist oriOctaveSession.m ../../../../MissionTelemetry/flightgear_2015.04.16-22.11.29_mission-TheHeist_FlighGear/mission/info.txt ../../../../MissionTelemetry/flightgear_2015.04.16-22.11.29_mission-TheHeist_FlighGear/logs/ori.csv
