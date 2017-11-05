# read gps and parse data
function [gpsTime,gpsLatitude,gpsLongitude,gpsAltitude,gpsAltitudeDifference,gpsHeading,gpsSpeed,gpsAccuracy] = gpsGetData(inFile)
	# read gps data
	inData = csvread(inFile);
	
	# parse gps data
	gpsTime = inData(:,1)';
	gpsLatitude = inData(:,2)';
	gpsLongitude = inData(:,3)';
	gpsAltitude = inData(:,4)';
	gpsAltitudeDifference = inData(:,5)';
	gpsHeading = inData(:,6)';
	gpsSpeed = inData(:,7)';
	gpsAccuracy = inData(:,8)';
endfunction
