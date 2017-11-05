import os
import argparse
import cv
import sys

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
	
	os.system("touch /run/shm/attitude2video_opencv.lock")
	
	data_yaw=[]
	data_pitch=[]
	data_roll=[]
	
	data_speed=[]
	data_altitude=[]
	data_climb=[]
	
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
	
	sourceFile_climb = open(inputArgs.sessionFolder[0]+"/logs/interpolated_altitude_speed_30fps.txt", 'r')
	for line in sourceFile_climb :
		data_climb.append(float(line))
	sourceFile_climb.close()
	
	# create working directory in shared memmory
	if not os.path.exists("/dev/shm/attitude2video_opencv") :
		os.makedirs("/dev/shm/attitude2video_opencv")
		
		# prepare image templates
		os.system("cp "+scriptDir+"/resources_opencv/speed.png /dev/shm/attitude2video_opencv/speed.png")
		os.system("cp "+scriptDir+"/resources_opencv/climb.png /dev/shm/attitude2video_opencv/climb.png")
		os.system("cp "+scriptDir+"/resources_opencv/center.png /dev/shm/attitude2video_opencv/center.png")
		os.system("cp "+scriptDir+"/resources_opencv/horizon.png /dev/shm/attitude2video_opencv/horizon.png")
		os.system("cp "+scriptDir+"/resources_opencv/terrain.png /dev/shm/attitude2video_opencv/terrain.png")
		os.system("cp "+scriptDir+"/resources_opencv/compass.png /dev/shm/attitude2video_opencv/compass.png")
		os.system("cp "+scriptDir+"/resources_opencv/altitude.png /dev/shm/attitude2video_opencv/altitude.png")
		os.system("cp "+scriptDir+"/resources_opencv/background.png /dev/shm/attitude2video_opencv/background.png")

	
	# create working directory
	if not os.path.exists(inputArgs.sessionFolder[0]+"/media/work") :
		os.makedirs(inputArgs.sessionFolder[0]+"/media/work")
	if not os.path.exists(inputArgs.sessionFolder[0]+"/media/work/attitude_opencv") :
		os.makedirs(inputArgs.sessionFolder[0]+"/media/work/attitude_opencv")
		
		
	## Apptly named MAGIC
	# But before that, let's load the templates
	originalSpeed = cv.LoadImage("/dev/shm/attitude2video_opencv/speed.png")
	originalClimb = cv.LoadImage("/dev/shm/attitude2video_opencv/climb.png")
	originalCenter = cv.LoadImage("/dev/shm/attitude2video_opencv/center.png")
	originalHorizon = cv.LoadImage("/dev/shm/attitude2video_opencv/horizon.png")	# -1 to load with alpha channel
	originalTerrain = cv.LoadImage("/dev/shm/attitude2video_opencv/terrain.png")
	originalCompass = cv.LoadImage("/dev/shm/attitude2video_opencv/compass.png")
	originalAltitude = cv.LoadImage("/dev/shm/attitude2video_opencv/altitude.png")
	
	for x in range(0, len(data_speed)):
		progressbar(((x*1.0+1)/len(data_speed)), "Rendering openCV:", "(" + str(x+1) + "/" + str(len(data_speed)) + ") " + "attitude_%07d.jpg" % (x+1), 20)
	#for x in range(0,1):
		currentSpeed = cv.CloneImage(originalSpeed)
		currentClimb = cv.CloneImage(originalClimb)
		currentCenter = cv.CloneImage(originalCenter)
		currentHorizon = cv.CloneImage(originalHorizon)
		currentTerrain = cv.CloneImage(originalTerrain)
		currentCompass = cv.CloneImage(originalCompass)
		currentAltitude = cv.CloneImage(originalAltitude)
		
# 0 ------------------------------------------------	
		
		# Rotate center
		picCenter = (originalHorizon.width/2.0, originalHorizon.height/2.0)
		outputMatrix = cv.CreateMat(2, 3, cv.CV_32F)
		cv.GetRotationMatrix2D(picCenter , (data_roll[x]) , 1.0 , outputMatrix) # Positive number goes counter-clockwise, counter to what the original code did. Ergo, do * away with -1
		cv.WarpAffine(originalHorizon, currentHorizon, outputMatrix, cv.CV_WARP_FILL_OUTLIERS+cv.CV_INTER_LINEAR, fillval=(0,0,0,0))
				
		# Rotate horizon
		picCenter = (originalCenter.width/2.0, originalCenter.height/2.0)
		outputMatrix = cv.CreateMat(2, 3, cv.CV_32F)
		cv.GetRotationMatrix2D(picCenter , (data_roll[x]) , 1.0 , outputMatrix)
		cv.WarpAffine(originalCenter, currentCenter, outputMatrix, cv.CV_WARP_FILL_OUTLIERS+cv.CV_INTER_LINEAR, fillval=(0,0,0,0))
		
		
# 1 ------------------------------------------------		
	
		odmik = data_pitch[x]*3.0*-1.0	# Reverse it again.
		width = 640
		height = 360
		picCenter = (currentHorizon.width/2.0, currentHorizon.height/2.0);
		regionOfInterest = (int(picCenter[0]-(width/2.0)), int(picCenter[1]-(height/2.0)+(odmik)), int(width), int(height))
		thirdHorizon = cv.GetSubRect(currentHorizon, regionOfInterest)
		
		# Instead of copy-ing we do subtraction. Works, since we're using (mostly) black for displaying things. Templates need to be alpha-less and inverted.
		cv.Sub(thirdHorizon, originalTerrain, thirdHorizon);
		cv.Sub(thirdHorizon, currentCenter, thirdHorizon);
	

# 2 ------------------------------------------------
		zacetnaPozicija = width/2.0;
		if data_yaw[x] <= 180:
			zacetnaPozicija = zacetnaPozicija + 3.9*data_yaw[x]
		else:
			zacetnaPozicija = zacetnaPozicija + 3.9*(360-data_yaw[x])

		
		# Speed imporvement. Which isn't faster. Yay.
		compassHeight = 50
		regionOfInterest = (int(currentCompass.width/2.0-zacetnaPozicija), int(0), int(width), int(compassHeight))
		currentCompass = cv.GetSubRect(currentCompass, regionOfInterest)
		regionOfInterest = (int(0), int(0), int(width), int(compassHeight))
		pointerToSpace = cv.GetSubRect(thirdHorizon, regionOfInterest)
		cv.Sub(pointerToSpace, currentCompass, pointerToSpace)

# 3 ------------------------------------------------		

		speedCenter = (originalSpeed.width/2.0, originalSpeed.height/2.0)
		speedWidth = originalSpeed.width
		speedHeight = originalSpeed.height
		zacetnaPozicija = speedHeight/2.0	
		if data_speed[x] < 0:
			zacetnaPozicija = zacetnaPozicija - 210
		elif data_speed[x] >= 100:
			zacetnaPozicija = zacetnaPozicija + 210
		else:
			zacetnaPozicija = zacetnaPozicija + (200-(data_speed[x]*4))
		
		doDol = speedHeight - zacetnaPozicija
		if (doDol > 130):
			doDol = 130
		
		doGor = zacetnaPozicija
		if (doGor > 130):
			doGor = 130
			
		regionOfInterest = (int(0), int(zacetnaPozicija-doGor), int(speedWidth), int(doDol+doGor))
		currentSpeed = cv.GetSubRect(originalSpeed, regionOfInterest)

		regionOfInterest = (int(width/2.0 - 190 - currentSpeed.width/2.0),
			int(height/2.0 - doGor),
			int(speedWidth),
			int(doGor+doDol))
		
		pointerToWhereToCopySpeed = cv.GetSubRect(thirdHorizon, regionOfInterest)
		cv.Sub(pointerToWhereToCopySpeed, currentSpeed, pointerToWhereToCopySpeed)
		
# 4 ------------------------------------------------		

		lokalniOdmik = 0
		if data_altitude[x] < 0:
			lokalniOdmik = 2010
		elif data_altitude[x] >= 1000:
			lokalniOdmik = -2010
		else:
			lokalniOdmik = -(2000-(data_altitude[x]*4))
		
		temp = currentAltitude.height / 2.0
		
		doDol =  (temp + lokalniOdmik)
		if (doDol > 130):
			doDol = 130
		
		doGor = temp - lokalniOdmik
		if (doGor > 130):
			doGor = 130

		regionOfInterest = (int(0),
			int(currentAltitude.height/2.0 - lokalniOdmik - doGor),
			int(speedWidth),
			int(doGor+doDol))
			
		cutAltitude = cv.GetSubRect(currentAltitude, regionOfInterest)
				
		regionOfInterest = (int(width/2.0 + 160), int(height/2.0 - doGor), int(70), int(doGor+doDol))
		pointerToWhereToCopyAltitude = cv.GetSubRect(thirdHorizon, regionOfInterest)

		cv.Sub(pointerToWhereToCopyAltitude, cutAltitude, pointerToWhereToCopyAltitude)	

#   ------------------------------------------------
		lokalniOdmik = 0
		if data_climb[x] < -10:
			lokalniOdmik = -410
		elif data_climb[x] >= 10:
			lokalniOdmik = 410
		else:
			lokalniOdmik = ((data_climb[x]*4*10))
		
		temp = currentClimb .height / 2.0
		
		doDol =  (temp + lokalniOdmik)
		if (doDol > 130):
			doDol = 130
		
		doGor = temp - lokalniOdmik
		if (doGor > 130):
			doGor = 130
		
		regionOfInterest = (int(0),
			int(currentClimb.height/2.0 - lokalniOdmik - doGor),
			int(speedWidth),
			int(doGor+doDol))
		
		cutClimb = cv.GetSubRect(currentClimb, regionOfInterest)
		
		regionOfInterest = (int(width/2.0 + 245), int(height/2.0 - doGor), int(70), int(doGor+doDol))
		pointerToWhereToCopyClimb = cv.GetSubRect(thirdHorizon, regionOfInterest)
	
		cv.Sub(pointerToWhereToCopyClimb, cutClimb, pointerToWhereToCopyClimb)	
		
# 5 ------------------------------------------------	
		
		cv.SaveImage("/dev/shm/attitude2video_opencv/composed.png", thirdHorizon)
		os.system("cp /dev/shm/attitude2video_opencv/composed.png "+inputArgs.sessionFolder[0]+"/media/work/attitude_opencv/attitude_"+"%07d.png" % (x+1,))	

# CLEAR ALL VARIABLES! You know, memory leaks and such.
# 6 ------------------------------------------------		


	# KONEC
	os.system("avconv -r 30 -i "+inputArgs.sessionFolder[0]+"/media/work/attitude_opencv/attitude_"+"%07d.png -qscale 1 -b 1300k -vcodec libx264 "+inputArgs.sessionFolder[0]+"/media/attitude.mp4")
	
	# remove working directory with temporary files
	fileList = os.listdir(inputArgs.sessionFolder[0]+"/media/work/attitude_opencv")
	for fileName in fileList:
		os.remove(inputArgs.sessionFolder[0]+"/media/work/attitude_opencv"+"/"+fileName)
	os.rmdir(inputArgs.sessionFolder[0]+"/media/work/attitude_opencv")
	#os.rmdir(inputArgs.sessionFolder[0]+"/media/work")
	
	# remove working directory with temporary files in shared memmory
	fileList = os.listdir("/dev/shm/attitude2video_opencv")
	for fileName in fileList:
		os.remove("/dev/shm/attitude2video_opencv"+"/"+fileName)
	os.rmdir("/dev/shm/attitude2video_opencv")
	
	os.system("rm -f /run/shm/attitude2video_opencv.lock")

def progressbar(progress, prefix = "", postfix = "", size = 60) :
	x = int(size*progress)
	sys.stdout.write("%s [%s%s] %d%% %s\r" % (prefix, "#"*x, "."*(size-x), (int)(progress*100), postfix))
	sys.stdout.flush()
	
if __name__ == "__main__":			# this is not a module
	
	parseInput()	# what do we have to do
	processInput()	# doing what we have to do
	print ""		# for estetic output
		
