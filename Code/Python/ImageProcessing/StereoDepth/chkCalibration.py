import os
#import cv
import cv2
import glob
import argparse
import numpy as np

global inputParser	# just a reminder, it's used as a global variable
global inputArgs	# just a reminder, it's used as a global variable

def parseInput() :
	
	global inputParser
	global inputArgs
	
	inputParser = argparse.ArgumentParser(description='Calibrate camera and fix image lense distortion. (Calibration images should be in FOLDER/calibration/images)')
	
	inputParser.add_argument('path', nargs='+')
	
	inputParser.add_argument('-bw', '--boardWidth', dest='boardWidth', action='store', default=10, type=int, help='number of horizontal corners on the calibration checkerboard')
	inputParser.add_argument('-bh', '--boardHeight', dest='boardHeight', action='store', default=7, type=int, help='number of vertical corners on the calibration checkerboard')
	inputParser.add_argument('-c', '--crop', dest='crop', action='store', default=0, type=int, help='crop fixed images: 1 - yes, 0 - no')

	inputParser.add_argument('-d', '--debug', action='store_true', help='visualize keyboard detection and lense correction on calibration images')
	
	inputArgs = inputParser.parse_args()

def processInput() :
	board_w = inputArgs.boardWidth		# number of horizontal corners
	board_h = inputArgs.boardHeight		# number of vertical corners
	
	board_n = board_w*board_h			# no of total corners
	board_sz = (board_w, board_h)		#size of board
	
	# creation of memory storages
	image_points = []
	object_points = []

	patternPoints = np.zeros( (np.prod(board_sz), 3), np.float32 )
	patternPoints[:,:2] = np.indices(board_sz).T.reshape(-1, 2)

	imageSize = []
	
	# do the thing - calculate intrinsic parameters and fix lense distortion
	for cam in range(len(inputArgs.path)):
		print "Calibrating set: " + str(cam+1)
		images = glob.glob(inputArgs.path[cam]+'/calibration/images/*.*')	# make sure there are images there
		if len(images) == 0:
			print "                 no images"
			break
		else:
			print "                 calculating parameters"
		
		for fname in images :
			# load and convert image
			image = cv2.imread(fname)
			gray_image = cv2.cvtColor(image,cv2.COLOR_BGR2GRAY)
			
			imageSize = image.shape[1],image.shape[0]
			
			found, corners = cv2.findChessboardCorners(gray_image, board_sz, flags=cv2.cv.CV_CALIB_CB_ADAPTIVE_THRESH)
			
			# if got a good image,draw chess board
			if found:
				cv2.cornerSubPix(gray_image, corners, (11,11), (-1,-1), (cv2.cv.CV_TERMCRIT_EPS+cv2.cv.CV_TERMCRIT_ITER,30,0.1))				
				
				cv2.drawChessboardCorners(image, board_sz, corners, 1)
				
				# save the images with the detected corners
				head, tail = os.path.split(fname)
				# visualized
				if inputArgs.debug == 1:
					cv2.imwrite(head+"/"+"chk_"+tail, image)
				     
				# if got a good image, add to matrix
				if len(corners) == board_n:
					image_points.append(corners)
					object_points.append(patternPoints)
					
		
		# camera calibration
		cameraMatrix = np.zeros((3, 3))
		distCoefs = np.zeros(4)

		rc, cameraMatrix, distCoeffs, rvecs, tvecs = cv2.calibrateCamera(object_points, image_points, board_sz, cameraMatrix, distCoefs)
		
		# save & load data, to see if it's saved properly
		# storing results in txt files
		np.savetxt(inputArgs.path[cam]+'/calibration/intrinsics.txt', cameraMatrix)
		np.savetxt(inputArgs.path[cam]+'/calibration/distortion.txt', distCoeffs)
		# Loading from xml files
		intrinsic = np.loadtxt(inputArgs.path[cam]+'/calibration/intrinsics.txt')
		distortion = np.loadtxt(inputArgs.path[cam]+'/calibration/distortion.txt')

		newCameraMatrix, newExtents = cv2.getOptimalNewCameraMatrix(intrinsic, distortion, imageSize, 1.0)
		mapx, mapy = cv2.initUndistortRectifyMap(intrinsic, distortion, None, newCameraMatrix, imageSize, cv2.CV_32FC1)
		
		imgCount = 0;
		
		# visualization of the calibrated checkerd board
		if inputArgs.debug == 1:
			images = glob.glob(inputArgs.path[cam]+'/calibration/images/*.*')
			for fname in images:
				imgCount = imgCount + 1
				print "                 correcting (debug) image " + str(imgCount) + " / " + str(len(images)) 
				image = cv2.imread(fname)
				newImage = cv2.remap(image, mapx, mapy, cv2.INTER_LINEAR)
				# crop the image
				if inputArgs.crop == 1:
					x, y, w, h = newExtents	# roi
					newImage = newImage[y:y+h, x:x+w]
				# save the images with the detected corners visualized
				head, tail = os.path.split(fname)
				cv2.imwrite(head+"/"+"cal_"+tail, newImage)
		# correcting images
		else:
			images = glob.glob(inputArgs.path[cam]+'/*.*')
			for fname in images:
				imgCount = imgCount + 1
				print "                 correcting (debug) image " + str(imgCount) + " / " + str(len(images)) 
				image = cv2.imread(fname)
				newImage = cv2.remap(image, mapx, mapy, cv2.INTER_LINEAR)
				# crop the image
				if inputArgs.crop == 1:
					x, y, w, h = newExtents	# roi
					newImage = newImage[y:y+h, x:x+w]
				# save the images with the detected corners visualized (!!!) 
				head, tail = os.path.split(fname)
				cv2.imwrite(head+"/"+"cal_"+tail, newImage)
				
if __name__ == "__main__":			# this is not a module
	
	parseInput()	# what do we have to do
	
	processInput()	# doing what we have to do
	
	print ""		# for estetic output
