#!/bin/bash

cd MissionTelemetry

#rm -R *_processed # !!! debug

keyword="_processed"

for dir in *; do
	if [ -d "$dir" ]; then
		
		if [[ "$dir" != *_processed* ]]; then
			if [ ! -d "$dir$keyword" ]; then
				echo "$dir"
				
				mkdir $dir$keyword
				cd $dir$keyword
				
				# processed log files
				mkdir logs
				
				# prepare visualitation of GPS data in GoogleEarth
				python ../../Code/Python/TelemetryProcessing/GPS/gps2kml.py ../$dir
				python ../../Code/Python/TelemetryProcessing/GPS/gps2kmlTour.py ../$dir
				
				# prepare visualization and octave session with GPS data
				echo "#! /bin/bash" >> ../$dir$keyword/logs/gpsOctaveSession.sh
				echo "wmctrl -r :ACTIVE: -N \"GPS Octave Session\"" >> ../$dir$keyword/logs/gpsOctaveSession.sh
				echo "cd ../../../Code/Octave/TelemetryProcessing/GPS/" >> ../$dir$keyword/logs/gpsOctaveSession.sh
				echo "octave --silent --persist gpsOctaveSession.m ../../../../MissionTelemetry/$dir/mission/info.txt ../../../../MissionTelemetry/$dir/logs/gps.csv" >> ../$dir$keyword/logs/gpsOctaveSession.sh
				chmod +x ../$dir$keyword/logs/gpsOctaveSession.sh
				
				# prepare visualization and octave session with orientation data
				echo "#! /bin/bash" >> ../$dir$keyword/logs/oriOctaveSession.sh
				echo "wmctrl -r :ACTIVE: -N \"Orientation Octave Session\"" >> ../$dir$keyword/logs/oriOctaveSession.sh
				echo "cd ../../../Code/Octave/TelemetryProcessing/ORI/" >> ../$dir$keyword/logs/oriOctaveSession.sh
				echo "octave --silent --persist oriOctaveSession.m ../../../../MissionTelemetry/$dir/mission/info.txt ../../../../MissionTelemetry/$dir/logs/ori.csv" >> ../$dir$keyword/logs/oriOctaveSession.sh
				chmod +x ../$dir$keyword/logs/oriOctaveSession.sh
				
				# data is from android - process aditional files if they are present
				autopilotType=$(cat "../"$dir"/mission/info.txt" | grep autopilot_type | awk -F " " '{print $2;}' )
				if [ "$autopilotType" = "0" ]; then
				
					# prepare visualization and octave session with raw accelerometer data
					if [ -f "../$dir/logs/rawACC.csv" ]; then					
						echo "#! /bin/bash" >> ../$dir$keyword/logs/accOctaveSession.sh
						echo "wmctrl -r :ACTIVE: -N \"Accelerometer Octave Session\"" >> ../$dir$keyword/logs/accOctaveSession.sh
						echo "cd ../../../Code/Octave/TelemetryProcessing/ACC/" >> ../$dir$keyword/logs/accOctaveSession.sh
						echo "octave --silent --persist accOctaveSession.m ../../../../MissionTelemetry/$dir/mission/info.txt ../../../../MissionTelemetry/$dir/logs/rawACC.csv" >> ../$dir$keyword/logs/accOctaveSession.sh
						chmod +x ../$dir$keyword/logs/accOctaveSession.sh
					fi
					
					# prepare visualization and octave session with raw magnetometer data
					if [ -f "../$dir/logs/rawMAG.csv" ]; then
						echo "#! /bin/bash" >> ../$dir$keyword/logs/magOctaveSession.sh
						echo "wmctrl -r :ACTIVE: -N \"Magnetometer Octave Session\"" >> ../$dir$keyword/logs/magOctaveSession.sh
						echo "cd ../../../Code/Octave/TelemetryProcessing/MAG/" >> ../$dir$keyword/logs/magOctaveSession.sh
						echo "octave --silent --persist magOctaveSession.m ../../../../MissionTelemetry/$dir/mission/info.txt ../../../../MissionTelemetry/$dir/logs/rawMAG.csv" >> ../$dir$keyword/logs/magOctaveSession.sh
						chmod +x ../$dir$keyword/logs/magOctaveSession.sh
					fi
					
					# prepare visualization and octave session with raw gyroscope data
					if [ -f "../$dir/logs/rawGYR.csv" ]; then
						echo "#! /bin/bash" >> ../$dir$keyword/logs/gyrOctaveSession.sh
						echo "wmctrl -r :ACTIVE: -N \"Gyroscope Octave Session\"" >> ../$dir$keyword/logs/gyrOctaveSession.sh
						echo "cd ../../../Code/Octave/TelemetryProcessing/GYR/" >> ../$dir$keyword/logs/gyrOctaveSession.sh
						echo "octave --silent --persist gyrOctaveSession.m ../../../../MissionTelemetry/$dir/mission/info.txt ../../../../MissionTelemetry/$dir/logs/rawGYR.csv" >> ../$dir$keyword/logs/gyrOctaveSession.sh
						chmod +x ../$dir$keyword/logs/gyrOctaveSession.sh
					fi
					
					# prepare visualization and octave session with computed acceleration data
					if [ -f "../$dir/logs/comACL.csv" ]; then
						echo "#! /bin/bash" >> ../$dir$keyword/logs/aclOctaveSession.sh
						echo "wmctrl -r :ACTIVE: -N \"Acceleration Octave Session\"" >> ../$dir$keyword/logs/aclOctaveSession.sh
						echo "cd ../../../Code/Octave/TelemetryProcessing/ACL/" >> ../$dir$keyword/logs/aclOctaveSession.sh
						echo "octave --silent --persist aclOctaveSession.m ../../../../MissionTelemetry/$dir/mission/info.txt ../../../../MissionTelemetry/$dir/logs/comACL.csv" >> ../$dir$keyword/logs/aclOctaveSession.sh
						chmod +x ../$dir$keyword/logs/aclOctaveSession.sh
					fi
					
					# prepare visualization and octave session with computed gravity data
					if [ -f "../$dir/logs/comGRA.csv" ]; then
						echo "#! /bin/bash" >> ../$dir$keyword/logs/graOctaveSession.sh
						echo "wmctrl -r :ACTIVE: -N \"Gravity Octave Session\"" >> ../$dir$keyword/logs/graOctaveSession.sh
						echo "cd ../../../Code/Octave/TelemetryProcessing/GRA/" >> ../$dir$keyword/logs/graOctaveSession.sh
						echo "octave --silent --persist graOctaveSession.m ../../../../MissionTelemetry/$dir/mission/info.txt ../../../../MissionTelemetry/$dir/logs/comGRA.csv" >> ../$dir$keyword/logs/graOctaveSession.sh
						chmod +x ../$dir$keyword/logs/graOctaveSession.sh
					fi
					
					# prepare visualization and octave session with computed rotation data
					if [ -f "../$dir/logs/comROT.csv" ]; then
						echo "#! /bin/bash" >> ../$dir$keyword/logs/rotOctaveSession.sh
						echo "wmctrl -r :ACTIVE: -N \"Rotation Octave Session\"" >> ../$dir$keyword/logs/rotOctaveSession.sh
						echo "cd ../../../Code/Octave/TelemetryProcessing/ROT/" >> ../$dir$keyword/logs/rotOctaveSession.sh
						echo "octave --silent --persist rotOctaveSession.m ../../../../MissionTelemetry/$dir/mission/info.txt ../../../../MissionTelemetry/$dir/logs/comROT.csv" >> ../$dir$keyword/logs/rotOctaveSession.sh
						chmod +x ../$dir$keyword/logs/rotOctaveSession.sh
					fi
					
					# prepare visualization and octave session with roll pid data
					if [ -f "../$dir/logs/pidROLL.csv" ]; then
						echo "#! /bin/bash" >> ../$dir$keyword/logs/pidRollOctaveSession.sh
						echo "wmctrl -r :ACTIVE: -N \"PID Roll Octave Session\"" >> ../$dir$keyword/logs/pidRollOctaveSession.sh
						echo "cd ../../../Code/Octave/TelemetryProcessing/PID/" >> ../$dir$keyword/logs/pidRollOctaveSession.sh
						echo "octave --silent --persist pidOctaveSession.m ../../../../MissionTelemetry/$dir/mission/info.txt ../../../../MissionTelemetry/$dir/logs/pidROLL.csv" >> ../$dir$keyword/logs/pidRollOctaveSession.sh
						chmod +x ../$dir$keyword/logs/pidRollOctaveSession.sh
					fi
					
					# prepare visualization and octave session with pitch pid data
					if [ -f "../$dir/logs/pidPITCH.csv" ]; then
						echo "#! /bin/bash" >> ../$dir$keyword/logs/pidPitchOctaveSession.sh
						echo "wmctrl -r :ACTIVE: -N \"PID Pitch Octave Session\"" >> ../$dir$keyword/logs/pidPitchOctaveSession.sh
						echo "cd ../../../Code/Octave/TelemetryProcessing/PID/" >> ../$dir$keyword/logs/pidPitchOctaveSession.sh
						echo "octave --silent --persist pidOctaveSession.m ../../../../MissionTelemetry/$dir/mission/info.txt ../../../../MissionTelemetry/$dir/logs/pidPITCH.csv" >> ../$dir$keyword/logs/pidPitchOctaveSession.sh
						chmod +x ../$dir$keyword/logs/pidPitchOctaveSession.sh
					fi
					
					# prepare visualization and octave session with yaw pid data
					if [ -f "../$dir/logs/pidYAW.csv" ]; then
						echo "#! /bin/bash" >> ../$dir$keyword/logs/pidYawOctaveSession.sh
						echo "wmctrl -r :ACTIVE: -N \"PID Yaw Octave Session\"" >> ../$dir$keyword/logs/pidYawOctaveSession.sh
						echo "cd ../../../Code/Octave/TelemetryProcessing/PID/" >> ../$dir$keyword/logs/pidYawOctaveSession.sh
						echo "octave --silent --persist pidOctaveSession.m ../../../../MissionTelemetry/$dir/mission/info.txt ../../../../MissionTelemetry/$dir/logs/pidYAW.csv" >> ../$dir$keyword/logs/pidYawOctaveSession.sh
						chmod +x ../$dir$keyword/logs/pidYawOctaveSession.sh
					fi
				fi
				
				# processed media files
				mkdir media
				# process in a separate script
				
				# processed mission report
				mkdir mission
				
				cd ..
			fi
		fi
		
	fi
done


