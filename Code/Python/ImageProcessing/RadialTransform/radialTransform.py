import sys
import cv
import math
import numpy
#import time

import bSpline

import argparse

global inputParser	# just a reminder, it's used as a global variable
global inputArgs	# just a reminder, it's used as a global variable

#global imgTransMat

def parseInput() :
	
	global inputParser
	global inputArgs
	
	inputParser = argparse.ArgumentParser(description='Apply radial transformation to images.')
	
	inputParser.add_argument('image', nargs='+')
	
	inputParser.add_argument('-c', '--center', dest='center', action='store', default=0, type=int, nargs=2, help='annulus center in the image: x y')
	inputParser.add_argument('-o', '--offset', dest='offset', action='store', default=0, type=float, help='offset transformation starting angle: rad')
	inputParser.add_argument('-r', '--insideR', dest='insideR', action='store', default=1, type=int, help='annulus inside radius: pixels')
	inputParser.add_argument('-R', '--outsideR', dest='outsideR', action='store', default=10, type=int, help='annulus outside radius: pixels')
	inputParser.add_argument('-p', '--pProcess', dest='pProcess', action='store', default="lp2c", help='image processing: type (p2c, lp2c)')
	inputParser.add_argument('-pp', '--ppProcess', dest='ppProcess', action='store', default="bspl", help='image post processing: type (none, bspl)')
	inputParser.add_argument('-s', '--scale', dest='scale', action='store', default=1.0, type=float, help='output image scaling factor: x')
	#inputParser.add_argument('-a', '--ellipseA', dest='ellipseA', action='store', default=0, type=float, help='elipse parameter a for distortion correction')
	#inputParser.add_argument('-b', '--ellipseB', dest='ellipseB', action='store', default=0, type=float, help='elipse parameter b for distortion correction')
	inputParser.add_argument('-m', '--magic', dest='magic', action='store', default=3.49095094408, type=float, help='"magic" number for the lp2c function to get the correct ratio (has to be more then 1.0)')
	
	inputArgs = inputParser.parse_args()

def pix2x(pixel, diff, offset) :	# diff - diference in image size, offset - center offset
	#return ( (1.0 * 2 * pixel) / (1.0 * 2 * inputArgs.center[0]) ) - 1	# x = 2 * i / width -1 : width = 2 * center
	return ( (1.0 * 2 * (pixel - diff)) / (1.0 * 2 * (inputArgs.center[0] - offset - diff) ) ) - 1
	
def pix2y(pixel, diff, offset) :
	#return ( (1.0 * 2 * pixel) / (1.0 * 2 * inputArgs.center[1]) ) - 1	# y = 2 * j / height -1 : height = 2 * center
	return ( (1.0 * 2 * (pixel - diff)) / (1.0 * 2 * (inputArgs.center[1] - offset - diff) ) ) - 1
	
def x2pix(x, diff, offset) :
	#return (int)(math.ceil((x + 1) * (1.0 * 2 * inputArgs.center[0]) / 2 ))	# i = (x + 1) * width / 2 : width = 2 * center
	#return (int)(math.ceil((x + 1) * (1.0 * 2 * (inputArgs.center[0] - offset - diff)) / 2 ) + diff)
	return ((x + 1) * (1.0 * 2 * (inputArgs.center[0] - offset - diff)) / 2 + diff)

def y2pix(y, diff, offset) :
	#return (int)(math.ceil((y + 1) * (1.0 * 2 * inputArgs.center[1]) / 2 )) # j = (x + 1) * height / 2 : height = 2 * center
	#return (int)(math.ceil((y + 1) * (1.0 * 2 * (inputArgs.center[1] - offset - diff)) / 2 ) + diff)
	return ((y + 1) * (1.0 * 2 * (inputArgs.center[1] - offset - diff)) / 2 + diff)
	
def getR(x, y) :
	return math.sqrt(math.pow(x, 2) + math.pow(y, 2))	# r = sqrt(x^2 + y^2)
	
def getPHI(x, y) :
	return math.atan2(y, x)	# phi = atan2(y, x)

def getX(r, phi) :
	#return (r - inputArgs.ellipseA) * math.cos(phi)	# x = cos(phi) * (r + a); circle a == b == 1
	return r * math.cos(phi)	# x = cos(phi) * (r + a); circle a == b == 1
	
def getY(r, phi) :
	#return (r + inputArgs.ellipseB) * math.sin(phi)	# y = sin(phi) * (r + b); circle a == b == 1
	return r * math.sin(phi)	# y = sin(phi) * (r + b); circle a == b == 1
	
def processInput() :
	# calculate image transformation template
	if inputArgs.pProcess == "lp2c" : 
		#log-polar to cartasian (lp2c)
		print "Calculating log-polar to cartasian image translation template ..."
		(img_tpl, cv_img_roi) = translateImage_logPolar2cartasian(inputArgs.image[0])	# use the first picture for calculations
	else :
		# default, polar ro cartasin (p2c)
		print "Calculating polar to cartasian image translation template ..."
		(img_tpl, cv_img_roi) = translateImage_polar2cartasian(inputArgs.image[0])	# use the first picture for calculations
	
	img_beta_f = []	# for bspline, if used
	
	if inputArgs.ppProcess == "bspl" :
		# calculate b-spline beta function
		print "Calculating B-spline beta function template ..."
		#bSpl = bSpline.BSpline()
		#img_beta_f = bSpl.cubic_getBeta(cv_img_roi, img_tpl)
		img_beta_f = bSpline.cubic_getBeta(cv_img_roi, img_tpl)
	else :
		# default, none
		pass
	
	for img in range(0,len(inputArgs.image),1) :
		progressbar(((img*1.0+1)/len(inputArgs.image)), "Transforming:", "(" + str(img+1) + "/" + str(len(inputArgs.image)) + ") " + inputArgs.image[img], 20)
		transformImage(img, img_tpl, img_beta_f)
	
def transformImage(img, img_tpl, img_beta_f = None) :	# img - index of inputArgs.image[]
	# load image
	cv_img_input = cv.LoadImage(inputArgs.image[img], cv.CV_LOAD_IMAGE_COLOR)
	
	# select region of interest on image (for faster computation)
	cv.SetImageROI(cv_img_input,((inputArgs.center[0] - inputArgs.outsideR), (inputArgs.center[1] - inputArgs.outsideR),(inputArgs.outsideR*2),(inputArgs.outsideR*2)))
	
	# let's work with a smaller image
	cv_img_roi = cv.CreateImage(((inputArgs.outsideR*2), (inputArgs.outsideR*2)),cv.IPL_DEPTH_8U,3)
	cv.Copy(cv_img_input, cv_img_roi)
	
	# calculate the size of the output image
	out_y = img_tpl.shape[0]
	out_x = img_tpl.shape[1]
	
	# create the output image
	cv_img_out = cv.CreateImage((out_x, out_y), cv.IPL_DEPTH_8U,3)
	
	if inputArgs.ppProcess == "bspl" :
		# transform using b-splines (bspl)
		#bSpl = bSpline.BSpline()
		#cv_img_out = bSpl.cubic_setBeta(cv_img_roi, img_tpl, img_beta_f)
		cv_img_out = bSpline.cubic_setBeta(cv_img_roi, img_tpl, img_beta_f)
	else :
		# none, pixel for pixel copy is just fine (none)
		for x in range(0,out_x) :
			for y in range(0,out_y) :
				cv_img_out[y,x] = cv_img_input[(int)(img_tpl[y][x][0]), (int)(img_tpl[y][x][1])]
		pass
	# save image
	cv.SaveImage("out_%s_%s_%s" % (inputArgs.image[img], inputArgs.pProcess, inputArgs.ppProcess), cv_img_out)
	pass

def translateImage_logPolar2cartasian(img) :
	# load image
	cv_img_input = cv.LoadImage(img, cv.CV_LOAD_IMAGE_COLOR)
	
	# select region of interest on image (for faster computation)
	cv.SetImageROI(cv_img_input,((inputArgs.center[0] - inputArgs.outsideR), (inputArgs.center[1] - inputArgs.outsideR), (inputArgs.outsideR*2), (inputArgs.outsideR*2)))
	
	# let's work with a smaller image
	cv_img_roi = cv.CreateImage(((inputArgs.outsideR*2), (inputArgs.outsideR*2)), cv.IPL_DEPTH_8U, 3)
	cv.Copy(cv_img_input, cv_img_roi)
	
	cv_img_width = cv.GetSize(cv_img_input)[0]
	cv_img_height = cv.GetSize(cv_img_input)[1]
	
	# !!! uncomment if you want to see the whole picture
	#inputArgs.insideR = 1
	
	cv_img_width_half = cv_img_width / 2
	cv_img_height_half = cv_img_height / 2
	
	# calibration factors to fix the ratio
	#img_cal_f_p = [-6.2062e-07, 2.1236e-03, 2.3781]
	#magic = img_cal_f_p[0]*(inputArgs.outsideR**2) + img_cal_f_p[1]*inputArgs.outsideR + img_cal_f_p[2]
	magic = inputArgs.magic
	# distance from the transform's focus to the image's farthest corner - here we cheat and reduce it to r (by multiplying one side with 0)
	# this is used to calculate the iteration step across the transform's dimension
	img_dist = (((cv_img_width / 2)**2 + 0*(cv_img_height / 2)**2)**0.5) * magic * inputArgs.scale
	
	img_len = int(math.ceil(img_dist))
	
	# recalculated inside r length
	ri = math.ceil(math.log(inputArgs.insideR) / (math.log(img_dist)/img_len))	# math.ceil(math.log(inputArgs.insideR) / p_scale
	
	# recalculated outside r length
	ro = math.ceil(math.log(inputArgs.outsideR) / (math.log(img_dist)/img_len))	# math.ceil(math.log(inputArgs.outsideR) / p_scale
	
	# recalculated length of usable image - hight
	ri_ro = int(ro - ri)
	
	img_width = int(cv_img_height * inputArgs.scale)

	# scale factor determines the size of each "step" along the transformation
	p_scale = math.log(img_dist) / img_len	# p
	fi_scale = (2.0 * math.pi) / img_width	# fi
	
	
	# create output image
	transformed = cv.CreateImage((img_width, ri_ro),cv_img_input.depth,cv_img_input.nChannels)
	
	# create translation template
	img_tpl = numpy.zeros( (ri_ro, img_width, 2) )
	
	# transformation
	for p in range(0, img_len):
		p_exp = math.exp((p+ri) * p_scale)
		if p_exp >= inputArgs.insideR and p_exp <= inputArgs.outsideR:
			for t in range(0, img_width):
			
				t_rad = t * fi_scale
				i = (cv_img_width_half + p_exp * math.sin(t_rad + inputArgs.offset))
				j = (cv_img_height_half + p_exp * math.cos(t_rad + inputArgs.offset))
				
				if 0 <= i < cv_img_width and 0 <= j < cv_img_height:
					img_tpl[p, img_width-1-t][0] = i
					img_tpl[p, img_width-1-t][1] = j
	
	return (img_tpl, cv_img_roi)

def translateImage_polar2cartasian(img) :
	# load image
	cv_img_input = cv.LoadImage(img, cv.CV_LOAD_IMAGE_COLOR)
	
	# calculate the size of the output image
	out_y = (int)(inputArgs.outsideR - inputArgs.insideR)
	#out_y = (int)(((inputArgs.outsideR - inputArgs.insideR) ** 2 + (inputArgs.outsideR - inputArgs.insideR) ** 2) ** 0.5)
	
	out_x = (int)(math.pi * (pow(inputArgs.outsideR,2) - pow(inputArgs.insideR,2))) / out_y #area of annulus / hight of the image == width of the image
	#out_x = (int)(2*math.pi*inputArgs.outsideR)	# circumference of the outer circle
	#out_x = (int)(2*math.pi*inputArgs.insideR)		# circumference of the inner circle
	#out_x = (int)(1.75*math.pi*inputArgs.insideR)	# almost full circumference of the inner circle	
	
	out_x = (int)(out_x * inputArgs.scale)
	out_y = (int)(out_y * inputArgs.scale)
	
	# create translation template
	img_tpl = numpy.zeros( (out_y, out_x,2) )
	
	# select region of interest on image (for faster computation)
	cv.SetImageROI(cv_img_input,((inputArgs.center[0] - inputArgs.outsideR), (inputArgs.center[1] - inputArgs.outsideR),(inputArgs.outsideR*2),(inputArgs.outsideR*2)))
	
	# let's work with a smaller image
	center_x = inputArgs.center[0]
	center_y = inputArgs.center[1]
	inputArgs.center[0] = inputArgs.outsideR
	cv_img_roi = cv.CreateImage(((inputArgs.outsideR*2), (inputArgs.outsideR*2)),cv.IPL_DEPTH_8U,3)
	cv.Copy(cv_img_input, cv_img_roi)
	
	# we are not where we were in a smaller image
	inputArgs.center[1] = inputArgs.outsideR
	
	# calculate offsets
	cv_img_width = cv.GetSize(cv_img_roi)[0]
	cv_img_height = cv.GetSize(cv_img_roi)[1]
	
	cv_img_width_diff = 0
	cv_img_height_diff = 0
	
	if cv_img_width != cv_img_height :
		if cv_img_width > cv_img_height :
			cv_img_width_diff = (cv_img_width - cv_img_height) / 2
			if (cv_img_width_diff % 2) != 0 :
				cv_img_width_diff-1
			
		else :
			cv_img_height_diff = (cv_img_height - cv_img_width) / 2
			if (cv_img_height_diff % 2) != 0 :
				cv_img_height_diff-1
	
	cv_img_center_width_diff = 0
	cv_img_center_height_diff = 0
	
	cv_img_center_width_diff = inputArgs.center[0] - (cv_img_width / 2)
	cv_img_center_height_diff = inputArgs.center[1] - (cv_img_height / 2)
	
	# calculate stuff
	r_min = getR(pix2x(inputArgs.center[0] - cv_img_center_width_diff + inputArgs.insideR, cv_img_width_diff, cv_img_center_width_diff), pix2y(inputArgs.center[1] - cv_img_center_height_diff, cv_img_height_diff, cv_img_center_height_diff))
	r_max = getR(pix2x(inputArgs.center[0] - cv_img_center_width_diff + inputArgs.outsideR, cv_img_width_diff, cv_img_center_width_diff), pix2y(inputArgs.center[1] - cv_img_center_height_diff, cv_img_height_diff, cv_img_center_height_diff))
	
	r_dif_count = (int)((inputArgs.outsideR - inputArgs.insideR) * inputArgs.scale)
	r_dif = ((r_max - r_min) / r_dif_count)
	
	phi_dif = (2 * math.pi) / out_x	# 2 * pi / width
	
	for p in range(0,out_x,1) :
		phi = phi_dif * p + inputArgs.offset
		for r in range(0, r_dif_count, 1) :
			radius = r_min + (r * r_dif)
			img_tpl[r,out_x-1-p][0] = y2pix(getY(radius, phi),cv_img_height_diff,cv_img_center_height_diff) + cv_img_center_height_diff
			img_tpl[r,out_x-1-p][1] = x2pix(getX(radius, phi),cv_img_width_diff, cv_img_center_width_diff) + cv_img_center_width_diff
	
	# lets go back where we were
	inputArgs.center[0] = center_x
	inputArgs.center[1] = center_y
	
	return (img_tpl, cv_img_roi)	
    	
def progressbar(progress, prefix = "", postfix = "", size = 60) :
	x = int(size*progress)
	sys.stdout.write("%s [%s%s] %d%% %s\r" % (prefix, "#"*x, "."*(size-x), (int)(progress*100), postfix))
	sys.stdout.flush()
	
	#time.sleep(1.0) # long computation
    
if __name__ == "__main__":			# this is not a module
	
	parseInput()	# what do we have to do
	
	processInput()	# doing what we have to do
	
	print ""		# for estetic output
	
