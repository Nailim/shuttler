import sys
import os

#import commands		# for command line

import argparse

global inputParser	# just a reminder, it's used as a global variable
global inputArgs	# just a reminder, it's used as a global variable

def parseInput() :
	
	global inputParser
	global inputArgs
	
	inputParser = argparse.ArgumentParser(description='Render telemetry (attitude) to video from Anemoi autopilot')
	
	inputParser.add_argument('sessionFolder', nargs=1, help='folder containing processed autopilot autopilot (30 FPS)')
	
	inputArgs = inputParser.parse_args()
	
def processInput() :
	
	print inputArgs.sessionFolder
	
	scriptDir = os.path.dirname(os.path.abspath(__file__))
	
	os.system("touch /run/shm/attitude2video.lock")
	
	data_yaw=[]
	data_pitch=[]
	data_roll=[]
	
	data_speed=[]
	data_altitude=[]
	
	sourceFile_yaw = open(inputArgs.sessionFolder[0]+"/logs/interpolated_yaw_30fps.txt", 'r')
	for line in sourceFile_yaw :
		data_yaw.append(float(line))
	sourceFile_yaw.close()
	
	sourceFile_pitch = open(inputArgs.sessionFolder[0]+"/logs/interpolated_pitch_30fps.txt", 'r')
	for line in sourceFile_pitch :
		data_pitch.append(float(line))
	sourceFile_pitch.close()
	
	sourceFile_roll = open(inputArgs.sessionFolder[0]+"/logs/interpolated_roll_30fps.txt", 'r')
	for line in sourceFile_roll :
		data_roll.append(float(line))
	sourceFile_roll.close()
	
	sourceFile_speed = open(inputArgs.sessionFolder[0]+"/logs/interpolated_speed_30fps.txt", 'r')
	for line in sourceFile_speed :
		data_speed.append(float(line)*3600/1000)
	sourceFile_speed.close()
	
	sourceFile_altitude = open(inputArgs.sessionFolder[0]+"/logs/interpolated_altitude_30fps.txt", 'r')
	for line in sourceFile_altitude :
		data_altitude.append(float(line))
	sourceFile_altitude.close()
	
	# create working directory in shared memmory
	if not os.path.exists("/dev/shm/attitude2video") :
		os.makedirs("/dev/shm/attitude2video")
		
		# prepare image templates
		os.system("cp "+scriptDir+"/resources/speed.png /dev/shm/attitude2video/speed.png")
		os.system("cp "+scriptDir+"/resources/center.png /dev/shm/attitude2video/center.png")
		os.system("cp "+scriptDir+"/resources/horizon.png /dev/shm/attitude2video/horizon.png")
		os.system("cp "+scriptDir+"/resources/terrain.png /dev/shm/attitude2video/terrain.png")
		os.system("cp "+scriptDir+"/resources/compass.png /dev/shm/attitude2video/compass.png")
		os.system("cp "+scriptDir+"/resources/altitude.png /dev/shm/attitude2video/altitude.png")
		os.system("cp "+scriptDir+"/resources/background.png /dev/shm/attitude2video/background.png")
		os.system("cp "+scriptDir+"/resources/background_small.png /dev/shm/attitude2video/background_small.png")
	
	# create working directory
	if not os.path.exists(inputArgs.sessionFolder[0]+"/media/work") :
		os.makedirs(inputArgs.sessionFolder[0]+"/media/work")
	if not os.path.exists(inputArgs.sessionFolder[0]+"/media/work/attitude") :
		os.makedirs(inputArgs.sessionFolder[0]+"/media/work/attitude")
	
	# magic
	for x in range(0, len(data_speed)):
		progressbar(((x*1.0+1)/len(data_speed)), "Rendering:", "(" + str(x+1) + "/" + str(len(data_speed)) + ") " + "attitude_%07d.jpg" % (x+1), 20)
		# new ori sensor
		os.system("convert -background black /dev/shm/attitude2video/horizon.png -rotate " + str(data_roll[x]*-1) + " -transparent black /dev/shm/attitude2video/horizon_rotated.png")
		os.system("convert -background \"transparent\" -alpha on /dev/shm/attitude2video/center.png -rotate " + str(data_roll[x]*-1) + " /dev/shm/attitude2video/center_rotated.png")
		
		# new ori sensor
		if data_pitch[x] >= 0:
			os.system("composite -gravity center -page 640x360 -geometry +0+" + str(data_pitch[x]*3.0) + " /dev/shm/attitude2video/horizon_rotated.png /dev/shm/attitude2video/background.png /dev/shm/attitude2video/composed.png")
		else:
			os.system("composite -gravity center -page 640x360 -geometry +0-" + str(data_pitch[x]*3.0) + " /dev/shm/attitude2video/horizon_rotated.png /dev/shm/attitude2video/background.png /dev/shm/attitude2video/composed.png")
		
		if data_yaw[x] <= 180:
			os.system("composite -background \"transparent\" -alpha on -gravity north -geometry +"+str(3.9*data_yaw[x])+"-0 /dev/shm/attitude2video/compass.png /dev/shm/attitude2video/composed.png /dev/shm/attitude2video/composed.png")
		else:
			os.system("composite -background \"transparent\" -alpha on -gravity north -geometry +"+str(3.9*(360-data_yaw[x]))+"-0 /dev/shm/attitude2video/compass.png /dev/shm/attitude2video/composed.png /dev/shm/attitude2video/composed.png")
		
		if data_speed[x] < 0:
			os.system("composite -gravity center -geometry +0-210 /dev/shm/attitude2video/speed.png /dev/shm/attitude2video/background_small.png /dev/shm/attitude2video/speed_small.png")
		elif data_speed[x] >= 100:
			os.system("composite -gravity center -geometry +0+210 /dev/shm/attitude2video/speed.png /dev/shm/attitude2video/background_small.png /dev/shm/attitude2video/speed_small.png")
		else :
			os.system("composite -gravity center -geometry +0-"+str(200-(data_speed[x]*4))+" /dev/shm/attitude2video/speed.png /dev/shm/attitude2video/background_small.png /dev/shm/attitude2video/speed_small.png")
		
		if data_altitude[x] < 0:
			os.system("composite -gravity center -geometry +0-2010 /dev/shm/attitude2video/altitude.png /dev/shm/attitude2video/background_small.png /dev/shm/attitude2video/altitude_small.png")
		elif data_altitude[x] >= 1000:
			os.system("composite -gravity center -geometry +0+2010 /dev/shm/attitude2video/altitude.png /dev/shm/attitude2video/background_small.png /dev/shm/attitude2video/altitude_small.png")
		else :
			os.system("composite -gravity center -geometry +0-"+str(2000-(data_altitude[x]*4))+" /dev/shm/attitude2video/altitude.png /dev/shm/attitude2video/background_small.png /dev/shm/attitude2video/altitude_small.png")
		
		#os.system("composite -gravity center /dev/shm/attitude2video/terrain.png /dev/shm/attitude2video/composed.png /dev/shm/attitude2video/composed.png")
		#os.system("composite -gravity center /dev/shm/attitude2video/center_rotated.png /dev/shm/attitude2video/composed.png /dev/shm/attitude2video/composed.png")
		#os.system("composite -gravity center -geometry -190-0 /dev/shm/attitude2video/speed_small.png /dev/shm/attitude2video/composed.png /dev/shm/attitude2video/composed.png")
		#os.system("composite -gravity center -geometry +190-0 /dev/shm/attitude2video/altitude_small.png /dev/shm/attitude2video/composed.png /dev/shm/attitude2video/composed.png")		
		os.system("convert -size 640x360 xc:black /dev/shm/attitude2video/composed.png -composite /dev/shm/attitude2video/terrain.png -composite /dev/shm/attitude2video/center_rotated.png -gravity center -composite /dev/shm/attitude2video/speed_small.png -gravity center -geometry -190-0 -composite /dev/shm/attitude2video/altitude_small.png -gravity center -geometry +190-0 -composite /dev/shm/attitude2video/composed.jpg")
		
		os.system("cp /dev/shm/attitude2video/composed.jpg "+inputArgs.sessionFolder[0]+"/media/work/attitude/attitude_"+"%07d.jpg" % (x+1,))
	
	os.system("avconv -r 30 -i "+inputArgs.sessionFolder[0]+"/media/work/attitude/attitude_"+"%07d.jpg -qscale 1 -b 1300k -vcodec libx264 "+inputArgs.sessionFolder[0]+"/media/attitude.mp4")
	
	# remove working directory with temporary files
	fileList = os.listdir(inputArgs.sessionFolder[0]+"/media/work/attitude")
	for fileName in fileList:
		if "attitude" in fileName:
			os.remove(inputArgs.sessionFolder[0]+"/media/work/attitude"+"/"+fileName)
	os.rmdir(inputArgs.sessionFolder[0]+"/media/work/attitude")
	#os.rmdir(inputArgs.sessionFolder[0]+"/media/work")

	# remove working directory with temporary files in shared memmory
	fileList = os.listdir("/dev/shm/attitude2video")
	for fileName in fileList:
		os.remove("/dev/shm/attitude2video"+"/"+fileName)
	os.rmdir("/dev/shm/attitude2video")
	
	os.system("rm -f /run/shm/attitude2video.lock")
	
def progressbar(progress, prefix = "", postfix = "", size = 60) :
	x = int(size*progress)
	sys.stdout.write("%s [%s%s] %d%% %s\r" % (prefix, "#"*x, "."*(size-x), (int)(progress*100), postfix))
	sys.stdout.flush()
	
if __name__ == "__main__":			# this is not a module
	
	parseInput()	# what do we have to do
	
	processInput()	# doing what we have to do
	
	print ""		# for estetic output

