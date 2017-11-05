#! /bin/bash

# step by step - small 01 image

# calibrate cameras and fix lense distortion (images get "cal_" prefix)
python chkCalibration.py -d data_left data_right

# find coresponding matches between images, depending on the parameters could be better but is usualy worse (file gets "_kp" sufix)
python featureMatchStereo.py -l data_left/cal_img_left_01_small.jpg -r data_right/cal_img_right_01_small.jpg -n data_01_small -d -p 0.5 -s 10.0 -f surf

# use epipolar geometry to produce a workable stereo image set, if you have good matches (images get "warp_" prefix)
python epiGeometryStereo.py -l data_left/cal_img_left_01_small.jpg -r data_right/cal_img_right_01_small.jpg -n data_01_small -d

# create disparity image from stereo image set
python disparityStereo.py -l data_left/warp_cal_img_left_01_small.jpg -r data_right/warp_cal_img_right_01_small.jpg -n data_01_small -d

# create more but less reliable coresponding matches and do the reconstruction
python featureMatchStereo.py -l data_left/cal_img_left_01_small.jpg -r data_right/cal_img_right_01_small.jpg -n data_01_small_points -d -p 0.75 -s 10.0 -f sift
python reconstructStereo.py -l data_left/cal_img_left_01_small.jpg -lc data_left/calibration/intrinsics.txt -r data_right/cal_img_right_01_small.jpg -rc data_right/calibration/intrinsics.txt -nc data_01_small_kp.txt -np data_01_small_points_kp.txt -d
