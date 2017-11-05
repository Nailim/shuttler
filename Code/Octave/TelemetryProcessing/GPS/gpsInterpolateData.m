#! /usr/bin/octave -qf

# set format for displaying - we like to see whole numbers
#format long

# input arguments:
arg_list = argv();
# arg_list{1} - paht to mission info data file
# arg_list{2} - paht to GPS data file
# arg_list{3} - paht to interpolated data files

# add function path, one directory up, "global" process mission info function
#addpath( strtrunc( pwd, strchr(pwd,"/",1,"last")-1 ) )
# add function path, hardcoded
addpath( "/media/Data/Project/Shuttler/Code/Octave/TelemetryProcessing" )
addpath( "/media/Data/Project/Shuttler/Code/Octave/TelemetryProcessing/GPS" )

# get mission info data
[msnTime_start, msnTime_stop] = processMissionInfo(arg_list{1});

# get gps data
[gpsTime,gpsLatitude,gpsLongitude,gpsAltitude,gpsAltitudeOffset,gpsHeading,gpsSpeed,gpsAccuracy] = gpsGetData(arg_list{2});

# compute gps data
gpsAltitudeDifference = [gpsAltitude(1:end)] - [gpsAltitude(1) gpsAltitude(1:end-1)];
gpsTimeDifference = [gpsTime(1:end)] - [gpsTime(1) gpsTime(1:end-1)];
gpsAltitudeSpeed = (gpsAltitudeDifference ./ gpsTimeDifference) * 1000;

# interpolate time stamps, 30 fps (1000ms/30 = 33.33333ms between frames)
#gpsTimeInterpolated=[gpsTime(1):33.33333:gpsTime(end)];
gpsTimeInterpolated=[msnTime_start:33.33333:msnTime_stop];

# interpolate data
gpsAltitudeInterpolated=interp1(gpsTime, gpsAltitude(1:end), gpsTimeInterpolated);
gpsAltitudeSpeedInterpolated=interp1(gpsTime, gpsAltitudeSpeed(1:end), gpsTimeInterpolated);
gpsSpeedInterpolated=interp1(gpsTime, gpsSpeed(1:end), gpsTimeInterpolated);

# save data
gpsAltitudeInterpolated=gpsAltitudeInterpolated';
gpsAltitudeSpeedInterpolated=gpsAltitudeSpeedInterpolated';
gpsSpeedInterpolated=gpsSpeedInterpolated';
save(strcat(pwd, "/", arg_list{3}, "interpolated_altitude_30fps.txt"), "gpsAltitudeInterpolated");
save(strcat(pwd, "/", arg_list{3}, "interpolated_altitude_speed_30fps.txt"), "gpsAltitudeSpeedInterpolated");
save(strcat(pwd, "/", arg_list{3}, "interpolated_speed_30fps.txt"), "gpsSpeedInterpolated");
