#! /usr/bin/octave -qf

# set gnuplot as default plotting object (historical reasons)
graphics_toolkit("gnuplot")

# set format for displaying - we like to see whole numbers
#format long

# input arguments:
arg_list = argv();
# arg_list{1} - paht to mission info data file
# arg_list{2} - paht to GPS data file

# add function path, one directory up, "global" process mission info function
addpath( strtrunc( pwd, strchr(pwd,"/",1,"last")-1 ) )

# get mission info data
[msnTime_start, msnTime_stop] = processMissionInfo(arg_list{1});

# get gps data
[gpsTime,gpsLatitude,gpsLongitude,gpsAltitude,gpsAltitudeDifference,gpsHeading,gpsSpeed,gpsAccuracy] = gpsGetData(arg_list{2});

# prepare sample count
sampleCount = [1:length(gpsTime)];

# plot gps sample timestamp graph
figure;
plot(((gpsTime-msnTime_start)/1000), sampleCount);	# timestamps in seconds
axis tight;
title("samples in time");
xlabel("time (s)");
ylabel("samples (n)");
legend("samples", 'Location','NorthWest');
legend("right");

# plot sample time difference graph
figure;
plot(((gpsTime-msnTime_start)/1000), [(gpsTime-msnTime_start)(2:end) (gpsTime-msnTime_start)(end)]-(gpsTime-msnTime_start) );	# timestamps in seconds
axis tight;
title("sample time difference");
xlabel("time (s)");
ylabel("time difference (ms)");
legend("difference", 'Location','NorthWest');
legend("right");

# plot gps speed graph
figure;
plot(((gpsTime-msnTime_start)/1000), (gpsSpeed * 3600 / 1000));	# km/h in econds
axis tight;
title("speed in time");
xlabel("time (s)");
ylabel("speed (km/h)");
legend("speed", 'Location','NorthWest');
legend("right");

# plot gps altitude graph
figure;
hold on;
plot(((gpsTime-msnTime_start)/1000), gpsAltitude);	# meters in econds
plot(((gpsTime-msnTime_start)/1000), gpsAltitude-gpsAltitudeDifference, "g");	# meters in econds
hold off;
axis tight;
title("altitude in time");
xlabel("time (s)");
ylabel("altitude (m)");
legend("measured altitude", "used altitude", 'Location','NorthWest');
legend("right");
