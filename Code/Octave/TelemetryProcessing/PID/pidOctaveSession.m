#! /usr/bin/octave -qf

# set gnuplot as default plotting object (historical reasons)
graphics_toolkit("gnuplot")

# set format for displaying - we like to see whole numbers
#format long

# input arguments:
arg_list = argv();
# arg_list{1} - paht to mission info data file
# arg_list{2} - paht to PID data file

# add function path, one directory up, "global" process mission info function
addpath( strtrunc( pwd, strchr(pwd,"/",1,"last")-1 ) )

# get mission info data
[msnTime_start, msnTime_stop] = processMissionInfo(arg_list{1});

# get pid data
[pidTime,pidValue,pidTarget,pidOutput,pidPError,pidIError,pidDError,pidP,pidI,pidD] = pidGetData(arg_list{2});

# prepare sample count
sampleCount = [1:length(pidTime)];

# plot pid sample timestamp graph
figure;
plot(((pidTime-msnTime_start)/1000), sampleCount);	# timestamps in seconds
axis tight;
title("samples in time");
xlabel("time (s)");
ylabel("samples (n)");
legend("samples", 'Location','NorthWest');
legend("right");

# plot sample time difference graph
figure;
plot(((pidTime-msnTime_start)/1000), [(pidTime-msnTime_start)(2:end) (pidTime-msnTime_start)(end)]-(pidTime-msnTime_start) );	# timestamps in seconds
axis tight;
title("sample time difference");
xlabel("time (s)");
ylabel("time difference (ms)");
legend("difference", 'Location','NorthWest');
legend("right");

# plot pid input value graph
figure;
plot(((pidTime-msnTime_start)/1000), pidValue);	# input value in seconds
axis tight;
title("input value in time");
xlabel("time (s)");
ylabel("input value (e)");
legend("input value", 'Location','NorthWest');
legend("right");

# plot pid target value graph
figure;
plot(((pidTime-msnTime_start)/1000), pidTarget);	# input value in seconds
axis tight;
title("target in time");
xlabel("time (s)");
ylabel("target (e)");
legend("target", 'Location','NorthWest');
legend("right");

# plot pid output value graph
figure;
plot(((pidTime-msnTime_start)/1000), pidOutput);	# input value in seconds
axis tight;
title("output value in time");
xlabel("time (s)");
ylabel("output value (e)");
legend("output value", 'Location','NorthWest');
legend("right");

# plot pid P error graph
figure;
plot(((pidTime-msnTime_start)/1000), pidPError);	# input value in seconds
axis tight;
title("P error in time");
xlabel("time (s)");
ylabel("P error (e)");
legend("P error", 'Location','NorthWest');
legend("right");

# plot pid I error graph
figure;
plot(((pidTime-msnTime_start)/1000), pidIError);	# input value in seconds
axis tight;
title("I error in time");
xlabel("time (s)");
ylabel("I error (e)");
legend("I error", 'Location','NorthWest');
legend("right");

# plot pid D error graph
figure;
plot(((pidTime-msnTime_start)/1000), pidDError);	# input value in seconds
axis tight;
title("D error in time");
xlabel("time (s)");
ylabel("D error (e)");
legend("D error", 'Location','NorthWest');
legend("right");

# plot pid P graph
figure;
plot(((pidTime-msnTime_start)/1000), pidP);	# input value in seconds
axis tight;
title("P in time");
xlabel("time (s)");
ylabel("P (e)");
legend("P", 'Location','NorthWest');
legend("right");

# plot pid I graph
figure;
plot(((pidTime-msnTime_start)/1000), pidI);	# input value in seconds
axis tight;
title("I in time");
xlabel("time (s)");
ylabel("I (e)");
legend("I", 'Location','NorthWest');
legend("right");

# plot pid D graph
figure;
plot(((pidTime-msnTime_start)/1000), pidD);	# input value in seconds
axis tight;
title("D in time");
xlabel("time (s)");
ylabel("D (e)");
legend("D", 'Location','NorthWest');
legend("right");
