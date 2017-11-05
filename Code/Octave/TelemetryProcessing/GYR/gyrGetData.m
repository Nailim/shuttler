# read magnetometer and parse data
function [magTime,magX,magY,magZ] = gyrGetData(inFile)
	# read mag data
	inData = csvread(inFile);
	
	# parse mag data
	magTime = inData(:,1)';
	magX = inData(:,2)';
	magY = inData(:,3)';
	magZ = inData(:,4)';
endfunction
