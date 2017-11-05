# for new opencv
#import os,sys
#os.chdir(os.path.expanduser('~/opencv-2.4.6.1/lib'))
#sys.path.append(os.path.expanduser('~/opencv-2.4.6.1/lib/python2.7/dist-packages'))

# before starting
#export PYTHONPATH=~/opencv-2.4.6.1/lib/python2.7/dist-packages

import os
import cv
import cv2
import string
import argparse
import numpy as np
from matplotlib import pyplot as plt

global inputParser	# just a reminder, it's used as a global variable
global inputArgs	# just a reminder, it's used as a global variable

def parseInput() :
	
	global inputParser
	global inputArgs
	
	inputParser = argparse.ArgumentParser(description='Rectification of stereo images using epipolar geometry.')
	
	inputParser.add_argument('-l', '--left', dest='left', action='store', default="", type=str, help='left image')
	inputParser.add_argument('-r', '--right', dest='right', action='store', default="", type=str, help='right image')
	inputParser.add_argument('-n', '--name', dest='name', action='store', default="fm_out", type=str, help='name of the current set (used to save output values)')
	
	inputParser.add_argument('-d', '--debug', action='store_true', help='debug output')
	
	inputArgs = inputParser.parse_args()

def processInput() :
	print ""
	if inputArgs.left == "" or inputArgs.right == "":
		print "Missing images!"
		quit()
	
	# here we go ...
	
	# load image pair
	img_l = cv2.imread(inputArgs.left)
	img_r = cv2.imread(inputArgs.right)
	
	if img_l == None or img_r == None:
		print "Missing images!"
		quit()
	
	# disparity range
	window_size = 5
	min_disp = 16
	num_disp = 112-min_disp
	
	stereo = cv2.StereoSGBM(minDisparity = min_disp,
		numDisparities = num_disp,
		SADWindowSize = window_size,
		uniquenessRatio = 10,
		speckleWindowSize = 100,
		speckleRange = 32,
		disp12MaxDiff = 1,
		P1 = 8*3*window_size**2,
		P2 = 32*3*window_size**2,
		fullDP = False
	)
	
	print 'Computing disparity ...'
	disp = stereo.compute(img_l, img_r).astype(np.float32) / 16.0
	
	cv2.imwrite(inputArgs.name + "_disparity.jpg", disp)
	

if __name__ == "__main__":			# this is not a module
	
	parseInput()	# what do we have to do
	
	processInput()	# doing what we have to do
	
	print ""		# for estetic output
