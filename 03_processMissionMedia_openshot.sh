#!/bin/bash

# thread function
function thrFunction {
	# $1 command
	#echo $1
	eval "$1"
}

# 1 - fast; 2 - good quality
processMode="2"

cd MissionTelemetry

keyword="_processed_"


for dir in *; do
	# is it a direcrory?
	if [ -d "$dir" ]; then
		# is it a right directory?
		if [[ "$dir" != *_processed* ]]; then
			
			# process panorama video
			avprobe "$dir/media/panorama.mp4" 2> "/run/shm/avprobe.out"
			panoramaTime=$(cat "/run/shm/avprobe.out" | grep -o -P "(?<=Duration: ).*?(?=,)")
			rm "/run/shm/avprobe.out"
			arrIN=(${panoramaTime//:/ })
			panoramaTime=$(( (10#${arrIN[0]}*3600)+(10#${arrIN[1]}*60) ))
			panoramaTime=$(bc <<< "scale=2; ${arrIN[2]}+$panoramaTime")
			
			eval "../Code/Bash/TelemetryProcessing/telemetry2video/panorama2OpenShot/panorama2OpenShot.sh $dir $dir$keyword $panoramaTime"
			
			# process attitude video
			avprobe "$dir/media/panorama.mp4" 2> "/run/shm/avprobe_panorama.out"
			panoramaTime=$(cat "/run/shm/avprobe_panorama.out" | grep -o -P "(?<=Duration: ).*?(?=,)")
			rm "/run/shm/avprobe_panorama.out"
			arrIN=(${panoramaTime//:/ })
			panoramaTime=$(( (10#${arrIN[0]}*3600)+(10#${arrIN[1]}*60) ))
			panoramaTime=$(bc <<< "scale=2; ${arrIN[2]}+$panoramaTime")
			
			avprobe "$dir$keyword/media/attitude.mp4" 2> "/run/shm/avprobe_attitude.out"
			attitudeTime=$(cat "/run/shm/avprobe_attitude.out" | grep -o -P "(?<=Duration: ).*?(?=,)")
			rm "/run/shm/avprobe_attitude.out"
			arrIN=(${attitudeTime//:/ })
			attitudeTime=$(( (10#${arrIN[0]}*3600)+(10#${arrIN[1]}*60) ))
			attitudeTime=$(bc <<< "scale=2; ${arrIN[2]}+$attitudeTime")
			
			eval "../Code/Bash/TelemetryProcessing/telemetry2video/visualization2OpenShot/visualization2OpenShot.sh $dir $dir$keyword $panoramaTime $attitudeTime"
		fi
	fi
	#break	# debug
done
