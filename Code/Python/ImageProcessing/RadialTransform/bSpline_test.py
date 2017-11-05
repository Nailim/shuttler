# this resizes __1.jpt to x it's original size & it turns it grayscale
import cv
import numpy

import bSpline

if __name__ == "__main__":			# this is not a module
	
	scale = 10
	
	# load image
	#cv_img = cv.LoadImage("__1.jpg", cv.CV_LOAD_IMAGE_GRAYSCALE) # CV_LOAD_IMAGE_GRAYSCALE
	cv_img = cv.LoadImage("__1.jpg", cv.CV_LOAD_IMAGE_UNCHANGED) # CV_LOAD_IMAGE_UNCHANGED
	
	# width & height
	cv_img_width = cv.GetSize(cv_img)[0]
	cv_img_height = cv.GetSize(cv_img)[1]
	
	img_tpl = numpy.zeros( ((cv_img_height * scale),(cv_img_width * scale),2) )
	
	for h in range(0,(cv_img_height * scale),1) :
		for w in range(0,(cv_img_width * scale),1) :
			img_tpl[h][w][0] = (h + 0) / (cv_img_height * scale * 1.0) * cv_img_height
			img_tpl[h][w][1] = (w + 0) / (cv_img_width * scale * 1.0) * cv_img_width
	
	##bSpl = bSpline.BSpline()	# v4.0
	
	# single picture
	##cv_img_out = bSpl.cubic(cv_img, img_tpl)	# v4.0
	#cv_img_out = bSpline.cubic(cv_img, img_tpl)
	#cv.SaveImage("out__1.jpg", cv_img_out)
	
	# multiple pictures
	img_beta_f = bSpline.cubic_getBeta(cv_img, img_tpl)
	
	cv_img_out = bSpline.cubic_setBeta(cv_img, img_tpl, img_beta_f)
	cv.SaveImage("out__1.01.jpg", cv_img_out)
	#cv_img_out = bSpl.cubic_setBeta(cv_img, img_tpl, img_beta_f)
	#cv.SaveImage("out__1.02.jpg", cv_img_out)
	#cv_img_out = bSpl.cubic_setBeta(cv_img, img_tpl, img_beta_f)
	#cv.SaveImage("out__1.03.jpg", cv_img_out)
