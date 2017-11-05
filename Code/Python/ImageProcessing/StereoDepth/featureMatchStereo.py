# for new opencv
#import os,sys
#os.chdir(os.path.expanduser('~/opencv-2.4.6.1/lib'))
#sys.path.append(os.path.expanduser('~/opencv-2.4.6.1/lib/python2.7/dist-packages'))

# before starting
#export PYTHONPATH=~/opencv-2.4.6.1/lib/python2.7/dist-packages

import os
#import cv
import cv2
import math
import argparse
import numpy as np

global inputParser	# just a reminder, it's used as a global variable
global inputArgs	# just a reminder, it's used as a global variable

def parseInput() :
	
	global inputParser
	global inputArgs
	
	inputParser = argparse.ArgumentParser(description='Match features between two stereo images.')
	
	inputParser.add_argument('-l', '--left', dest='left', action='store', default="", type=str, help='left image')
	inputParser.add_argument('-r', '--right', dest='right', action='store', default="", type=str, help='right image')
	inputParser.add_argument('-n', '--name', dest='name', action='store', default="fm_out", type=str, help='name of the current set (used to save output values)')
	inputParser.add_argument('-f', '--feature', dest='feature', action='store', default="sift", type=str, help='feature to use: sift, surf, orb, brisk')
	inputParser.add_argument('-m', '--match', dest='match', action='store', default="bf", type=str, help='match using: bf (bruteforce), flann')
	inputParser.add_argument('-p', '--proportion', dest='proportion', action='store', default=0.25, type=float, help='Lowe\'s distance test ratio')
	inputParser.add_argument('-s', '--stddev', dest='stddev', action='store', default=0.0, type=float, help='max standard deviation between angles of each point pair (stereo cheat, 0.0 don\'t; use, > 0.0 use this)')
	
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
	
	# we like them gray
	gray_l = cv2.cvtColor(img_l, cv2.COLOR_BGR2GRAY)
	gray_r = cv2.cvtColor(img_r, cv2.COLOR_BGR2GRAY)
	
	# which decetor are we using
	if inputArgs.feature == 'sift':
		detector = cv2.SIFT()
		norm = cv2.NORM_L2
	elif inputArgs.feature == 'surf':
		detector = cv2.SURF(800)
		norm = cv2.NORM_L2
	elif inputArgs.feature == 'orb':
		detector = cv2.ORB(400)
		norm = cv2.NORM_HAMMING
	elif inputArgs.feature == 'brisk':
		detector = cv2.BRISK()
		norm = cv2.NORM_HAMMING
	else:
		print "Wrong feature detector!"
		quit()
	
	# how are we matching detected features
	if inputArgs.match == 'bf':
		matcher = cv2.BFMatcher(norm)
        
	elif inputArgs.match == 'flann':
		# borrowed from: https://github.com/Itseez
		FLANN_INDEX_KDTREE = 1 # bug: flann enums are missing
		FLANN_INDEX_LSH = 6
		
		flann_params = []
		if norm == cv2.NORM_L2:
			flann_params = dict(algorithm = FLANN_INDEX_KDTREE, trees = 5)
		else:
			flann_params = dict(algorithm = FLANN_INDEX_LSH,
								table_number = 6, # 12
								key_size = 12, # 20
								multi_probe_level = 1) #2
		matcher = cv2.FlannBasedMatcher(flann_params, {}) # bug : need to pass empty dict (#1329)
	
	print "Using: " + inputArgs.feature + " with " + inputArgs.match
	print ""
	
	print "detecting ..."
	# find the keypoints and descriptors
	kp_l, des_l = detector.detectAndCompute(gray_l, None)
	kp_r, des_r = detector.detectAndCompute(gray_r, None)	
	
	print "Left image features: " +  str(len(kp_l))
	print "Right image features: " +  str(len(kp_l))
	print ""
	# visualization
	if inputArgs.debug == 1:
		# left
		img_l_tmp = img_l.copy()
		#for kp in kp_l:
		#	x = int(kp.pt[0])
		#	y = int(kp.pt[1])
		#	cv2.circle(img_l_tmp, (x, y), 2, (0, 0, 255))
		img_l_tmp = cv2.drawKeypoints(img_l_tmp, kp_l, img_l_tmp, (0, 0, 255), cv2.DRAW_MATCHES_FLAGS_DEFAULT)
		head, tail = os.path.split(inputArgs.left)
		cv2.imwrite(head+"/"+"feat_"+tail, img_l_tmp)
		# right
		img_r_tmp = img_r.copy()
		#for kp in kp_r:
		#	x = int(kp.pt[0])
		#	y = int(kp.pt[1])
		#	cv2.circle(img_r_tmp, (x, y), 2, (0, 0, 255))
		img_r_tmp = cv2.drawKeypoints(img_r_tmp, kp_r, img_r_tmp, (0, 0, 255), cv2.DRAW_MATCHES_FLAGS_DEFAULT)
		head, tail = os.path.split(inputArgs.right)
		cv2.imwrite(head+"/"+"feat_"+tail, img_r_tmp)
	
	print "matching ..."
	
	# match
	raw_matches = matcher.knnMatch(des_l, trainDescriptors = des_r, k = 2)
	print "Raw matches: " + str(len(raw_matches))
	
	# filter matches: per Lowe's ratio test
	filtered_matches = []
	mkp_l = []
	mkp_r = []
	
	for m in raw_matches:
		if len(m) == 2 and m[0].distance < m[1].distance * inputArgs.proportion:
			filtered_matches.append(m)
			mkp_l.append( kp_l[m[0].queryIdx] )
			mkp_r.append( kp_r[m[0].trainIdx] )
	print "Filtered matches: " + str(len(filtered_matches))
	
	# visualization
	if inputArgs.debug == 1:
		# draw points
		img_l_tmp = cv2.drawKeypoints(img_l_tmp, mkp_l, img_l_tmp, (255, 0, 0), cv2.DRAW_MATCHES_FLAGS_DRAW_RICH_KEYPOINTS)
		head, tail = os.path.split(inputArgs.left)
		#cv2.imwrite(head+"/"+"feat_"+tail, img_l_tmp)
		img_r_tmp = cv2.drawKeypoints(img_r_tmp, mkp_r, img_r_tmp, (255, 0, 0), cv2.DRAW_MATCHES_FLAGS_DRAW_RICH_KEYPOINTS)
		head, tail = os.path.split(inputArgs.right)
		#cv2.imwrite(head+"/"+"feat_"+tail, img_r_tmp)
		
		# merge image side by side
		h_l, w_l = img_l_tmp.shape[:2]
		h_r, w_r = img_r_tmp.shape[:2]
		img_tmp = np.zeros((max(h_l, h_l), w_r+w_r, 3), np.uint8)
		img_tmp[:h_l, :w_l] = img_l_tmp
		img_tmp[:h_r, w_l:w_l+w_r] = img_r_tmp
		
		# draw lines
		for m in filtered_matches:
			cv2.line(img_tmp, (int(round(kp_l[m[0].queryIdx].pt[0])), int(round(kp_l[m[0].queryIdx].pt[1]))), (int(w_l + round(kp_r[m[0].trainIdx].pt[0])), int(round(kp_r[m[0].trainIdx].pt[1]))), (255, 0, 0), 1)	
		
		cv2.imwrite(inputArgs.name + "_features.jpg", img_tmp)
	
	# filter matches: per direction (since it's a stereo pair, most of the points should have the same angle between them)
	if inputArgs.stddev != 0.0:
		ang_stddev = 360.0
		stddev = 180.0
		while abs(stddev) > inputArgs.stddev:
			ang_stddev = stddev
			raw_matches = []				# silly !!!
			for m in filtered_matches:		# silly !!!
				raw_matches.append(m)		# silly !!!
	
			filtered_matches = []
			mkp_l = []
			mkp_r = []
		
		
			ang = []
			for m in raw_matches:
				xDiff = kp_r[m[0].trainIdx].pt[0] - kp_l[m[0].queryIdx].pt[0]	#p2.x - p1.x
				yDiff = kp_r[m[0].trainIdx].pt[1] - kp_l[m[0].queryIdx].pt[1]	#p2.y - p1.y
				#print math.degrees(math.atan2(yDiff,xDiff))
				ang.append(math.degrees(math.atan2(yDiff,xDiff)))
		
			mean = np.mean(ang)
			differences = [(value - mean)**2 for value in ang]
			stddev = np.mean(differences) ** 0.5
			#print mean
			#print stddev
		
			ang = []
			for m in raw_matches:
				xDiff = kp_r[m[0].trainIdx].pt[0] - kp_l[m[0].queryIdx].pt[0]	#p2.x - p1.x
				yDiff = kp_r[m[0].trainIdx].pt[1] - kp_l[m[0].queryIdx].pt[1]	#p2.y - p1.y
				ang_tmp = math.degrees(math.atan2(yDiff,xDiff))
				if (mean + stddev) > (mean - stddev):
					if (mean + stddev) >= ang_tmp and (mean - stddev) <= ang_tmp:
						filtered_matches.append(m)
						mkp_l.append( kp_l[m[0].queryIdx] )
						mkp_r.append( kp_r[m[0].trainIdx] )
						ang.append(math.degrees(math.atan2(yDiff,xDiff)))
				else:
					if (mean + stddev) <= ang_tmp and (mean - stddev) >= ang_tmp:
						filtered_matches.append(m)
						mkp_l.append( kp_l[m[0].queryIdx] )
						mkp_r.append( kp_r[m[0].trainIdx] )
						ang.append(math.degrees(math.atan2(yDiff,xDiff)))
			
			##print np.median(ang)
			mean = np.mean(ang)
			differences = [(value - mean)**2 for value in ang]
			stddev = np.mean(differences) ** 0.5
			#print mean
			#print stddev
			if (abs(ang_stddev) - abs(stddev)) < 0.001:
				break
		
		print "Filtered matches cheat: " + str(len(filtered_matches))
		
		mkp_pairs = zip(mkp_l, mkp_r)
		file = open(inputArgs.name + "_kp.txt", "w")
		for p in mkp_pairs:
			# left x , left y ; right x , right y
			file.write(str(p[0].pt[0]) + "," + str(p[0].pt[1]) + ";" + str(p[1].pt[0]) + "," + str(p[1].pt[1]) + "\n")
		file.close()
		
		# visualization
		if inputArgs.debug == 1:
			# draw lines
			for m in filtered_matches:
				cv2.line(img_tmp, (int(round(kp_l[m[0].queryIdx].pt[0])), int(round(kp_l[m[0].queryIdx].pt[1]))), (int(w_l + round(kp_r[m[0].trainIdx].pt[0])), int(round(kp_r[m[0].trainIdx].pt[1]))), (0, 255, 0), 1)	
		
			cv2.imwrite(inputArgs.name + "_features.jpg", img_tmp)
		
if __name__ == "__main__":			# this is not a module
	
	parseInput()	# what do we have to do
	
	processInput()	# doing what we have to do
	
	print ""		# for estetic output
