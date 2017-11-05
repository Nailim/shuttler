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
			
			# does the session have a panorama video?
			if [ -f "$dir/media/panorama.mp4" ]; then
				# is it already processed?
				if [ ! -f "$dir$keyword/media/panorama.mp4" ]; then
					# create directory structure
					if [ ! -d "$dir$keyword" ]; then
						mkdir "$dir$keyword"
					fi
					if [ ! -d "$dir$keyword/media/" ]; then
						mkdir "$dir$keyword/media"
					fi
					if [ ! -d "$dir$keyword/media/work/" ]; then
						mkdir "$dir$keyword/media/work"
					fi
					if [ ! -d "$dir$keyword/media/work/panorama/" ]; then
						mkdir "$dir$keyword/media/work/panorama"
					fi
				
					# get framerate
					avprobe "$dir/media/panorama.mp4" 2> "/run/shm/avprobe.out"
					fps=$(cat "/run/shm/avprobe.out" | grep -o '[0-9]*.\{1,3\}\sfps' | awk -F ' ' '{print $1}')
					rm "/run/shm/avprobe.out"
					
					# extract images with correct framerate
					avconv -i "$dir/media/panorama.mp4" -r $fps -f image2 -qscale 1 "$dir$keyword/media/work/panorama/img_%07d.jpg"
					
					# unwrap and interpolate images
					
					# fast with no interpolation - threaded (4 threads - make prettier)
					if [ "$processMode" -eq "1" ]; then
						cd $dir$keyword
						cd "media"
						cd "work"
						cd "panorama"
						# 1280x720 - normal
						#allCommandStrings="python /media/Data/Project/Shuttler/Code/Python/ImageProcessing/RadialTransform/radialTransform.py -p lp2c -pp none -c 655 362 -r 180 -R 352 -s 1.819 -o 3.1415"
						#allCommandStrings="python /media/Data/Project/Shuttler/Code/Python/ImageProcessing/RadialTransform/radialTransform.py -p lp2c -pp none -c 655 362 -r 180 -R 352 -s 1.819 -o 1.57075"
						# 1280x720 - opencl
						allCommandStrings="python /media/Data/Project/Shuttler/Code/Python/ImageProcessing/RadialTransformOpenCL/radialTransform.py -p lp2c -pp none -c 655 362 -r 180 -R 352 -s 1.819 -o 3.1415"
						#allCommandStrings="python /media/Data/Project/Shuttler/Code/Python/ImageProcessing/RadialTransformOpenCL/radialTransform.py -p lp2c -pp none -c 655 362 -r 180 -R 352 -s 1.819 -o 1.57075"
						# 640x480 - normal
						#allCommandStrings="python /media/Data/Project/Shuttler/Code/Python/ImageProcessing/RadialTransform/radialTransform.py -p lp2c -pp none -c 323 240 -r 68 -R 124 -s 5.165"
						# 640x480 - opencl
						#allCommandStrings="python /media/Data/Project/Shuttler/Code/Python/ImageProcessing/RadialTransformOpenCL/radialTransform.py -p lp2c -pp none -c 323 240 -r 68 -R 124 -s 5.165"
												
						fileNames_1=""
						fileNames_2=""
						fileNames_3=""
						fileNames_4=""	
	
						fileCount=0
						allFileNames=""
						
						for file in *.jpg; do
							#echo $fileCount
							fileCount=$(($fileCount + 1))
							if [ $fileCount -eq 1 ]; then
								if [[ -f $file ]]; then
									allFileNames_1="$allFileNames_1 $(basename $file)"
								fi
								#fileCount=0
							fi
							if [ $fileCount -eq 2 ]; then
								if [[ -f $file ]]; then
									allFileNames_2="$allFileNames_2 $(basename $file)"
								fi
								#fileCount=0
							fi
							if [ $fileCount -eq 3 ]; then
								if [[ -f $file ]]; then
									allFileNames_3="$allFileNames_3 $(basename $file)"
								fi
								#fileCount=0
							fi
							if [ $fileCount -eq 4 ]; then
								if [[ -f $file ]]; then
									allFileNames_4="$allFileNames_4 $(basename $file)"
								fi
								fileCount=0
							fi
						done

						#eval "$allCommandStrings $allFileNames_1 & $allCommandStrings $allFileNames_2 & $allCommandStrings $allFileNames_3 & $allCommandStrings $allFileNames_4"
						thrFunction "$allCommandStrings $allFileNames_1" &
						thrFunction "$allCommandStrings $allFileNames_2" &
						thrFunction "$allCommandStrings $allFileNames_3" &
						thrFunction "$allCommandStrings $allFileNames_4" &
						
						cd ".."
						cd ".."
						cd ".."
						cd ".."
					fi
					# slow with interpolation
					if [ "$processMode" -eq "2" ]; then
						cd $dir$keyword
						cd "media"
						cd "work"
						cd "panorama"
						# 1280x720 - normal
						#allCommandStrings="python /media/Data/Project/Shuttler/Code/Python/ImageProcessing/RadialTransform/radialTransform.py -p lp2c -pp bspl -c 655 362 -r 180 -R 352 -s 1.819 -o 3.1415"
						#allCommandStrings="python /media/Data/Project/Shuttler/Code/Python/ImageProcessing/RadialTransform/radialTransform.py -p lp2c -pp bspl -c 655 362 -r 180 -R 352 -s 1.819 -o 1.57075"
						# 1280x720 - opencl
						allCommandStrings="python /media/Data/Project/Shuttler/Code/Python/ImageProcessing/RadialTransformOpenCL/radialTransform.py -p lp2c -pp bspl -c 655 362 -r 180 -R 352 -s 1.819 -o 3.1415"
						#allCommandStrings="python /media/Data/Project/Shuttler/Code/Python/ImageProcessing/RadialTransformOpenCL/radialTransform.py -p lp2c -pp bspl -c 655 362 -r 180 -R 352 -s 1.819 -o 1.57075"
						# 640x480 - normal
						#allCommandStrings="python /media/Data/Project/Shuttler/Code/Python/ImageProcessing/RadialTransform/radialTransform.py -p lp2c -pp bspl -c 323 240 -r 68 -R 124 -s 5.165 -o 3.1415"
						# 640x480 - opencl
						#allCommandStrings="python /media/Data/Project/Shuttler/Code/Python/ImageProcessing/RadialTransformOpenCL/radialTransform.py -p lp2c -pp bspl -c 323 240 -r 68 -R 124 -s 5.165 -o 3.1415"
						
						allFileNames=""
						for file in *.jpg; do
							if [[ -f $file ]]; then
								allFileNames="$allFileNames $(basename $file)"
							fi
						done
	
						eval "$allCommandStrings $allFileNames"
						
						cd ".."
						cd ".."
						cd ".."
						cd ".."
					fi
					
					# we don't need raw video images any more
					rm $dir$keyword/media/work/panorama/img_*
					
					# rename and expand image to even size for h264 (1280x174)
					for file in $dir$keyword/media/work/panorama/out*.jpg*; do
						if [[ -f $file ]]; then
							#echo $(basename $file)
							fullName=$(basename $file)
							#echo ${fullName:8:11}
							fileName=${fullName:8:11}
							commString="convert $dir$keyword/media/work/panorama/$fullName -gravity center -background black -extent 1280x174 $dir$keyword/media/work/panorama/$fileName"
							#echo "$commString"
							eval $commString
						fi
					done
					
					# we don't need raw processed images any more
					rm $dir$keyword/media/work/panorama/out_*
					
					# render images back to video
					avconv -r $fps -i "$dir$keyword/media/work/panorama/%07d.jpg" -qscale 1 -b 1300k -vcodec libx264 "$dir$keyword/media/panorama_silent.mp4"

					# we don't need any processed images any more
					rm $dir$keyword/media/work/panorama/*.jpg
					
					# we don't need the directory any more
					#rmdir $dir$keyword/media/work/panorama 
					rm -R $dir$keyword/media/work/panorama
					
					# merge silent video track with the original with sound in the same container (for some reason that is the only way to keep the sound in sync)
					#avconv -y -i $dir$keyword/media/panorama_silent.mp4 -i $dir/media/panorama.mp4 -map 0:0 -map 1 -vcodec copy -acodec copy $dir$keyword/media/panorama_tmp.mp4
					#avconv -y -i $dir$keyword/media/panorama_tmp.mp4 -map 0:0 -map 0:2 -acodec copy -vcodec copy $dir$keyword/media/panorama.mp4
					#rm $dir$keyword/media/panorama_tmp.mp4
					avconv -i $dir/media/panorama.mp4 -i $dir$keyword/media/panorama_silent.mp4 -vcodec copy -acodec copy -map 0:1 -map 1:0 $dir$keyword/media/panorama.mp4
					
				fi
			fi
				
			# visualize data telemetry
			
			# attitude visualization
			if [ ! -f "$dir$keyword/media/attitude.mp4" ]; then
				# do we need a directory?
				if [ ! -d "$dir$keyword" ]; then
					mkdir "$dir$keyword"
				fi
				if [ ! -d "$dir$keyword/media/" ]; then
					mkdir "$dir$keyword/media"
				fi
				if [ ! -d "$dir$keyword/media/work/" ]; then
					mkdir "$dir$keyword/media/work"
				fi
				if [ ! -d "$dir$keyword/logs/" ]; then
					mkdir "$dir$keyword/logs"
				fi
			
				# get data files
				octave ../Code/Octave/TelemetryProcessing/ORI/oriInterpolateData.m $dir/mission/info.txt $dir/logs/ori.csv $dir$keyword/logs/
				octave ../Code/Octave/TelemetryProcessing/GPS/gpsInterpolateData.m $dir/mission/info.txt $dir/logs/gps.csv $dir$keyword/logs/
				# "fix" data files 
				sed -i '1,5d' $dir$keyword/logs/interpolated_pitch_30fps.txt
				sed -i '$d' $dir$keyword/logs/interpolated_pitch_30fps.txt
				sed -i '$d' $dir$keyword/logs/interpolated_pitch_30fps.txt
				sed -i '1,5d' $dir$keyword/logs/interpolated_roll_30fps.txt
				sed -i '$d' $dir$keyword/logs/interpolated_roll_30fps.txt
				sed -i '$d' $dir$keyword/logs/interpolated_roll_30fps.txt
				sed -i '1,5d' $dir$keyword/logs/interpolated_yaw_30fps.txt
				sed -i '$d' $dir$keyword/logs/interpolated_yaw_30fps.txt
				sed -i '$d' $dir$keyword/logs/interpolated_yaw_30fps.txt
				sed -i '1,5d' $dir$keyword/logs/interpolated_altitude_30fps.txt
				sed -i '$d' $dir$keyword/logs/interpolated_altitude_30fps.txt
				sed -i '$d' $dir$keyword/logs/interpolated_altitude_30fps.txt
				sed -i '1,5d' $dir$keyword/logs/interpolated_altitude_speed_30fps.txt
				sed -i '$d' $dir$keyword/logs/interpolated_altitude_speed_30fps.txt
				sed -i '$d' $dir$keyword/logs/interpolated_altitude_speed_30fps.txt
				sed -i '1,5d' $dir$keyword/logs/interpolated_speed_30fps.txt
				sed -i '$d' $dir$keyword/logs/interpolated_speed_30fps.txt
				sed -i '$d' $dir$keyword/logs/interpolated_speed_30fps.txt
			
				sed -i 's/NA/0.0/g' $dir$keyword/logs/interpolated_pitch_30fps.txt
				sed -i 's/NA/0.0/g' $dir$keyword/logs/interpolated_roll_30fps.txt
				sed -i 's/NA/0.0/g' $dir$keyword/logs/interpolated_yaw_30fps.txt
				sed -i 's/NA/0.0/g' $dir$keyword/logs/interpolated_altitude_30fps.txt
				sed -i 's/NA/0.0/g' $dir$keyword/logs/interpolated_altitude_speed_30fps.txt
				sed -i 's/NA/0.0/g' $dir$keyword/logs/interpolated_speed_30fps.txt
				
				sed -i 's/NaN/0.0/g' $dir$keyword/logs/interpolated_altitude_speed_30fps.txt
				
				# render telemetry to video
				fullPath=$(pwd)
				#python ../Code/Python/TelemetryProcessing/telemetry2video/Attitude/attitude2video.py "$fullPath/$dir$keyword"
				# treaded, wait for /run/shm/attitude2video.lock to clear
				#thrFunction "python ../Code/Python/TelemetryProcessing/telemetry2video/Attitude/attitude2video.py $fullPath/$dir$keyword" &
				thrFunction "python ../Code/Python/TelemetryProcessing/telemetry2video/Attitude/attitude2video_opencv.py $fullPath/$dir$keyword" &
				# !!! re enable upper line
			fi
		
			# acceleration visualization
			if [ ! -f "$dir$keyword/media/acceleration.mp4" ]; then
				# do we need a directory?
				if [ ! -d "$dir$keyword" ]; then
					mkdir "$dir$keyword"
				fi
				if [ ! -d "$dir$keyword/media/" ]; then
					mkdir "$dir$keyword/media"
				fi
				if [ ! -d "$dir$keyword/media/work/" ]; then
					mkdir "$dir$keyword/media/work"
				fi
				if [ ! -d "$dir$keyword/logs/" ]; then
					mkdir "$dir$keyword/logs"
				fi
				# get acceleration data files
				octave ../Code/Octave/TelemetryProcessing/ACL/aclInterpolateData.m $dir/mission/info.txt $dir/logs/comACL.csv $dir$keyword/logs/
				# "fix" data files 
				sed -i '1,5d' $dir$keyword/logs/interpolated_aclX_30fps.txt
				sed -i '$d' $dir$keyword/logs/interpolated_aclX_30fps.txt
				sed -i '$d' $dir$keyword/logs/interpolated_aclX_30fps.txt
				sed -i '1,5d' $dir$keyword/logs/interpolated_aclY_30fps.txt
				sed -i '$d' $dir$keyword/logs/interpolated_aclY_30fps.txt
				sed -i '$d' $dir$keyword/logs/interpolated_aclY_30fps.txt
				sed -i '1,5d' $dir$keyword/logs/interpolated_aclZ_30fps.txt
				sed -i '$d' $dir$keyword/logs/interpolated_aclZ_30fps.txt
				sed -i '$d' $dir$keyword/logs/interpolated_aclZ_30fps.txt
				
				sed -i 's/NA/0.0/g' $dir$keyword/logs/interpolated_aclX_30fps.txt
				sed -i 's/NA/0.0/g' $dir$keyword/logs/interpolated_aclY_30fps.txt
				sed -i 's/NA/0.0/g' $dir$keyword/logs/interpolated_aclZ_30fps.txt
				
				# get gravity data files
				octave ../Code/Octave/TelemetryProcessing/GRA/graInterpolateData.m $dir/mission/info.txt $dir/logs/comGRA.csv $dir$keyword/logs/
				# "fix" data files 
				sed -i '1,5d' $dir$keyword/logs/interpolated_graX_30fps.txt
				sed -i '$d' $dir$keyword/logs/interpolated_graX_30fps.txt
				sed -i '$d' $dir$keyword/logs/interpolated_graX_30fps.txt
				sed -i '1,5d' $dir$keyword/logs/interpolated_graY_30fps.txt
				sed -i '$d' $dir$keyword/logs/interpolated_graY_30fps.txt
				sed -i '$d' $dir$keyword/logs/interpolated_graY_30fps.txt
				sed -i '1,5d' $dir$keyword/logs/interpolated_graZ_30fps.txt
				sed -i '$d' $dir$keyword/logs/interpolated_graZ_30fps.txt
				sed -i '$d' $dir$keyword/logs/interpolated_graZ_30fps.txt
				
				sed -i 's/NA/0.0/g' $dir$keyword/logs/interpolated_graX_30fps.txt
				sed -i 's/NA/0.0/g' $dir$keyword/logs/interpolated_graY_30fps.txt
				sed -i 's/NA/0.0/g' $dir$keyword/logs/interpolated_graZ_30fps.txt
			
				# render telemetry to video
				fullPath=$(pwd)
				#python ../Code/Python/TelemetryProcessing/telemetry2video/Acceleration/acceleration2video.py "$fullPath/$dir$keyword"
				# treaded, wait for /run/shm/acceleration2video.lock to clear
				#thrFunction "python ../Code/Python/TelemetryProcessing/telemetry2video/Acceleration/acceleration2video.py $fullPath/$dir$keyword" &
				thrFunction "python ../Code/Python/TelemetryProcessing/telemetry2video/Acceleration/acceleration2video_opencv.py $fullPath/$dir$keyword" &
			fi
			
			# wait for threaded code finishes
			lockLock=1
			while [ $lockLock -gt 0 ]; do
				sleep 5
#				if [ ! -f /run/shm/acceleration2video.lock ] && [ ! -f /run/shm/attitude2video.lock ]; then
				if [ ! -f /run/shm/acceleration2video_opencv.lock ] && [ ! -f /run/shm/attitude2video_opencv.lock ]; then				
					lockLock=0
				fi				
			done
			
			# remove the evidence
			if [ -d "$dir$keyword/media/work" ]; then
				rmdir $dir$keyword/media/work
			fi
						
		fi			
	fi
	#break	# debug
done
