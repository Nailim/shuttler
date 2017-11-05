# read orientation and parse data
function [graTime,graX,graY,graZ] = graGetData(inFile)
	# read gra data
	inData = csvread(inFile);
	
	# parse gra data
	graTime = inData(:,1)';
	graX = inData(:,2)';
	graY = inData(:,3)';
	graZ = inData(:,4)';
endfunction
