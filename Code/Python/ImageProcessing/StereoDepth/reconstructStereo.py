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

from pylab import *

import homography
import camera
import sfm

global inputParser	# just a reminder, it's used as a global variable
global inputArgs	# just a reminder, it's used as a global variable

ply_header = '''ply
format ascii 1.0
element vertex %(vert_num)d
property float x
property float y
property float z
property uchar red
property uchar green
property uchar blue
end_header
'''

def parseInput() :
	
	global inputParser
	global inputArgs
	
	inputParser = argparse.ArgumentParser(description='Rectification of stereo images using epipolar geometry.')
	
	inputParser.add_argument('-l', '--left', dest='left', action='store', default="", type=str, help='left image')
	inputParser.add_argument('-r', '--right', dest='right', action='store', default="", type=str, help='right image')
	inputParser.add_argument('-lc', '--leftCalibration', dest='leftCalibration', action='store', default="", type=str, help='left image parameters')
	inputParser.add_argument('-rc', '--rightCalibration', dest='rightCalibration', action='store', default="", type=str, help='right image parameters')
	inputParser.add_argument('-nc', '--nameCalibration', dest='nameCalibration', action='store', default="fm_out", type=str, help='name of file with coresponding points for calibration (less but good)')
	inputParser.add_argument('-np', '--namePoints', dest='namePoints', action='store', default="fm_out", type=str, help='name of file with coresponding points for reconstruction (more but probably not good)')
	
	inputParser.add_argument('-d', '--debug', action='store_true', help='debug output')
	
	inputArgs = inputParser.parse_args()

def processInput() :
	print ""
	if inputArgs.left == "" or inputArgs.right == "":
		print "Missing images!"
		quit()
	
	# here we go ...
	
	# load image pair - we might need them for later
	img_l = cv2.imread(inputArgs.left)
	img_r = cv2.imread(inputArgs.right)
	
	if img_l == None or img_r == None:
		print "Missing images!"
		quit()
	
	
	### git them points - calibration
	mkp_l = []
	mkp_r = []
	
	file_kp = open(inputArgs.nameCalibration, 'r')
 	
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
	
	### git them points - reconstruction
	mkp_l_p = []
	mkp_r_p = []
	
	file_kp = open(inputArgs.namePoints, 'r')
 	
 	if file_kp == None:
		print "Missing matching points file"
		quit()
 	
 	for line in file_kp:
		l_r = line.split(';')		
		mkp_l_p.append([float(l_r[0].split(',')[0]), float(l_r[0].split(',')[1])])
		mkp_r_p.append([float(l_r[1].split(',')[0]), float(l_r[1].split(',')[1])])
	file_kp.close()
	
	mkp_l_p = np.float32(mkp_l_p)
	mkp_r_p = np.float32(mkp_r_p)
	
	### git them calibrations - left
	K_l = []
	file_c_l = open(inputArgs.leftCalibration, 'r')
 	
 	if file_c_l == None:
		print "Missing left calibration file"
		quit()
 	
 	for line in file_c_l:
		c_l = line.split(' ')
		K_l.append([float(c_l[0]), float(c_l[1]), float(c_l[2])])
	file_c_l.close()
	K_l = np.float32(K_l)
	
	### git them calibrations - right
	K_r = []
	file_c_r = open(inputArgs.rightCalibration, 'r')
 	
 	if file_c_r == None:
		print "Missing right calibration file"
		quit()
 	
 	for line in file_c_r:
		c_l = line.split(' ')
		K_r.append([float(c_l[0]), float(c_l[1]), float(c_l[2])])
	file_c_r.close()
	K_r = np.float32(K_r)
	
	
	### ok, now we start work
	
	# make homogeneous and normalize with inv(K)
	x1 = homography.make_homog(mkp_l_p.T)
	x2 = homography.make_homog(mkp_r_p.T)
	
	x1n = dot(inv(K_l),x1)
	x2n = dot(inv(K_r),x2)

	# compute E (E = (K_r)T * F * K_l)
	#F, mask = cv2.findFundamentalMat(mkp_l, mkp_r, cv2.FM_8POINT)
	F, mask = cv2.findFundamentalMat(mkp_l, mkp_r, cv2.FM_RANSAC, 1, 0.99)
	
	# we select only inlier points - most pf the time this makes it worse
	#mkp_l = mkp_l[mask.ravel()==1]
	#mkp_r = mkp_r[mask.ravel()==1]
	
	E = K_r.transpose()
	E = E.dot(F)
	E = E.dot(K_l)
	
	# compute camera matrices (P2 will be list of four solutions)
	P1 = array([[1,0,0,0],[0,1,0,0],[0,0,1,0]])
	P2 = sfm.compute_P_from_essential(E)

	# pick the solution with points in front of cameras
	ind = 0
	maxres = 0
	
	for i in range(4):
		# triangulate inliers and compute depth for each camera
		X = sfm.triangulate(x1n,x2n,P1,P2[i])
		d1 = dot(P1,X)[2]
		d2 = dot(P2[i],X)[2]
		if sum(d1>0)+sum(d2>0) > maxres:
			maxres = sum(d1>0)+sum(d2>0)
			ind = i
			infront = (d1>0) & (d2>0)

	# triangulate inliers and remove points not in front of both cameras
	X = sfm.triangulate(x1n,x2n,P1,P2[ind])
	X = X[:,infront]
	
	# visualization
	if inputArgs.debug == 1:
		# draw points
		img_tmp = img_l.copy()
		for kp in mkp_l_p:
			x = int(kp[0])
			y = int(kp[1])
			cv2.circle(img_tmp, (x, y), 3, (0, 0, 255))
		
		cv2.imwrite(inputArgs.namePoints + ".jpg", img_tmp)
	
	
	# 3D plot
	out_points = []
	out_colors = []
	
	for i in range(len(X[0])):
		out_points.append([X[0][i], X[1][i], X[2][i]])
		#out_colors.append(img_l[int(x1[1][i])][int(x1[0][i])])
		out_colors.append([img_l[int(x1[1][i])][int(x1[0][i])][2], img_l[int(x1[1][i])][int(x1[0][i])][1], img_l[int(x1[1][i])][int(x1[0][i])][0]])
		
	out_points = np.float32(out_points)
	out_colors = np.float32(out_colors)
	write_ply(inputArgs.namePoints + ".ply", out_points, out_colors)
	


def write_ply(fn, verts, colors):
    verts = verts.reshape(-1, 3)
    colors = colors.reshape(-1, 3)
    verts = np.hstack([verts, colors])
    with open(fn, 'w') as f:
        f.write(ply_header % dict(vert_num=len(verts)))
        np.savetxt(f, verts, '%f %f %f %d %d %d')

if __name__ == "__main__":			# this is not a module
	
	parseInput()	# what do we have to do
	
	processInput()	# doing what we have to do
	
	print ""		# for estetic output
