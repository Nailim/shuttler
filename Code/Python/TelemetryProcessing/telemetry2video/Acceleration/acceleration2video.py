import sys
import os

#import commands		# for command line

import argparse

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
	
	os.system("touch /run/shm/acceleration2video.lock")
	
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
	if not os.path.exists("/dev/shm/acceleration2video") :
		os.makedirs("/dev/shm/acceleration2video")
		
		# prepare image templates
		os.system("cp "+scriptDir+"/resources/x_axis.png /dev/shm/acceleration2video/x_axis.png")
		os.system("cp "+scriptDir+"/resources/y_axis.png /dev/shm/acceleration2video/y_axis.png")
		os.system("cp "+scriptDir+"/resources/z_axis.png /dev/shm/acceleration2video/z_axis.png")
		os.system("cp "+scriptDir+"/resources/controls.png /dev/shm/acceleration2video/controls.png")
		os.system("cp "+scriptDir+"/resources/background.png /dev/shm/acceleration2video/background.png")
	
	# create working directory
	if not os.path.exists(inputArgs.sessionFolder[0]+"/media/work") :
		os.makedirs(inputArgs.sessionFolder[0]+"/media/work")
	if not os.path.exists(inputArgs.sessionFolder[0]+"/media/work/acceleration") :
		os.makedirs(inputArgs.sessionFolder[0]+"/media/work/acceleration")
	
	# magic
	for x in range(0, len(data_graZ)):
		progressbar(((x*1.0+1)/len(data_graZ)), "Rendering:", "(" + str(x+1) + "/" + str(len(data_graZ)) + ") " + "acceleration_%07d.jpg" % (x+1), 20)

		os.system("composite -background \"transparent\" -alpha on -gravity north -geometry -"+str(298)+"+"+str(43+(data_graZ[x]*-6.422))+" /dev/shm/acceleration2video/z_axis.png /dev/shm/acceleration2video/background.png /dev/shm/acceleration2video/gra_z_axis.png")
		os.system("composite -background \"transparent\" -alpha on -gravity north -geometry -"+str(137)+"+"+str(43+(data_graY[x]*6.422))+" /dev/shm/acceleration2video/y_axis.png /dev/shm/acceleration2video/background.png /dev/shm/acceleration2video/gra_y_axis.png")
		os.system("composite -background \"transparent\" -alpha on -gravity north -geometry -"+str(137+(data_graX[x]*6.422))+"+"+str(43)+" /dev/shm/acceleration2video/x_axis.png /dev/shm/acceleration2video/background.png /dev/shm/acceleration2video/gra_x_axis.png")
		
		os.system("composite -background \"transparent\" -alpha on -gravity north -geometry +"+str(22)+"+"+str(43+(data_aclZ[x]*6.422))+" /dev/shm/acceleration2video/z_axis.png /dev/shm/acceleration2video/background.png /dev/shm/acceleration2video/acl_z_axis.png")
		os.system("composite -background \"transparent\" -alpha on -gravity north -geometry +"+str(183)+"+"+str(43+(data_aclY[x]*6.422))+" /dev/shm/acceleration2video/y_axis.png /dev/shm/acceleration2video/background.png /dev/shm/acceleration2video/acl_y_axis.png")
		os.system("composite -background \"transparent\" -alpha on -gravity north -geometry +"+str(183+(data_aclX[x]*6.422))+"+"+str(43)+" /dev/shm/acceleration2video/x_axis.png /dev/shm/acceleration2video/background.png /dev/shm/acceleration2video/acl_x_axis.png")
		
		os.system("convert -size 640x360 xc:black /dev/shm/acceleration2video/controls.png -composite /dev/shm/acceleration2video/gra_z_axis.png -composite /dev/shm/acceleration2video/gra_y_axis.png -composite /dev/shm/acceleration2video/gra_x_axis.png -composite /dev/shm/acceleration2video/acl_z_axis.png -composite /dev/shm/acceleration2video/acl_y_axis.png -composite /dev/shm/acceleration2video/acl_x_axis.png -composite /dev/shm/acceleration2video/composed.jpg")
		
		os.system("cp /dev/shm/acceleration2video/composed.jpg "+inputArgs.sessionFolder[0]+"/media/work/acceleration/acceleration_"+"%07d.jpg" % (x+1,))
	
	os.system("avconv -r 30 -i "+inputArgs.sessionFolder[0]+"/media/work/acceleration/acceleration_"+"%07d.jpg -qscale 1 -b 1300k -vcodec libx264 "+inputArgs.sessionFolder[0]+"/media/acceleration.mp4")
	
	# remove working directory with temporary files
	fileList = os.listdir(inputArgs.sessionFolder[0]+"/media/work/acceleration")
	for fileName in fileList:
		if "acceleration" in fileName:
			os.remove(inputArgs.sessionFolder[0]+"/media/work/acceleration"+"/"+fileName)
	os.rmdir(inputArgs.sessionFolder[0]+"/media/work/acceleration")
	#os.rmdir(inputArgs.sessionFolder[0]+"/media/work")
	
	# remove working directory with temporary files in shared memmory
	fileList = os.listdir("/dev/shm/acceleration2video")
	for fileName in fileList:
		os.remove("/dev/shm/acceleration2video"+"/"+fileName)
	os.rmdir("/dev/shm/acceleration2video")
	
	os.system("rm -f /run/shm/acceleration2video.lock")
	
def progressbar(progress, prefix = "", postfix = "", size = 60) :
	x = int(size*progress)
	sys.stdout.write("%s [%s%s] %d%% %s\r" % (prefix, "#"*x, "."*(size-x), (int)(progress*100), postfix))
	sys.stdout.flush()
	
if __name__ == "__main__":			# this is not a module
	
	parseInput()	# what do we have to do
	
	processInput()	# doing what we have to do
	
	print ""		# for estetic output

