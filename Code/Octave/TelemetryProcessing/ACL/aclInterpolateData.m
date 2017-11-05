#! /usr/bin/octave -qf

# set format for displaying - we like to see whole numbers
#format long

# input arguments:
arg_list = argv();
# arg_list{1} - paht to mission info data file
# arg_list{2} - paht to acl data file
# arg_list{3} - paht to interpolated data files

# add function path, one directory up, "global" process mission info function
#addpath( strtrunc( pwd, strchr(pwd,"/",1,"last")-1 ) )
# add function path, hardcoded
addpath( "/media/Data/Project/Shuttler/Code/Octave/TelemetryProcessing" )
addpath( "/media/Data/Project/Shuttler/Code/Octave/TelemetryProcessing/ACL" )

# get mission info data
[msnTime_start, msnTime_stop] = processMissionInfo(arg_list{1});

# get acl data
[aclTime,aclX,aclY,aclZ] = aclGetData(arg_list{2});

# interpolate time stamps, 30 fps (1000ms/30 = 33.33333ms between frames)
#aclTimeInterpolated=[aclTime(1):33.33333:aclTime(end)];
aclTimeInterpolated=[msnTime_start:33.33333:msnTime_stop];


# interpolate data
aclXInterpolated=interp1(aclTime, aclX(1:end), aclTimeInterpolated);
aclYInterpolated=interp1(aclTime, aclY(1:end), aclTimeInterpolated);
aclZInterpolated=interp1(aclTime, aclZ(1:end), aclTimeInterpolated);

# save data
aclXInterpolated=aclXInterpolated';
aclYInterpolated=aclYInterpolated';
aclZInterpolated=aclZInterpolated';
save(strcat(pwd, "/", arg_list{3}, "interpolated_aclX_30fps.txt"), "aclXInterpolated");
save(strcat(pwd, "/", arg_list{3}, "interpolated_aclY_30fps.txt"), "aclYInterpolated");
save(strcat(pwd, "/", arg_list{3}, "interpolated_aclZ_30fps.txt"), "aclZInterpolated");
