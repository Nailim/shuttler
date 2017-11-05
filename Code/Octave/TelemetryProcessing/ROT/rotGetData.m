# read orientation and parse data
function [rotTime,rotX,rotY,rotZ] = rotGetData(inFile)
	# read rot data
	inData = csvread(inFile);
	
	# parse rot data
	rotTime = inData(:,1)';
	rotX = inData(:,2)';
	rotY = inData(:,3)';
	rotZ = inData(:,4)';
endfunction
