# read orientation and parse data
function [aclTime,aclX,aclY,aclZ] = aclGetData(inFile)
	# read acl data
	inData = csvread(inFile);
	
	# parse acl data
	aclTime = inData(:,1)';
	aclX = inData(:,2)';
	aclY = inData(:,3)';
	aclZ = inData(:,4)';
endfunction
