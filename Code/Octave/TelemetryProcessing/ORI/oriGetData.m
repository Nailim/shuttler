# read orientation and parse data
function [oriTime,oriYaw,oriPitch,oriRoll] = oriGetData(inFile)
	# read gps data
	inData = csvread(inFile);
	
	# parse gps data
	oriTime = inData(:,1)';
	oriYaw = inData(:,2)';
	oriPitch = inData(:,3)';
	oriRoll = inData(:,4)';
endfunction
