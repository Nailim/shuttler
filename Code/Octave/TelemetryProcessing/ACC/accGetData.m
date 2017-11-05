# read orientation and parse data
function [accTime,accX,accY,accZ] = accGetData(inFile)
	# read acc data
	inData = csvread(inFile);
	
	# parse acc data
	accTime = inData(:,1)';
	accX = inData(:,2)';
	accY = inData(:,3)';
	accZ = inData(:,4)';
endfunction
