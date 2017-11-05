#! /usr/bin/octave -qf

# set gnuplot as default plotting object (historical reasons)
graphics_toolkit("gnuplot")

# set format for displaying - we like to see whole numbers
#format long

# input arguments:
arg_list = argv();
# arg_list{1} - paht to mission info data file
# arg_list{2} - paht to gra data file

# add function path, one directory up, "global" process mission info function
addpath( strtrunc( pwd, strchr(pwd,"/",1,"last")-1 ) )

# get mission info data
[msnTime_start, msnTime_stop] = processMissionInfo(arg_list{1});

# get gra data
[graTime,graX,graY,graZ] = graGetData(arg_list{2});

# prepare sample count
sampleCount = [1:length(graTime)];

# plot gra sample timestamp graph
figure;
plot(((graTime-msnTime_start)/1000), sampleCount);	# timestamps in seconds
axis tight;
title("samples in time");
xlabel("time (s)");
ylabel("samples (n)");
legend("samples", 'Location','NorthWest');
legend("right");

# plot sample time difference graph
figure;
plot(((graTime-msnTime_start)/1000), [(graTime-msnTime_start)(2:end) (graTime-msnTime_start)(end)]-(graTime-msnTime_start) );	# timestamps in seconds
axis tight;
title("sample time difference");
xlabel("time (s)");
ylabel("time difference (ms)");
legend("difference", 'Location','NorthWest');
legend("right");

# plot X graph
figure;
plot(((graTime-msnTime_start)/1000), graX);	# m/ss in seconds
axis tight;
title("X axis in time");
xlabel("time (s)");
ylabel("X (m/ss)");
legend("X", 'Location','NorthWest');
legend("right");

# plot Y graph
figure;
plot(((graTime-msnTime_start)/1000), graY);	# m/ss in seconds
axis tight;
title("Y axis in time");
xlabel("time (s)");
ylabel("Y (m/ss)");
legend("Y", 'Location','NorthWest');
legend("right");

# plot Z graph
figure;
plot(((graTime-msnTime_start)/1000), graZ);	# m/ss in seconds
axis tight;
title("Z axis in time");
xlabel("time (s)");
ylabel("Z (m/ss)");
legend("Z", 'Location','NorthWest');
legend("right");
