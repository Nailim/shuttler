#!/bin/bash

cd MissionTelemetry

for adbDir in `adb shell "ls /mnt/extSdCard/Anemoi/missionTelemetry/"`; do
	if [ ! -d "${adbDir/$'\r'/}" ]; then
		echo "$adbDir"
		mkdir ${adbDir/$'\r'/}
		cd ${adbDir/$'\r'/}
		adb pull /mnt/extSdCard/Anemoi/missionTelemetry/${adbDir/$'\r'/}/	# ${adbDir/$'\r'/} - removes '\r' from string	
		cd ..	
	fi
done
