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
[oriTime,oriYaw,oriPitch,oriRoll] = oriGetData(arg_list{2});

# prepare sample count
sampleCount = [1:length(oriTime)];

# plot gps sample timestamp graph
figure;
plot(((oriTime-msnTime_start)/1000), sampleCount);	# timestamps in seconds
axis tight;
title("samples in time");
xlabel("time (s)");
ylabel("samples (n)");
legend("samples", 'Location','NorthWest');
legend("right");

# plot sample time difference graph
figure;
plot(((oriTime-msnTime_start)/1000), [(oriTime-msnTime_start)(2:end) (oriTime-msnTime_start)(end)]-(oriTime-msnTime_start) );	# timestamps in seconds
axis tight;
title("sample time difference");
xlabel("time (s)");
ylabel("time difference (ms)");
legend("difference", 'Location','NorthWest');
legend("right");

# plot yaw graph
figure;
plot(((oriTime-msnTime_start)/1000), oriYaw);	# degrees in seconds
axis tight;
title("yaw in time");
xlabel("time (s)");
ylabel("yaw (deg)");
legend("yaw", 'Location','NorthWest');
legend("right");

# plot pitch graph
figure;
plot(((oriTime-msnTime_start)/1000), oriPitch);	# degrees in seconds
axis tight;
title("pitch in time");
xlabel("time (s)");
ylabel("pitch (deg)");
legend("pitch", 'Location','NorthWest');
legend("right");

# plot roll graph
figure;
plot(((oriTime-msnTime_start)/1000), oriRoll);	# degrees in seconds
axis tight;
title("roll in time");
xlabel("time (s)");
ylabel("roll (deg)");
legend("roll", 'Location','NorthWest');
legend("right");
