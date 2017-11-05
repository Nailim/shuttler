#! /usr/bin/octave -qf

# set format for displaying - we like to see whole numbers
#format long

# input arguments:
arg_list = argv();
# arg_list{1} - paht to mission info data file
# arg_list{2} - paht to gra data file
# arg_list{3} - paht to interpolated data files

# add function path, one directory up, "global" process mission info function
#addpath( strtrunc( pwd, strchr(pwd,"/",1,"last")-1 ) )
# add function path, hardcoded
addpath( "/media/Data/Project/Shuttler/Code/Octave/TelemetryProcessing" )
addpath( "/media/Data/Project/Shuttler/Code/Octave/TelemetryProcessing/GRA" )

# get mission info data
[msnTime_start, msnTime_stop] = processMissionInfo(arg_list{1});

# get gra data
[graTime,graX,graY,graZ] = graGetData(arg_list{2});

# interpolate time stamps, 30 fps (1000ms/30 = 33.33333ms between frames)
#graTimeInterpolated=[graTime(1):33.33333:graTime(end)];
graTimeInterpolated=[msnTime_start:33.33333:msnTime_stop];


# interpolate data
graXInterpolated=interp1(graTime, graX(1:end), graTimeInterpolated);
graYInterpolated=interp1(graTime, graY(1:end), graTimeInterpolated);
graZInterpolated=interp1(graTime, graZ(1:end), graTimeInterpolated);

# save data
graXInterpolated=graXInterpolated';
graYInterpolated=graYInterpolated';
graZInterpolated=graZInterpolated';
save(strcat(pwd, "/", arg_list{3}, "interpolated_graX_30fps.txt"), "graXInterpolated");
save(strcat(pwd, "/", arg_list{3}, "interpolated_graY_30fps.txt"), "graYInterpolated");
save(strcat(pwd, "/", arg_list{3}, "interpolated_graZ_30fps.txt"), "graZInterpolated");
