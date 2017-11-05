# read pid and parse data
function [pidTime,pidValue,pidTarget,pidOutput,pidPError,pidIError,pidDError,pidP,pidI,pidD] = pidGetData(inFile)
	# read pid data
	inData = csvread(inFile);
	
	# parse gps data
	pidTime = inData(:,1)';
	pidValue = inData(:,2)';
	pidTarget = inData(:,3)';
	pidOutput = inData(:,4)';
	pidPError = inData(:,5)';
	pidIError = inData(:,6)';
	pidDError = inData(:,7)';
	pidP = inData(:,8)';
	pidI = inData(:,9)';
	pidD = inData(:,10)';
endfunction
