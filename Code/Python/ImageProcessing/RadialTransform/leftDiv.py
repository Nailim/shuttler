# -*- coding: utf-8 -*-
"""
Created on Thu Sep 27 23:39:12 2012

@author: j
"""

import numpy

N = 20
E = numpy.zeros([N,N])

E[0][0] = (2.0/3.0)
E[0][1] = (1.0/6.0)

E[N-1][N-2] = (1.0/6.0)
E[N-1][N-1] = (2.0/3.0)
    
for p in range(1,N-1,1) :
    E[p][p-1] = (1.0/6.0)
    E[p][p] = (2.0/3.0)
    E[p][p+1] = (1.0/6.0)

e = numpy.zeros([1,N])
for n in range(0,N,1) :
    e[0][n] = 255.0
e = e.transpose()

#E = numpy.array([[0.667,0.167,0.0],[0.167,0.667,0.167],[0.0,0.167,0.667]])
#e = numpy.array([[255.0],[255.0],[255.0]])
o = numpy.linalg.lstsq(E,e)[0]

x = 0