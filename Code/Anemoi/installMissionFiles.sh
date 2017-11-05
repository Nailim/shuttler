#!/bin/bash

cd MissionFile

for file in *.myFly; do
	if [ -f $file ]; then
		adb push $file /sdcard/$file
		#adb shell "su -c cat /sdcard/$file > /storage/extSdCard/Anemoi/missionFiles/$file"
		adb shell "su -c cp /sdcard/$file /storage/extSdCard/Anemoi/missionFiles/"
		adb shell "su -c rm /sdcard/$file"
	fi
done
