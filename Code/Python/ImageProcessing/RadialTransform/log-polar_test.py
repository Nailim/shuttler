import sys
import cv
import math
import numpy
from math import ceil, exp, pi
from math import log, cos, sin

from numpy import array, zeros

if __name__ == "__main__":			# this is not a module
	#cv_img_input = cv.LoadImage("3_big.jpg", cv.CV_LOAD_IMAGE_GRAYSCALE)
	cv_img_input = cv.LoadImage("1_small.jpg", cv.CV_LOAD_IMAGE_GRAYSCALE)
	#cv_img_input = cv.LoadImage("7.png", cv.CV_LOAD_IMAGE_GRAYSCALE)
	
	#cv_img_width = cv.GetSize(cv_img_input)[0]
	#cv_img_height = cv.GetSize(cv_img_input)[1]
	
	#center = [1654, 1216]
	#outsideR = 646
	#insideR = 1#320
	
	center = [325, 239]
	outsideR = 129
	insideR = 1#63
	
	#center = [128, 128]
	#outsideR = 128
	#insideR = 1#10
	
	cv.SetImageROI(cv_img_input,((center[0] - outsideR), (center[1] - outsideR),(outsideR*2),(outsideR*2)))
	cv.SaveImage("out__3_small_roi.jpg", cv_img_input)
	
	cv_img_width = cv.GetSize(cv_img_input)[0]
	cv_img_height = cv.GetSize(cv_img_input)[1]
		
	#i_n = cv_img_width
	#j_n = cv_img_height

	i_0 = cv_img_width / 2
	j_0 = cv_img_height / 2
	
	print "\n F:"
	#print exp( ( log(outsideR)/outsideR )/p_n )/ d_c
	
	#fx = math.sqrt( ( math.exp( (math.log(129)/129)/129 ) )/(1000*129) )
	#fx = math.sqrt( ( exp( (log(outsideR)/(outsideR-insideR+1))/outsideR ) )/(1000*((outsideR*outsideR)**0.5)) )
	fx = math.sqrt( ( exp( (math.log(outsideR)/outsideR)/outsideR ) )/(1000*outsideR) )
	
	print fx
	
	#i_c = max(i_0, i_n - i_0)
	#j_c = max(j_0, j_n - j_0)
	#d_c = ((i_c ** 2 + j_c ** 2) ** 0.5)
	d_c = (((cv_img_width / 2)**2 + 0*(cv_img_height / 2)**2)**0.5)*3.3#*2.35197#2.34#(fx*1000)*1# * 1.63
	
#	print ""
#	print i_c
#	print j_c
	print d_c
#	
	p_n = int(ceil(d_c))
	print p_n
	ri = ceil(log(insideR) / (log(d_c)/p_n))
	print ri
	ro = ceil(log(outsideR) / (log(d_c)/p_n))
	print ro
	print ro - ri
	riro = int(ro - ri)
	t_n = int(cv_img_width * 1)# * 1.63)
	
	print "\nDebug:"
	print log(outsideR)
	print log(d_c)/p_n
	print log(outsideR) / (log(d_c)/p_n)
	print ceil(log(outsideR) / (log(d_c)/p_n))
	#t_n = (int)(math.pi * (pow(outsideR,2) - pow(insideR,2))) / riro
#	#t_n = (int)(2*math.pi*outsideR)
	
#	# The scale factors determine the size of each "step" along the transform.
	p_s = log(d_c) / p_n 	# p
	t_s = 2.0 * pi / (t_n)	# fi

#	
#	# create output image
	transformed = cv.CreateImage((t_n, riro),cv_img_input.depth,cv_img_input.nChannels)
#	print ""
#	print transformed
#	
	for p in range(0, p_n):
		p_exp = exp((p+ri) * p_s)
		if p_exp >= insideR and p_exp <= outsideR:
			for t in range(0, t_n):
			
				t_rad = t * t_s
				i = int(i_0 + p_exp * sin(t_rad))
				j = int(j_0 + p_exp * cos(t_rad))
			
			
				if 0 <= i < cv_img_width and 0 <= j < cv_img_height:
	#				print "ok"
					transformed[p, t_n-1-t] = cv_img_input[i, j]
		#else :
		#	print "%f %f" %(p, p_exp)
#			
	cv.SaveImage("out__7.jpg", transformed)
#	print ""
#	print transformed
	
