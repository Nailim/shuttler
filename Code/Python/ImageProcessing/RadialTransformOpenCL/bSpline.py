import cv
import numpy
import ctypes
import multiprocessing
from multiprocessing import Pool

import pyopencl as cl
import numpy
import numpy.linalg as la

import warnings

# global variables for shared memory
img_gray_edge = []
img_r_edge = []
img_g_edge = []
img_b_edge = []

warnings.simplefilter("ignore", RuntimeWarning)	# python 2.7 woes with ctype



# worker declaration for single channel processing - getBeta
def cubic_getBetaFunction((x, y, w, h)) :
	# setting coefficients
	n = 3	# cubic B-spline
	K = 4	# K = n + 1
	
	k1 = (numpy.floor(x) - ((n+1) / 2))
	l1 = (numpy.floor(y) - ((n+1) / 2))
	
	beta3X = numpy.zeros([5,5])
	beta3Y = numpy.zeros([5,5])
	
	beta3_x = 0
	beta3_y = 0
	
	for k in numpy.arange(k1,(k1+K+1),1) :		# + 1 -> for k < k1+K
		for l in numpy.arange(l1,(l1+K+1),1) :	# + 1 -> for l < l1+K				
			#beta3X.itemset((beta3_x,beta3_y), cubic_beta3(x - k))				
			beta3_f = numpy.abs(x - k)
			if beta3_f >= 2.0 :
				# = 0
				beta3X.itemset((beta3_x,beta3_y),  0.0)
			elif (beta3_f >= 1.0) and (beta3_f < 2.0) :
				# = (numpy.power((2.0 - pix),3) / 6.0) 
				beta3X.itemset((beta3_x,beta3_y), (((2.0-beta3_f)*(2.0-beta3_f)*(2.0-beta3_f)) / 6.0)) 
			else :
				# = ((2.0/3.0) - numpy.power(pix,2) + (numpy.power(pix,3) / 2.0))
				beta3X.itemset((beta3_x,beta3_y), ((2.0/3.0) - (beta3_f*beta3_f) + ((beta3_f*beta3_f*beta3_f) / 2.0)))
			
			#beta3Y.itemset((beta3_x,beta3_y), cubic_beta3(y - l))
			beta3_f = numpy.abs(y - l)
			if beta3_f >= 2.0 :
				# = 0
				beta3Y.itemset((beta3_x,beta3_y),  0.0)
			elif (beta3_f >= 1.0) and (beta3_f < 2.0) :
				# = (numpy.power((2.0 - pix),3) / 6.0) 
				beta3Y.itemset((beta3_x,beta3_y), (((2.0-beta3_f)*(2.0-beta3_f)*(2.0-beta3_f)) / 6.0)) 
			else :
				# = ((2.0/3.0) - numpy.power(pix,2) + (numpy.power(pix,3) / 2.0))
				beta3Y.itemset((beta3_x,beta3_y), ((2.0/3.0) - (beta3_f*beta3_f) + ((beta3_f*beta3_f*beta3_f) / 2.0)))
			
			beta3_y += 1

		beta3_x += 1
		beta3_y = 0
	
	beta3_mul = numpy.multiply(beta3X, beta3Y).astype(numpy.float32)
	
	return (beta3_mul.transpose(), w, h) # need to trsnspose for later, so we don't do it for every pixel

# worker declaration for single channel processing
def cubic_setBeta_singleChannel((x, y, w, h, beta3), def_param=(img_gray_edge)) :
#def cubic_setBeta_singleChannel((b, x, y, w, h, beta3)) :
	# setting coefficients
	n = 3	# cubic B-spline
	K = 4	# K = n + 1
	
	k1 = (numpy.floor(x) - ((n+1) / 2))
	l1 = (numpy.floor(y) - ((n+1) / 2))
	
	ctrlP = numpy.asarray(img_gray_edge[l1+2:l1+7,k1+2:k1+7])
	
	pix_sum = numpy.multiply(beta3, ctrlP) # need to transpose, somewhere in the chain it gets flipped
	pix_value = pix_sum.sum()
	
	return (pix_value, w, h)

# worker declaration for single multi processing	
def cubic_setBeta_multiChannel((x, y, w, h, beta3), def_param=(img_r_edge,img_g_edge,img_b_edge)) :
#def cubic_setBeta_multiChannel((b_r, b_g, b_b, x, y, w, h, beta3)) :
	# setting coefficients
	n = 3	# cubic B-spline
	K = 4	# K = n + 1
	
	k1 = (numpy.floor(x) - ((n+1) / 2))
	l1 = (numpy.floor(y) - ((n+1) / 2))
	
	ctrlP_R = numpy.asarray(img_r_edge[l1+2:l1+7,k1+2:k1+7])
	ctrlP_G = numpy.asarray(img_g_edge[l1+2:l1+7,k1+2:k1+7])
	ctrlP_B = numpy.asarray(img_b_edge[l1+2:l1+7,k1+2:k1+7])	
	
	pix_value = (numpy.multiply(beta3, ctrlP_R).sum().astype(numpy.float32), numpy.multiply(beta3, ctrlP_G).sum().astype(numpy.float32), numpy.multiply(beta3, ctrlP_B).sum().astype(numpy.float32))
	
	return (pix_value, w, h)

def cubic_getBeta(cv_img, img_tpl) :		
	#!print "set_getBeta"				
	# arguments for individual process (might use a lot of memorry)
	worker_arguments = []
	
	# pool of process workes
	pool = Pool(processes = multiprocessing.cpu_count())
	
	for h in range(0,img_tpl.shape[1],1) :
		for w in range(0,img_tpl.shape[0],1) :
			#cv_img_out[w,h] = cubic_f_singleChannel(img_gray_edge, img_tpl[w][h][1], img_tpl[w][h][0])
			worker_arguments.append((img_tpl[w][h][1], img_tpl[w][h][0], w, h))
	
	# result is already a list, just return it
	img_beta_f = pool.map(cubic_getBetaFunction, worker_arguments)
	
	pool.close()
	pool.join()
	
	return img_beta_f

def cubic_setBeta(cv_img, img_tpl, img_beta_f, inY = None, inX = None, inW = None, inH = None, inBetaMatrika = None) :
	# linking global variables
	global img_gray_edge
	global img_r_edge
	global img_g_edge
	global img_b_edge
	
	#!print "setBeta"

	# image properties
	img_ch = cv_img.nChannels
	img_dpth = cv_img.depth
	img_width = cv.GetSize(cv_img)[0]
	img_height = cv.GetSize(cv_img)[1]
		
	# create output image
	cv_img_out = cv.CreateImage((img_tpl.shape[1], img_tpl.shape[0]),cv_img.depth,cv_img.nChannels) # img_tpl.shape[0] - width, img_tpl.shape[1] - height
	#!print "bigassifelse"

	# decompose image to channel arrays
	if img_ch == 1 :
		# decompose channels
		cv_img_gray = cv.CreateImage(cv.GetSize(cv_img),cv_img.depth,1)
		cv.Split(cv_img, cv_img_gray, None, None, None)
		# convert to array
		img_gray = numpy.asarray(cv_img_gray[:,:])
		# extend image for edge pixel computation
		#img_gray_edge = numpy.zeros([(img_height+4), (img_width+4)])
		
		img_gray_edge_base = multiprocessing.Array(ctypes.c_float, (img_height+4)*(img_width+4))
		img_gray_edge = numpy.ctypeslib.as_array(img_gray_edge_base.get_obj())
		img_gray_edge = img_gray_edge.reshape((img_height+4), (img_width+4))
		assert img_gray_edge.base.base is img_gray_edge_base.get_obj()
		
		img_gray_edge[2:(img_height+2),2:(img_width+2)] = img_gray[:,:]
		# compute edge pixel weight - gray
		img_gray_edge[1,2:(img_width+2)] = ((2 * img_gray_edge[2,2:(img_width+2)]) - img_gray_edge[3,2:(img_width+2)]) # upper edge
		img_gray_edge[(img_height+2),2:(img_width+2)] = ((2 * img_gray_edge[(img_height+1),2:(img_width+2)]) - img_gray_edge[(img_height),2:(img_width+2)]) # bottom edge
		img_gray_edge[1:(img_height+3):,1] = (2 * img_gray_edge[1:(img_height+3):,2]) - img_gray_edge[1:(img_height+3):,3] # left edge
		img_gray_edge[1:(img_height+3):,(img_width+2)] = (2 * img_gray_edge[1:(img_height+3):,(img_width+1)]) - img_gray_edge[1:(img_height+3):,(img_width+0)] # right edge
		img_gray_edge[0,1:(img_width+3)] = ((2 * img_gray_edge[1,1:(img_width+3)]) - img_gray_edge[2,1:(img_width+3)]) # upper edge x 2
		img_gray_edge[(img_height+3),1:(img_width+3)] = ((2 * img_gray_edge[(img_height+2),1:(img_width+3)]) - img_gray_edge[(img_height+1),1:(img_width+3)]) # bottom edge x 2
		img_gray_edge[0:(img_height+4):,0] = (2 * img_gray_edge[0:(img_height+4):,1]) - img_gray_edge[0:(img_height+4):,2] # left edge x 2
		img_gray_edge[0:(img_height+4):,(img_width+3)] = (2 * img_gray_edge[0:(img_height+4):,(img_width+2)]) - img_gray_edge[0:(img_height+4):,(img_width+1)] # right edge x 2							
	else :
		# decompose channels
		cv_img_r = cv.CreateImage(cv.GetSize(cv_img),cv_img.depth,1)
		cv_img_g = cv.CreateImage(cv.GetSize(cv_img),cv_img.depth,1)
		cv_img_b = cv.CreateImage(cv.GetSize(cv_img),cv_img.depth,1)
		cv.Split(cv_img, cv_img_r, cv_img_g, cv_img_b, None)
		# convert to array
		img_r = numpy.asarray(cv_img_r[:,:])
		img_g = numpy.asarray(cv_img_g[:,:])
		img_b = numpy.asarray(cv_img_b[:,:])
		# extend image for edge pixel computation - red
		#img_r_edge = numpy.zeros([(img_height+4), (img_width+4)])
		
		img_r_edge_base = multiprocessing.Array(ctypes.c_float, (img_height+4)*(img_width+4))
		img_r_edge = numpy.ctypeslib.as_array(img_r_edge_base.get_obj())
		img_r_edge = img_r_edge.reshape((img_height+4), (img_width+4))
		assert img_r_edge.base.base is img_r_edge_base.get_obj()
		
		img_r_edge[2:(img_height+2),2:(img_width+2)] = img_r[:,:]
		# compute edge pixel weight - red
		img_r_edge[1,2:(img_width+2)] = ((2 * img_r_edge[2,2:(img_width+2)]) - img_r_edge[3,2:(img_width+2)]) # upper edge
		img_r_edge[(img_height+2),2:(img_width+2)] = ((2 * img_r_edge[(img_height+1),2:(img_width+2)]) - img_r_edge[(img_height),2:(img_width+2)]) # bottom edge
		img_r_edge[1:(img_height+3):,1] = (2 * img_r_edge[1:(img_height+3):,2]) - img_r_edge[1:(img_height+3):,3] # left edge
		img_r_edge[1:(img_height+3):,(img_width+2)] = (2 * img_r_edge[1:(img_height+3):,(img_width+1)]) - img_r_edge[1:(img_height+3):,(img_width+0)] # right edge
		img_r_edge[0,1:(img_width+3)] = ((2 * img_r_edge[1,1:(img_width+3)]) - img_r_edge[2,1:(img_width+3)]) # upper edge x 2
		img_r_edge[(img_height+3),1:(img_width+3)] = ((2 * img_r_edge[(img_height+2),1:(img_width+3)]) - img_r_edge[(img_height+1),1:(img_width+3)]) # bottom edge x 2
		img_r_edge[0:(img_height+4):,0] = (2 * img_r_edge[0:(img_height+4):,1]) - img_r_edge[0:(img_height+4):,2] # left edge x 2
		img_r_edge[0:(img_height+4):,(img_width+3)] = (2 * img_r_edge[0:(img_height+4):,(img_width+2)]) - img_r_edge[0:(img_height+4):,(img_width+1)] # right edge x 2
		# extend image for edge pixel computation - green
		#img_g_edge = numpy.zeros([(img_height+4), (img_width+4)])
		
		img_g_edge_base = multiprocessing.Array(ctypes.c_float, (img_height+4)*(img_width+4))
		img_g_edge = numpy.ctypeslib.as_array(img_g_edge_base.get_obj())
		img_g_edge = img_g_edge.reshape((img_height+4), (img_width+4))
		assert img_g_edge.base.base is img_g_edge_base.get_obj()
		
		img_g_edge[2:(img_height+2),2:(img_width+2)] = img_g[:,:]
		# compute edge pixel weight - green
		img_g_edge[1,2:(img_width+2)] = ((2 * img_g_edge[2,2:(img_width+2)]) - img_g_edge[3,2:(img_width+2)]) # upper edge
		img_g_edge[(img_height+2),2:(img_width+2)] = ((2 * img_g_edge[(img_height+1),2:(img_width+2)]) - img_g_edge[(img_height),2:(img_width+2)]) # bottom edge
		img_g_edge[1:(img_height+3):,1] = (2 * img_g_edge[1:(img_height+3):,2]) - img_g_edge[1:(img_height+3):,3] # left edge
		img_g_edge[1:(img_height+3):,(img_width+2)] = (2 * img_g_edge[1:(img_height+3):,(img_width+1)]) - img_g_edge[1:(img_height+3):,(img_width+0)] # right edge
		img_g_edge[0,1:(img_width+3)] = ((2 * img_g_edge[1,1:(img_width+3)]) - img_g_edge[2,1:(img_width+3)]) # upper edge x 2
		img_g_edge[(img_height+3),1:(img_width+3)] = ((2 * img_g_edge[(img_height+2),1:(img_width+3)]) - img_g_edge[(img_height+1),1:(img_width+3)]) # bottom edge x 2
		img_g_edge[0:(img_height+4):,0] = (2 * img_g_edge[0:(img_height+4):,1]) - img_g_edge[0:(img_height+4):,2] # left edge x 2
		img_g_edge[0:(img_height+4):,(img_width+3)] = (2 * img_g_edge[0:(img_height+4):,(img_width+2)]) - img_g_edge[0:(img_height+4):,(img_width+1)] # right edge x 2
		# extend image for edge pixel computation - blue
		#img_b_edge = numpy.zeros([(img_height+4), (img_width+4)])
		
		img_b_edge_base = multiprocessing.Array(ctypes.c_float, (img_height+4)*(img_width+4))	# Found you. Bitch
		img_b_edge = numpy.ctypeslib.as_array(img_b_edge_base.get_obj())
		img_b_edge = img_b_edge.reshape((img_height+4), (img_width+4))
		assert img_b_edge.base.base is img_b_edge_base.get_obj()
		
		img_b_edge[2:(img_height+2),2:(img_width+2)] = img_b[:,:]
		# compute edge pixel weight - blue
		img_b_edge[1,2:(img_width+2)] = ((2 * img_b_edge[2,2:(img_width+2)]) - img_b_edge[3,2:(img_width+2)]) # upper edge
		img_b_edge[(img_height+2),2:(img_width+2)] = ((2 * img_b_edge[(img_height+1),2:(img_width+2)]) - img_b_edge[(img_height),2:(img_width+2)]) # bottom edge
		img_b_edge[1:(img_height+3):,1] = (2 * img_b_edge[1:(img_height+3):,2]) - img_b_edge[1:(img_height+3):,3] # left edge
		img_b_edge[1:(img_height+3):,(img_width+2)] = (2 * img_b_edge[1:(img_height+3):,(img_width+1)]) - img_b_edge[1:(img_height+3):,(img_width+0)] # right edge
		img_b_edge[0,1:(img_width+3)] = ((2 * img_b_edge[1,1:(img_width+3)]) - img_b_edge[2,1:(img_width+3)]) # upper edge x 2
		img_b_edge[(img_height+3),1:(img_width+3)] = ((2 * img_b_edge[(img_height+2),1:(img_width+3)]) - img_b_edge[(img_height+1),1:(img_width+3)]) # bottom edge x 2
		img_b_edge[0:(img_height+4):,0] = (2 * img_b_edge[0:(img_height+4):,1]) - img_b_edge[0:(img_height+4):,2] # left edge x 2
		img_b_edge[0:(img_height+4):,(img_width+3)] = (2 * img_b_edge[0:(img_height+4):,(img_width+2)]) - img_b_edge[0:(img_height+4):,(img_width+1)] # right edge x 2
	
	#!print "endbigassifelse"

	a = 1	# Change me for benchmarks or space heater mode
	
	if (a==1):
		#!print "Doing OpenCL"

		#print lengthIBF	# V bistcu iTY*iTX
	
	 	
		# Screw it, bomo na roke
		# Koncna velikost slike je
		# cv_img_out = cv.CreateImage((img_tpl.shape[1], img_tpl.shape[0]),cv_img.depth,cv_img.nChannels) # img_tpl.shape[0] - width,
		# Tj. imgTplY * imgTplX, potem pa se kanali...

		# Input-i v kernel so:
		# - inX - matrika indexov x (input slika)
		# - inY - matrika indexov Y (input slika)
		# - inW - matrika indexov w (x output slike) -> po dolzini tega moramo spravit kernel cez
		# - inH - matrika indexov h (y output slike)
		# - inR - 
		# - inG - 
		# - inB - 
		# - beta matrika (5x5)	-> PER PIXEL. FUCK
		# - velikost matrik, da se bomo lahko cez pomikali
		# Output-i iz kernela so:
		# - outR - velikosti  
		# - outG - 
		# - outB -
	
		# edge matrike bo treba pretvorit v 1D
		# Da poenostavimo, gremo lahko cez list in si pripravimo vrednosti

		lengthIBF = len(img_beta_f)
		inR = img_r_edge.reshape(img_r_edge.shape[0]*img_r_edge.shape[1])
		inG = img_g_edge.reshape(img_r_edge.shape[0]*img_r_edge.shape[1])
		inB = img_b_edge.reshape(img_r_edge.shape[0]*img_r_edge.shape[1])
		inYRGB = numpy.zeros(1, 'int32');	# By the power of grayskull
		inYRGB[0] = img_r_edge.shape[0]
		inXRGB = numpy.zeros(1, 'int32');
		inXRGB[0] = img_r_edge.shape[1]
		inWRGB = numpy.zeros(1, 'int32');
		inWRGB[0] = img_tpl.shape[0]
		inHRGB = numpy.zeros(1, 'int32');
		inHRGB[0] = img_tpl.shape[1]

		ctx = cl.create_some_context()
		#!print ctx	# On what we working on
		queue = cl.CommandQueue(ctx)

		mf = cl.mem_flags
		inX_buf = cl.Buffer(ctx, mf.READ_ONLY | mf.COPY_HOST_PTR, hostbuf=inX)
		inY_buf = cl.Buffer(ctx, mf.READ_ONLY | mf.COPY_HOST_PTR, hostbuf=inY)
		inW_buf = cl.Buffer(ctx, mf.READ_ONLY | mf.COPY_HOST_PTR, hostbuf=inW)	
		inH_buf = cl.Buffer(ctx, mf.READ_ONLY | mf.COPY_HOST_PTR, hostbuf=inH)
		inBetaMatrika_buf = cl.Buffer(ctx, mf.READ_ONLY | mf.COPY_HOST_PTR, hostbuf=inBetaMatrika)
		inR_buf = cl.Buffer(ctx, mf.READ_ONLY | mf.COPY_HOST_PTR, hostbuf=inR)
		inG_buf = cl.Buffer(ctx, mf.READ_ONLY | mf.COPY_HOST_PTR, hostbuf=inG)
		inB_buf = cl.Buffer(ctx, mf.READ_ONLY | mf.COPY_HOST_PTR, hostbuf=inB)
		inYRGB_buf = cl.Buffer(ctx, mf.READ_ONLY | mf.COPY_HOST_PTR, hostbuf=inYRGB)	# Prvo Y, for some reason
		inXRGB_buf = cl.Buffer(ctx, mf.READ_ONLY | mf.COPY_HOST_PTR, hostbuf=inXRGB)
		inHRGB_buf = cl.Buffer(ctx, mf.READ_ONLY | mf.COPY_HOST_PTR, hostbuf=inHRGB)
		inWRGB_buf = cl.Buffer(ctx, mf.READ_ONLY | mf.COPY_HOST_PTR, hostbuf=inWRGB)	
		tempRGB = numpy.zeros(lengthIBF, 'float32')
		outR_buf = cl.Buffer(ctx, mf.WRITE_ONLY, tempRGB.nbytes)	# This should do
		outG_buf = cl.Buffer(ctx, mf.WRITE_ONLY, tempRGB.nbytes)
		outB_buf = cl.Buffer(ctx, mf.WRITE_ONLY, tempRGB.nbytes)

		prg = cl.Program(ctx, """
		    __kernel void sum(__global const int *inX, __global const int *inY, __global const int *inW, __global const int *inH, 
		     __global const float *inBetaMatrika, __global const float *inR, __global const float *inG, __global const float *inB,
			__global const int *inYRGB, __global const int *inXRGB, __global const int *inHRGB, __global const int *inWRGB,
				__global float *outR, __global float *outG, __global float *outB)
		    {
			int gid = get_global_id(0);
			int indeksX = inX[gid];
			int indeksY = inY[gid];
			int indeksLokacijaRGB = indeksY + inYRGB[0]*indeksX ;
			int indeksW = inW[gid];
			int indeksH = inH[gid];
			int indeksLokacijaHW = indeksH + inHRGB[0]*indeksW;
			int n = 3;	// cubic B-spline
			int K = 4;	// K = n + 1
	
			int k1 = (indeksX - ((n+1) / 2));
			int l1 = (indeksY - ((n+1) / 2));
			int i = 0;
			int j = 0;
			float sumR = 0;
			float sumG = 0;
			float sumB = 0;
			int counter = 0;
			for (i = l1+2; i < l1+7; i++){
				for (j = k1+2; j < k1+7; j++){
					float tempBeta = inBetaMatrika[gid*25 + counter];	// Whoops
					int indeksTemp = j+i*inXRGB[0];
					sumR = sumR + inR[indeksTemp] * tempBeta;
					sumG = sumG + inG[indeksTemp] * tempBeta;
					sumB = sumB + inB[indeksTemp] * tempBeta;
					counter++;
				}
			}
			outR[indeksLokacijaHW] = sumR;
			outG[indeksLokacijaHW] = sumG;
			outB[indeksLokacijaHW] = sumB;


		      //outR[indeksLokacijaHW] = inR[indeksLokacijaRGB];	// Friggin finally works
		      //outG[indeksLokacijaHW] = inG[indeksLokacijaRGB];
		      //outB[indeksLokacijaHW] = inB[indeksLokacijaRGB];

		    }
		    """).build()
		event = prg.sum(queue, inX.shape, None, inX_buf, inY_buf, inW_buf, inH_buf, inBetaMatrika_buf, inR_buf, inG_buf, inB_buf, inYRGB_buf, inXRGB_buf, inHRGB_buf, inWRGB_buf, outR_buf, outG_buf, outB_buf)	# Gremo cez vse indexe v X, Y in W, H

		event.wait()

		outR = numpy.empty_like(tempRGB)
		outG = numpy.empty_like(tempRGB)
		outB = numpy.empty_like(tempRGB)
		cl.enqueue_copy(queue, outR, outR_buf)
		cl.enqueue_copy(queue, outG, outG_buf)
		cl.enqueue_copy(queue, outB, outB_buf)

		reshapedR = outR.reshape(img_tpl.shape[0], img_tpl.shape[1])	# Iste dimenzije kot template
		reshapedG = outG.reshape(img_tpl.shape[0], img_tpl.shape[1])
		reshapedB = outB.reshape(img_tpl.shape[0], img_tpl.shape[1])


		for i in range(0, lengthIBF):		
			cv_img_out[inW[i], inH[i]] = (reshapedR[inW[i], inH[i]], reshapedG[inW[i], inH[i]], reshapedB[inW[i], inH[i]])	
	else:
		print "Doing oldfashioned"
		# arguments for individual process (might use a lot of memorry)
		worker_arguments = []
	
		# pool of process workes
		pool = Pool(processes = multiprocessing.cpu_count())
		print "workdisplacement"


		# Multicolor only
		for i in img_beta_f :
			worker_arguments.append((img_tpl[i[1]][i[2]][1], img_tpl[i[1]][i[2]][0], i[1], i[2], i[0]))
		pool_outputs = pool.map(cubic_setBeta_multiChannel, worker_arguments)
		

		pool.close()
		pool.join()
		print "endworkdisplacement"
		for o in pool_outputs :
			cv_img_out[o[1],o[2]] = o[0]	# To lahko na roke...
	return cv_img_out

