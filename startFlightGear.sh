#!/bin/bash

#fgfs --prop:/sim/rendering/texture-compression=off

#fgfs --prop:/sim/rendering/texture-compression=off --generic=serial,in,25,/dev/ttyUSB0,57600,RC2USB_in_controls

#fgfs --aircraft=Rascal110-JSBSim --lat=45.93129166666667 --lon=15.491208333333333 --heading=100 --timeofday=noon --disable-random-objects

#fgfs --geometry=480x320 --aircraft=Rascal110-JSBSim --lat=45.93129166666667 --lon=15.491208333333333 --heading=100 --altitude=1000 --timeofday=noon --disable-random-objects --generic=socket,out,1,127.0.0.1,3001,tcp,FlightGear_out_gps --generic=socket,out,40,127.0.0.1,3002,tcp,FlightGear_out_ori --generic=socket,in,40,,3000,tcp,FlightGear_in_controls

# base
#fgfs --prop:/sim/rendering/texture-compression=off --generic=socket,in,25,,3000,tcp,FlightGear_in_controls --generic=socket,out,1,192.168.0.14,3001,tcp,FlightGear_out_gps --generic=socket,out,60,192.168.0.14,3002,tcp,FlightGear_out_ori --aircraft=Rascal110-JSBSim --lat=45.93129166666667 --lon=15.491208333333333 --altitude=1200 --heading=330 --timeofday=noon --geometry=1280x800

# base
#fgfs --prop:/sim/rendering/texture-compression=off --generic=socket,in,25,,3000,tcp,FlightGear_in_controls --generic=socket,out,1,192.168.43.1,3001,tcp,FlightGear_out_gps --generic=socket,out,60,192.168.43.1,3002,tcp,FlightGear_out_ori --aircraft=Rascal110-JSBSim --lat=45.93129166666667 --lon=15.491208333333333 --altitude=1200 --heading=330 --timeofday=noon --geometry=480x320

# home
#fgfs --prop:/sim/rendering/texture-compression=off --generic=socket,in,25,,3000,tcp,FlightGear_in_controls --generic=socket,out,1,192.168.0.25,3001,tcp,FlightGear_out_gps --generic=socket,out,60,192.168.0.25,3002,tcp,FlightGear_out_ori --aircraft=Rascal110-JSBSim --lat=45.99746666666667 --lon=15.447336111111111 --altitude=1200 --heading=330 --timeofday=noon --geometry=1280x800

# home
#fgfs --prop:/sim/rendering/texture-compression=off --generic=socket,in,25,,3000,tcp,FlightGear_in_controls --generic=socket,out,1,192.168.43.1,3001,tcp,FlightGear_out_gps --generic=socket,out,60,192.168.43.1,3002,tcp,FlightGear_out_ori --aircraft=Rascal110-JSBSim --lat=45.99746666666667 --lon=15.447336111111111 --altitude=1200 --heading=330 --timeofday=noon --geometry=1280x800

# SiS demo
fgfs --prop:/sim/rendering/texture-compression=off --generic=socket,in,25,,3000,tcp,FlightGear_in_controls --generic=socket,out,1,164.8.19.62,3001,tcp,FlightGear_out_gps --generic=socket,out,60,164.8.19.62,3002,tcp,FlightGear_out_ori --aircraft=Rascal110-JSBSim --lat=45.93129166666667 --lon=15.491208333333333 --altitude=1200 --heading=330 --timeofday=noon --geometry=480x320
