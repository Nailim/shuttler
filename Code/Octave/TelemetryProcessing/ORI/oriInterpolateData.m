#! /usr/bin/octave -qf

# set format for displaying - we like to see whole numbers
#format long

# input arguments:
arg_list = argv();
# arg_list{1} - paht to mission info data file
# arg_list{2} - paht to ORI data file
# arg_list{3} - paht to interpolated data files

# add function path, one directory up, "global" process mission info function
#addpath( strtrunc( pwd, strchr(pwd,"/",1,"last")-1 ) )
# add function path, hardcoded
addpath( "/media/Data/Project/Shuttler/Code/Octave/TelemetryProcessing" )
addpath( "/media/Data/Project/Shuttler/Code/Octave/TelemetryProcessing/ORI" )

# get mission info data
[msnTime_start, msnTime_stop] = processMissionInfo(arg_list{1});

# get ori data
[oriTime,oriYaw,oriPitch,oriRoll] = oriGetData(arg_list{2});

# interpolate time stamps, 30 fps (1000ms/30 = 33.33333ms between frames)
#oriTimeInterpolated=[oriTime(1):33.33333:oriTime(end)];
oriTimeInterpolated=[msnTime_start:33.33333:msnTime_stop];

# interpolate data
oriPitchInterpolated=interp1(oriTime, oriPitch(1:end), oriTimeInterpolated);
oriRollInterpolated=interp1(oriTime, oriRoll(1:end), oriTimeInterpolated);
oriYawInterpolated=interp1(oriTime, oriYaw(1:end), oriTimeInterpolated);

# save data
oriPitchInterpolated=oriPitchInterpolated';
oriRollInterpolated=oriRollInterpolated';
oriYawInterpolated=oriYawInterpolated';
save(strcat(pwd, "/", arg_list{3}, "interpolated_pitch_30fps.txt"), "oriPitchInterpolated");
save(strcat(pwd, "/", arg_list{3}, "interpolated_roll_30fps.txt"), "oriRollInterpolated");
save(strcat(pwd, "/", arg_list{3}, "interpolated_yaw_30fps.txt"), "oriYawInterpolated");
