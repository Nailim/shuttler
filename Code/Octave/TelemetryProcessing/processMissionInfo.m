# process mission info data
function [msnTime_start, msnTime_stop] = processMissionInfo(inFile)
	# read data file
	inData =textread(inFile, "%s", "delimiter", " ");
	# loop trough data
	for i = 1:length(inData)
		# find time start stamp
		if (strcmp(inData(i,1),"time_start:"))
			msnTime_start = str2num(cell2mat(inData(i+1,1)));
		endif
		# find time stop stamp
		if (strcmp(inData(i,1),"time_stop:"))
			msnTime_stop = str2num(cell2mat(inData(i+1,1)));
		endif
	endfor
endfunction
