import sys
import os

#import commands		# for command line

import argparse
import cv

global inputParser	# just a reminder, it's used as a global variable
global inputArgs	# just a reminder, it's used as a global variable

def parseInput() :
	
	global inputParser
	global inputArgs
	
	inputParser = argparse.ArgumentParser(description='Render telemetry (gravity and acceleration) to video from Anemoi autopilot')
	
	inputParser.add_argument('sessionFolder', nargs=1, help='folder containing processed autopilot autopilot (30 FPS)')
	
	inputArgs = inputParser.parse_args()
	
def processInput() :
	
	print inputArgs.sessionFolder
	
	scriptDir = os.path.dirname(os.path.abspath(__file__))
	
	os.system("touch /run/shm/acceleration2video_opencv.lock")
	
	data_aclX=[]
	data_aclY=[]
	data_aclZ=[]
	
	data_graX=[]
	data_graY=[]
	data_graZ=[]
	
	sourceFile_aclX = open(inputArgs.sessionFolder[0]+"/logs/interpolated_aclX_30fps.txt", 'r')
	for line in sourceFile_aclX :
		data_aclX.append(float(line))
	sourceFile_aclX.close()
	
	sourceFile_aclY = open(inputArgs.sessionFolder[0]+"/logs/interpolated_aclY_30fps.txt", 'r')
	for line in sourceFile_aclY :
		data_aclY.append(float(line))
	sourceFile_aclY.close()
	
	sourceFile_aclZ = open(inputArgs.sessionFolder[0]+"/logs/interpolated_aclZ_30fps.txt", 'r')
	for line in sourceFile_aclZ :
		data_aclZ.append(float(line))
	sourceFile_aclZ.close()
	
	sourceFile_graX = open(inputArgs.sessionFolder[0]+"/logs/interpolated_graX_30fps.txt", 'r')
	for line in sourceFile_graX :
		data_graX.append(float(line))
	sourceFile_graX.close()
	
	sourceFile_graY = open(inputArgs.sessionFolder[0]+"/logs/interpolated_graY_30fps.txt", 'r')
	for line in sourceFile_graY :
		data_graY.append(float(line))
	sourceFile_graY.close()
	
	sourceFile_graZ = open(inputArgs.sessionFolder[0]+"/logs/interpolated_graZ_30fps.txt", 'r')
	for line in sourceFile_graZ :
		data_graZ.append(float(line))
	sourceFile_graZ.close()
	
	# create working directory in shared memmory
	if not os.path.exists("/dev/shm/acceleration2video_opencv") :
		os.makedirs("/dev/shm/acceleration2video_opencv")
		
		# prepare image templates
		os.system("cp "+scriptDir+"/resources_opencv/x_axis.png /dev/shm/acceleration2video_opencv/x_axis.png")
		os.system("cp "+scriptDir+"/resources_opencv/y_axis.png /dev/shm/acceleration2video_opencv/y_axis.png")
		os.system("cp "+scriptDir+"/resources_opencv/z_axis.png /dev/shm/acceleration2video_opencv/z_axis.png")
		os.system("cp "+scriptDir+"/resources_opencv/yz_axis_mask.png /dev/shm/acceleration2video_opencv/yz_axis_mask.png")
		os.system("cp "+scriptDir+"/resources_opencv/x_axis_mask.png /dev/shm/acceleration2video_opencv/x_axis_mask.png")
		os.system("cp "+scriptDir+"/resources_opencv/controls.png /dev/shm/acceleration2video_opencv/controls.png")
	
	# create working directory
	if not os.path.exists(inputArgs.sessionFolder[0]+"/media/work") :
		os.makedirs(inputArgs.sessionFolder[0]+"/media/work")
	if not os.path.exists(inputArgs.sessionFolder[0]+"/media/work/acceleration_opencv") :
		os.makedirs(inputArgs.sessionFolder[0]+"/media/work/acceleration_opencv")
	
	# More magic
	originalControls = cv.LoadImage("/dev/shm/acceleration2video_opencv/controls.png")	#640x360px
	originalAxisX = cv.LoadImage("/dev/shm/acceleration2video_opencv/x_axis.png")	# 3x26px
	originalAxisY = cv.LoadImage("/dev/shm/acceleration2video_opencv/y_axis.png")	# 26x3px
	originalAxisZ = cv.LoadImage("/dev/shm/acceleration2video_opencv/z_axis.png")	# 26x3px
	originalAxisMaskYZ = cv.LoadImage("/dev/shm/acceleration2video_opencv/yz_axis_mask.png")	# 26x3px
	originalAxisMaskX = cv.LoadImage("/dev/shm/acceleration2video_opencv/x_axis_mask.png")	# 26x3px	
	xWidth = 3
	xHeight = 26
	yzWidth = 26
	yzHeight = 3
	controlsWidth = 640
	controlsHeight = 360


	# magic
	for x in range(0, len(data_graZ)):
		progressbar(((x*1.0+1)/len(data_graZ)), "Rendering:", "(" + str(x+1) + "/" + str(len(data_graZ)) + ") " + "acceleration_%07d.jpg" % (x+1), 20)
		
		currentControls = cv.CloneImage(originalControls);
		
		# 1.1/3		
		tempHeight = ((controlsHeight/2) + (43+(-1*data_graZ[x]*6.422)) - (yzHeight/2));
		if (tempHeight > (controlsHeight-yzHeight)):
			tempHeight = (controlsHeight-yzHeight)
		elif (tempHeight < 0):
			tempHeight = 0
		regionOfInterest = (	int((controlsWidth/2) - 298 - (yzWidth/2)), 
					#int((controlsHeight/2) + (43+(data_graZ[x]*-6.422)) - (yzHeight/2)), 
					int(tempHeight),					
					int(yzWidth), 
					int(yzHeight)	)
		thirdHorizon = cv.GetSubRect(currentControls, regionOfInterest)
		cv.Copy(originalAxisZ, thirdHorizon, originalAxisMaskYZ)
		#cv.SaveImage("/dev/shm/output7.png", currentControls)
		
		# 1.2/3
		tempHeight = ((controlsHeight/2) + (43+(data_graY[x]*6.422)) - (yzHeight/2));
		if (tempHeight > (controlsHeight-yzHeight)):
			tempHeight = (controlsHeight-yzHeight)
		elif (tempHeight < 0):
			tempHeight = 0
		regionOfInterest = (	int((controlsWidth/2) - 137 - (yzWidth/2)), 
					#int((controlsHeight/2) + (43+(data_graY[x]*6.422)) - (yzHeight/2)), 
					int(tempHeight),
					int(yzWidth), 
					int(yzHeight)	)
		thirdHorizon = cv.GetSubRect(currentControls, regionOfInterest)
		cv.Copy(originalAxisY, thirdHorizon, originalAxisMaskYZ)
		#cv.SaveImage("/dev/shm/output7.png", currentControls)

		# 1.3/3
		tempWidth = int((controlsWidth/2) + (183+(data_graX[x]*6.422)) - (xWidth/2))
		if (tempWidth < 0):
			tempWidth = 0
		# elif ni potreben
		regionOfInterest = (	int((controlsWidth/2) - (137+(data_graX[x]*6.422)) - (xWidth/2)), 
					int((controlsHeight/2) + (43) - (xHeight/2)), 
					int(xWidth), 
					int(xHeight)	)
		thirdHorizon = cv.GetSubRect(currentControls, regionOfInterest)
		cv.Copy(originalAxisX, thirdHorizon, originalAxisMaskX)
		#cv.SaveImage("/dev/shm/output7.png", currentControls)


		# 2.1/3
		tempHeight = ((controlsHeight/2) + (43+(data_aclZ[x]*6.422)) - (yzHeight/2));
		if (tempHeight > (controlsHeight-yzHeight)):
			tempHeight = (controlsHeight-yzHeight)
		elif (tempHeight < 0):
			tempHeight = 0
		regionOfInterest = (	int((controlsWidth/2) + 22 - (yzWidth/2)), 
					#int((controlsHeight/2) + (43+(data_aclZ[x]*6.422)) - (yzHeight/2)), 
					int(tempHeight),
					int(yzWidth), 
					int(yzHeight)	)
		thirdHorizon = cv.GetSubRect(currentControls, regionOfInterest)
		cv.Copy(originalAxisZ, thirdHorizon, originalAxisMaskYZ)
		#cv.SaveImage("/dev/shm/output7.png", currentControls)


		# 2.2/3
		tempHeight = ((controlsHeight/2) + (43+(data_aclY[x]*6.422)) - (yzHeight/2));
		if (tempHeight > (controlsHeight-yzHeight)):
			tempHeight = (controlsHeight-yzHeight)
		elif (tempHeight < 0):
			tempHeight = 0
		regionOfInterest = (	int((controlsWidth/2) + 183 - (yzWidth/2)), 
					#int((controlsHeight/2) + (43+(data_aclY[x]*6.422)) - (yzHeight/2)), 
					int(tempHeight),
					int(yzWidth), 
					int(yzHeight)	)

		thirdHorizon = cv.GetSubRect(currentControls, regionOfInterest)
		cv.Copy(originalAxisY, thirdHorizon, originalAxisMaskYZ)
		#cv.SaveImage("/dev/shm/output7.png", currentControls)

		# 2.3/3
		tempWidth = int((controlsWidth/2) + (183+(data_aclX[x]*6.422)) - (xWidth/2))
		if (tempWidth > (controlsWidth - xWidth)):
			tempWidth = (controlsWidth - xWidth)
		# elif ni potreben

		regionOfInterest = (	#int((controlsWidth/2) + (183+(data_aclX[x]*6.422)) - (xWidth/2)), 
					int(tempWidth),					
					int((controlsHeight/2) + (43) - (xHeight/2)), 
					int(xWidth), 
					int(xHeight)	)
		thirdHorizon = cv.GetSubRect(currentControls, regionOfInterest)
		cv.Copy(originalAxisX, thirdHorizon, originalAxisMaskX)
		#cv.SaveImage("/dev/shm/output7.png", currentControls)
		
		cv.SaveImage("/dev/shm/acceleration2video_opencv/composed.jpg", currentControls)
		os.system("cp /dev/shm/acceleration2video_opencv/composed.jpg "+inputArgs.sessionFolder[0]+"/media/work/acceleration_opencv/acceleration_"+"%07d.jpg" % (x+1,))
	
	#REENABLE!
	os.system("avconv -r 30 -i "+inputArgs.sessionFolder[0]+"/media/work/acceleration_opencv/acceleration_"+"%07d.jpg -qscale 1 -b 1300k -vcodec libx264 "+inputArgs.sessionFolder[0]+"/media/acceleration.mp4")
	
	# remove working directory with temporary files
	fileList = os.listdir(inputArgs.sessionFolder[0]+"/media/work/acceleration_opencv")
	for fileName in fileList:
		if "acceleration" in fileName:
			os.remove(inputArgs.sessionFolder[0]+"/media/work/acceleration_opencv"+"/"+fileName)
	os.rmdir(inputArgs.sessionFolder[0]+"/media/work/acceleration_opencv")
	#os.rmdir(inputArgs.sessionFolder[0]+"/media/work")
	
	# remove working directory with temporary files in shared memmory
	fileList = os.listdir("/dev/shm/acceleration2video_opencv")
	for fileName in fileList:
		os.remove("/dev/shm/acceleration2video_opencv"+"/"+fileName)
	os.rmdir("/dev/shm/acceleration2video_opencv")
	
	os.system("rm -f /run/shm/acceleration2video_opencv.lock")
	
def progressbar(progress, prefix = "", postfix = "", size = 60) :
	x = int(size*progress)
	sys.stdout.write("%s [%s%s] %d%% %s\r" % (prefix, "#"*x, "."*(size-x), (int)(progress*100), postfix))
	sys.stdout.flush()
	
if __name__ == "__main__":			# this is not a module
	
	parseInput()	# what do we have to do
	
	processInput()	# doing what we have to do
	
	print ""		# for estetic output

