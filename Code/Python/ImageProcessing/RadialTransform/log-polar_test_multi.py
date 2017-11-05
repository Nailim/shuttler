import sys
import cv
import math
import numpy
from math import ceil, exp, pi
from math import log, cos, sin

from numpy import array, zeros

if __name__ == "__main__":			# this is not a module
	
	img_src = ["8_01.jpg", "8_02.jpg", "8_03.jpg", "8_04.jpg", "8_05.jpg"]
	
	img_center = [[1654, 1216], [1042, 767], [652, 361], [409, 240], [327, 241]]
	img_outsideR = [652, 417, 261, 163, 132]
	img_insideR = [320, 200, 128, 80, 66]
	
	img_cal_x = [37.0, 23.0, 15.0, 9.0, 7.0]
	img_cal_y = [12.5, 8.5, 6.5, 4.0, 3.0]
	img_cal_f = [3.5, 3.15, 2.9, 2.7, 2.65]
	
	img_cal_f_p = [-6.2062e-07, 2.1236e-03, 2.3781]
	
	for im in range(0,5) :
		print ""
		print img_src[im]
		
		cv_img_input = cv.LoadImage(img_src[im], cv.CV_LOAD_IMAGE_GRAYSCALE)
		
		cv.SetImageROI(cv_img_input,((img_center[im][0] - img_outsideR[im]), (img_center[im][1] - img_outsideR[im]),(img_outsideR[im]*2),(img_outsideR[im]*2)))
		cv.SaveImage("out_roi_"+img_src[im], cv_img_input)
		
		cv_img_width = cv.GetSize(cv_img_input)[0]
		cv_img_height = cv.GetSize(cv_img_input)[1]
		
		i_0 = cv_img_width / 2
		j_0 = cv_img_height / 2
		
		# magic schrooms
		#d_c = (((cv_img_width / 2)**2 + 0*(cv_img_height / 2)**2)**0.5) * (img_cal_f[im])
		
		# inteligent schrooms
		magic = img_cal_f_p[0]*(img_outsideR[im]**2) + img_cal_f_p[1]*img_outsideR[im] + img_cal_f_p[2]
		d_c = (((cv_img_width / 2)**2 + 0*(cv_img_height / 2)**2)**0.5) * magic
		
		p_n = int(ceil(d_c))
		ri = ceil(log(img_insideR[im]) / (log(d_c)/p_n))
		ro = ceil(log(img_outsideR[im]) / (log(d_c)/p_n))
		riro = int(ro - ri)
		t_n = int(cv_img_width * 1)
		p_s = log(d_c) / p_n 	# p
		t_s = 2.0 * pi / (t_n)	# fi
		
		print "i_0: %f" % i_0
		print "j_0: %f" % j_0
		print "d_c: %f" % d_c
		print "p_n: %f" % p_n
		print "riro: %f" % riro
		print "t_n: %f" % t_n
		print "p_s: %f" % p_s
		print "t_s: %f" % t_s
		print "magic: %f" % magic
		
		transformed = cv.CreateImage((t_n, riro),cv_img_input.depth,cv_img_input.nChannels)
		
		for p in range(0, p_n):
			p_exp = exp((p+ri) * p_s)
			
			if p_exp >= img_insideR[im] and p_exp <= img_outsideR[im]:
				for t in range(0, t_n):
				
					t_rad = t * t_s
					i = int(i_0 + p_exp * sin(t_rad))
					j = int(j_0 + p_exp * cos(t_rad))
					#print ""
					#print p
					#print t_n-1-t
					#print p_exp
					if 0 <= i < cv_img_width and 0 <= j < cv_img_height:
						if 0 <= (t_n-1-t) < t_n and 0 <= p < riro:
							transformed[p, t_n-1-t] = cv_img_input[i, j]			
		
		cv.SaveImage("out_"+img_src[im], transformed)
		
		
		
		
		
		
		
		
		
