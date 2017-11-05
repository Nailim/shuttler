# for new opencv
#import os,sys
#os.chdir(os.path.expanduser('~/opencv-2.4.6.1/lib'))
#sys.path.append(os.path.expanduser('~/opencv-2.4.6.1/lib/python2.7/dist-packages'))

# before starting
#export PYTHONPATH=~/opencv-2.4.6.1/lib/python2.7/dist-packages

import os
#import cv
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
	
	# we like them gray
	gray_l = cv2.cvtColor(img_l, cv2.COLOR_BGR2GRAY)
	gray_r = cv2.cvtColor(img_r, cv2.COLOR_BGR2GRAY)
	
	mkp_l = []
	mkp_r = []
	
	# git them points
	file_kp = open(inputArgs.name + '_kp.txt', 'r')
 	
 	if file_kp == None:
		print "Missing matching points file"
		quit()
 	
 	for line in file_kp:
		l_r = line.split(';')
		mkp_l.append([float(l_r[0].split(',')[0]), float(l_r[0].split(',')[1])])
		mkp_r.append([float(l_r[1].split(',')[0]), float(l_r[1].split(',')[1])])
	file_kp.close()
	
	mkp_l = np.float32(mkp_l)
	mkp_r = np.float32(mkp_r)
	#F, mask = cv2.findFundamentalMat(mkp_l, mkp_r, cv2.FM_8POINT)
	F, mask = cv2.findFundamentalMat(mkp_l, mkp_r, cv2.FM_RANSAC, 1, 0.99)
	
	# we select only inlier points - most pf the time this makes it worse
	#mkp_l = mkp_l[mask.ravel()==1]
	#mkp_r = mkp_r[mask.ravel()==1]
	
	colorList = getColor(100)
	
	# find epilines corresponding to points in right image (second image) and drawing its lines on left image
	lines_l = cv2.computeCorrespondEpilines(mkp_r.reshape(-1,1,2), 2, F)
	lines_l = lines_l.reshape(-1, 3)
	img_l_line, img_l_point = drawlines(gray_l ,gray_r ,lines_l, mkp_l, mkp_r, colorList, 100)

	# find epilines corresponding to points in left image (first image) and drawing its lines on right image
	lines_r = cv2.computeCorrespondEpilines(mkp_l.reshape(-1,1,2), 1, F)
	lines_r = lines_r.reshape(-1, 3)
	img_r_line, img_r_point = drawlines(gray_r, gray_l, lines_r, mkp_r, mkp_l, colorList, 100)
		
	# visualization
	if inputArgs.debug == 1:
		#for i in range(len(lines_l)):
		#	print i
		#	img_l_line, img_l_point = drawlinesOne(gray_l ,gray_r ,lines_l, mkp_l, mkp_r, colorList, 100, i)
		# !!!
		# merge image side by side - lines
		h_l, w_l = img_l_line.shape[:2]
		h_r, w_r = img_r_line.shape[:2]
		img_tmp = np.zeros((max(h_l, h_l), w_r+w_r, 3), np.uint8)
		img_tmp[:h_l, :w_l] = img_l_line
		img_tmp[:h_r, w_l:w_l+w_r] = img_r_line
	
		cv2.imwrite(inputArgs.name + "_epi_lines.jpg", img_tmp)
		
		# merge image side by side - points
		h_l, w_l = img_l_point.shape[:2]
		h_r, w_r = img_r_point.shape[:2]
		img_tmp = np.zeros((max(h_l, h_l), w_r+w_r, 3), np.uint8)
		img_tmp[:h_l, :w_l] = img_l_point
		img_tmp[:h_r, w_l:w_l+w_r] = img_r_point
	
		cv2.imwrite(inputArgs.name + "_epi_points.jpg", img_tmp)
			
	
	r, c, n = img_l.shape
	retval, H1, H2 = cv2.stereoRectifyUncalibrated(mkp_l.reshape(1, -1, 2), mkp_r.reshape(1, -1, 2), F, (c, r))
	
	# visualization
	if inputArgs.debug == 1:
		# warp images
		#H, status = cv2.findHomography(mkp_l, mkp_r, cv2.RANSAC, 5.0)
		img_l_tmp = cv2.warpPerspective(img_l_line, H1, (c,r));
		img_r_tmp = cv2.warpPerspective(img_r_line, H2, (c,r));
		
		# merge image side by side
		h_l, w_l = img_l_tmp.shape[:2]
		h_r, w_r = img_r_tmp.shape[:2]
		img_tmp = np.zeros((max(h_l, h_l), w_r+w_r, 3), np.uint8)
		img_tmp[:h_l, :w_l] = img_l_tmp
		img_tmp[:h_r, w_l:w_l+w_r] = img_r_tmp
		
		cv2.imwrite(inputArgs.name + "_warped_lines.jpg", img_tmp)
	
	img_l_warped = cv2.warpPerspective(img_l, H1, (c,r));
	img_r_warped = cv2.warpPerspective(img_r, H2, (c,r));
	
	head, tail = os.path.split(inputArgs.left)
	cv2.imwrite(head+"/"+"warp_"+tail, img_l_warped)
	
	head, tail = os.path.split(inputArgs.right)
	cv2.imwrite(head+"/"+"warp_"+tail, img_r_warped)
	
def getColor(number):
	color = []
	
	for i in range(0, number):
		col = tuple(np.random.randint(0,255,3).tolist())
		color.append(col)
		
	return color

def drawlines(img_1, img_2, lines, pts_1, pts_2, color, nol):
	# img_1 - image on which we draw the epilines for the points in img_2
	# lines - corresponding epilines
	
	cnt = 0
	
	r,c = img_1.shape
	img_1_tmp = img_1.copy()
	img_2_tmp = img_2.copy()
	
	img_1_tmp = cv2.cvtColor(img_1_tmp, cv2.COLOR_GRAY2BGR)
	img_2_tmp = cv2.cvtColor(img_2_tmp, cv2.COLOR_GRAY2BGR)
	
	for r, pt_1, pt_2 in zip(lines, pts_1, pts_2):		
		x0, y0 = map(int, [0, -r[2]/r[1]])
		x1, y1 = map(int, [c, -(r[2]+r[0]*c)/r[1]])
		
		cv2.line(img_1_tmp, (x0, y0), (x1, y1), color[cnt], 1)
		cv2.circle(img_1_tmp,tuple(pt_1), 5, color[cnt], -1)
		cv2.circle(img_2_tmp,tuple(pt_2), 5, color[cnt], -1)
		
		cnt = cnt + 1
		if cnt == len(color):
			cnt = 0
		if cnt >= nol:
			break
		
	return img_1_tmp, img_2_tmp

if __name__ == "__main__":			# this is not a module
	
	parseInput()	# what do we have to do
	
	processInput()	# doing what we have to do
	
	print ""		# for estetic output
