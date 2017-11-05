# -*- coding: utf-8 -*-
"""
Created on Wed Sep 26 23:37:13 2012

@author: j
"""

import cv
import math
import numpy

if __name__ == "__main__":            # this is not a module
    
    scale = 10
    
    # load image
    cv_img = cv.LoadImage("__1.jpg", cv.CV_LOAD_IMAGE_GRAYSCALE) # CV_LOAD_IMAGE_UNCHANGED
    # width & height
    cv_img_width = cv.GetSize(cv_img)[0]
    cv_img_height = cv.GetSize(cv_img)[1]
    
    img_tpl = numpy.zeros( ((cv_img_height * scale),(cv_img_width * scale),2) )
    
    for h in range(0,(cv_img_height * scale),1) :
        for w in range(0,(cv_img_width * scale),1) :
            img_tpl[h][w][0] = (h + 0) / (cv_img_height * scale * 1.0) * cv_img_height
            img_tpl[h][w][1] = (w + 0) / (cv_img_width * scale * 1.0) * cv_img_width

    #bSpl = bSpline.BSpline()
    #cv_img_out = bSpl.cubic(cv_img, img_tpl)
    # setting coefficients
    n = 3    # cubic B-spline
    K = 4    # K = n + 1
            
    A = 0
    
    img_ch = cv_img.nChannels
    img_dpth = cv_img.depth
    
    # grayscale, only one channel
    #b = self.cubic_b_gray(cv_img)
    cv_img_width = cv.GetSize(cv_img)[0]
    cv_img_height = cv.GetSize(cv_img)[1]
    
    b = numpy.zeros([cv_img_height,cv_img_width])
    b_img = numpy.zeros([cv_img_height,cv_img_width])
    b_img_out = numpy.zeros([(cv_img_height * scale), (cv_img_width * scale)])
    #print cv_img.nChannels
    
    vector = numpy.zeros([1,cv_img_width])
    #A = cubic_banal_prep(vector)
    N = numpy.shape(vector)[1]
    A = numpy.zeros([N,N])
    
    A[0][0] = (2.0/3.0)
    A[0][1] = (1.0/6.0)
    
    A[N-1][N-2] = (1.0/6.0)
    A[N-1][N-1] = (2.0/3.0)
        
    for p in range(1,N-1,1) :
        A[p][p-1] = (1.0/6.0)
        A[p][p] = (2.0/3.0)
        A[p][p+1] = (1.0/6.0)
    
    for row in range(0,cv_img_height,1) :
        vector = numpy.zeros([1,cv_img_width])
        for column in range(0,cv_img_width,1) :
            vector[0][column] = cv_img[row,column]
            #debug
            b_img[row][column] = cv_img[row,column]
        #b[row,:] = cubic_banal(vector)
        N = numpy.shape(vector)[1]
        if N == 1 :
            #return (vector * 1.5)
            vector = (vector * 1.5)
        else :
            vector = vector.transpose()
            numpy.linalg.lstsq(A,vector)         
            vector = vector.transpose()

            E = numpy.array([[2,4],[4,2]])
            e = numpy.array([[4],[4]])
            o = numpy.linalg.lstsq(E,e)[0]

        
        b[row,:] = vector
        
    vector = numpy.zeros([1,cv_img_height])
    #A = cubic_banal_prep(vector)
    N = numpy.shape(vector)[1]
    A = numpy.zeros([N,N])
    
    A[0][0] = (2.0/3.0)
    A[0][1] = (1.0/6.0)
    
    A[N-1][N-2] = (1.0/6.0)
    A[N-1][N-1] = (2.0/3.0)
        
    for p in range(1,N-1,1) :
        A[p][p-1] = (1.0/6.0)
        A[p][p] = (2.0/3.0)
        A[p][p+1] = (1.0/6.0)
      
    for column in range(0,cv_img_width,1) :
        vector = numpy.zeros([1,cv_img_height])
        for row in range(0,cv_img_height,1) :
            vector[0][row] = b[row,column]
        #b[:,column] = cubic_banal(vector)
        N = numpy.shape(vector)[1]
        if N == 1 :
            #return (vector * 1.5)
            vector = (vector * 1.5)
        else :
            vector = vector.transpose()
            numpy.linalg.lstsq(A,vector)
            vector = vector.transpose()
        
        b[:,column] = vector
    
    # create output image !!! fix for 16 bit images
    if img_dpth == 8 :
        cv_img_out = cv.CreateImage((img_tpl.shape[1], img_tpl.shape[0]),cv.IPL_DEPTH_8U,cv_img.nChannels)    # img_tpl.shape[0] - width, img_tpl.shape[1] - height
    else :
        cv_img_out = cv.CreateImage((img_tpl.shape[1], img_tpl.shape[0]),cv.IPL_DEPTH_8U,cv_img.nChannels)    # img_tpl.shape[0] - width, img_tpl.shape[1] - height
        
    if img_ch == 1 :
        for h in range(0,img_tpl.shape[1],1) :
            for w in range(0,img_tpl.shape[0],1) :
                #cv_img_out[h,w] = self.cubic_f(cv_img, img_tpl[h][w][1], img_tpl[h][w][0])
                ##print ">>>>>> x: %d; y: %d" % (w,h)
                #cv_img_out[h,w] = self.cubic_f(b, img_tpl[w][h][1], img_tpl[w][h][0])
                #k1 = cubic_coef(numpy.floor(x))
                #l1 = cubic_coef(numpy.floor(y))
                k1 = ((numpy.floor(img_tpl[w][h][1])) - ((n+1)/2))
                l1 = ((numpy.floor(img_tpl[w][h][0])) - ((n+1)/2))
                
                #print "---"
                #print x
                #print y
                #print k1
                #print l1
                
                #cv_img_width = cv.GetSize(cv_img)[0]
                #cv_img_height = cv.GetSize(cv_img)[1]
                img_width = b.shape[1] - 1
                img_height = b.shape[0] - 1
                
                pix_value = 0.0;
                #print ".....> x: %f; y: %f" % (x,y)
                for k in numpy.arange(k1,(k1+K+1),1) :        # + 1 -> for k < k1+K
                    for l in numpy.arange(l1,(l1+K+1),1) :    # + 1 -> for l < l1+K
                        f_b = 0.0
                        beta3_x = 0.0
                        beta3_y = 0.0
                        
                        # edge pixels
                        if k < 0 :
                            k = 0
                        elif k > img_width :
                            k = img_width
                        
                        if l < 0 :
                            l = 0
                        elif l > img_height :
                            l = img_height
                        
                        f_b = b[l, k]
                        
                        #beta3_x = cubic_beta3(x - k)                
                        #if beta3_x == 0 :
                        #    break
                        
                        #beta3_y = cubic_beta3(y - l)
                        #if beta3_y == 0 :
                        #    break
                        
                        #pix_value = pix_value + (f_b * beta3_x * beta3_y)
                        
                        #beta3_x = cubic_beta3(x - k)
                        #pix = numpy.abs(x - k)
                        pix = numpy.abs(img_tpl[h][w][1] - k)                        
                        #print pix
                        if pix >= 2 :
                            #print 0
                            #return 0
                            beta3_x = 0
                        elif (pix >= 1) and (pix < 2) :
                            #print (numpy.power((2.0 - numpy.abs(pix)),3) / 6.0)
                            #return (numpy.power((2.0 - pix),3) / 6.0) 
                            beta3_x = (numpy.power((2.0 - pix),3) / 6.0)
                            #return 1
                        else :
                            #print ((2.0/3.0) - numpy.power(numpy.abs(pix),2) + (numpy.power(numpy.abs(pix),3) / 2.0))
                            #return ((2.0/3.0) - numpy.power(pix,2) + (numpy.power(pix,3) / 2.0))
                            beta3_x = ((2.0/3.0) - numpy.power(pix,2) + (numpy.power(pix,3) / 2.0))
                        #return 1
                        if beta3_x != 0 :
                            #beta3_y = cubic_beta3(y - l)
                            #pix = numpy.abs(y - l)
                            pix = numpy.abs(img_tpl[w][h][0] - l)
                                                        
                            #print pix
                            if pix >= 2 :
                                #print 0
                                #return 0
                                beta3_y = 0
                            elif (pix >= 1) and (pix < 2) :
                                #print (numpy.power((2.0 - numpy.abs(pix)),3) / 6.0)
                                #return (numpy.power((2.0 - pix),3) / 6.0) 
                                beta3_y = (numpy.power((2.0 - pix),3) / 6.0)
                                #return 1
                            else :
                                #print ((2.0/3.0) - numpy.power(numpy.abs(pix),2) + (numpy.power(numpy.abs(pix),3) / 2.0))
                                #return ((2.0/3.0) - numpy.power(pix,2) + (numpy.power(pix,3) / 2.0))
                                beta3_y = ((2.0/3.0) - numpy.power(pix,2) + (numpy.power(pix,3) / 2.0))                            
                            if beta3_y != 0 :
                                pix_value = pix_value + (f_b * beta3_x * beta3_y)
                        
                cv_img_out[h,w] = pix_value
                b_img_out[h][w] = pix_value
    
    
    print img_tpl.shape
    print cv_img_out
    print cv.GetSize(cv_img)
    cv.SaveImage("out__1.jpg", cv_img_out)
    
    x = 4
    