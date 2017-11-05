#import sys
#import os

import commands		# use GeoidEval trough command line

import argparse

from geographiclib.geodesic import Geodesic
#import gpsRangeRing

global inputParser	# just a reminder, it's used as a global variable
global inputArgs	# just a reminder, it's used as a global variable

def parseInput() :
	
	global inputParser
	global inputArgs
	
	inputParser = argparse.ArgumentParser(description='Parse GPS telemetry from Anemoi autopilot')
	
	inputParser.add_argument('sessionFolder', nargs=1)
	
	inputArgs = inputParser.parse_args()
	
def processInput() :
	
	print inputArgs.sessionFolder
	
	#rr = gpsRangeRing.rangeRing()
	
	sourceFile_info = open(inputArgs.sessionFolder[0]+"/mission/info.txt", 'r')
	sourceFile_gps = open(inputArgs.sessionFolder[0]+"/logs/gps.csv", 'r')
	outputFile = open(inputArgs.sessionFolder[0]+"_processed/logs/gps.kml", 'w')
	
	tab_count = 0
	
	speed_min = 0
	speed_max = 0
	speed_slice = 0
	
	altitude_min = 0
	altitude_max = 0
	altitude_slice = 0
	
	# get info data
	sourceFile_info.seek(0)
	for line in sourceFile_info :
		if (line.strip().split(" ",-1)[0] == "speed_min:") :
			speed_min = float(line.strip().split(" ",-1)[1])
		if (line.strip().split(" ",-1)[0] == "speed_max:") :
			speed_max = float(line.strip().split(" ",-1)[1])
		
		if (line.strip().split(" ",-1)[0] == "altitude_min:") :
			altitude_min = float(line.strip().split(" ",-1)[1])
		if (line.strip().split(" ",-1)[0] == "altitude_max:") :
			altitude_max = float(line.strip().split(" ",-1)[1])
	
	speed_slice = (((speed_max - speed_min) * 1000) / 3600) / 1024
	altitude_slice = (altitude_max - altitude_min) / 1024
	
	# document - header
	outputFile.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+"\n")
	outputFile.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:gx=\"http://www.google.com/kml/ext/2.2\">"+"\n")
	# document - body
	tab_count += 1
	outputFile.write(tab(tab_count)+"<Document>"+"\n")
	# document - metadata
	tab_count += 1
	outputFile.write(tab(tab_count)+"<name>Mission: !!! - GPS log</name>"+"\n")
	outputFile.write(tab(tab_count)+"<visibility>1</visibility>"+"\n")
	outputFile.write(tab(tab_count)+"<open>1</open>"+"\n")
	outputFile.write(tab(tab_count)+"<description>!!! TODO</description>"+"\n")
	# document - metadata - style simple raw
	outputFile.write(tab(tab_count)+"<Style id=\"simpleRaw\">"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<LineStyle>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<color>7f00ff00</color>"+"\n")
	outputFile.write(tab(tab_count)+"<width>2</width>"+"\n")
	outputFile.write(tab(tab_count)+"</LineStyle>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"<PolyStyle>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<color>7f00ffff</color>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</PolyStyle>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</Style>"+"\n")
	# document - metadata - style simple evaluated
	outputFile.write(tab(tab_count)+"<Style id=\"simpleEvaluated\">"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<LineStyle>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<color>7fff0000</color>"+"\n")
	outputFile.write(tab(tab_count)+"<width>4</width>"+"\n")
	outputFile.write(tab(tab_count)+"</LineStyle>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"<PolyStyle>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<color>7fffff00</color>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</PolyStyle>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</Style>"+"\n")
	# document - metadata - style simple used
	outputFile.write(tab(tab_count)+"<Style id=\"simpleUsed\">"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<LineStyle>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<color>7f0000ff</color>"+"\n")
	outputFile.write(tab(tab_count)+"<width>4</width>"+"\n")
	outputFile.write(tab(tab_count)+"</LineStyle>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"<PolyStyle>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<color>7fff00ff</color>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</PolyStyle>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</Style>"+"\n")
	# document - metadata - style rror range ring
	outputFile.write(tab(tab_count)+"<Style id=\"errorRangeRing\">"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<PolyStyle>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<fill>1</fill>"+"\n")
	outputFile.write(tab(tab_count)+"<outline>0</outline>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</PolyStyle>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</Style>"+"\n")
	# document - metadata - style speed line
	outputFile.write(tab(tab_count)+"<Style id=\"lineSpeed\">"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<LineStyle>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<width>5</width>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</LineStyle>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</Style>"+"\n")
	# document - metadata - style altitude line
	outputFile.write(tab(tab_count)+"<Style id=\"lineAltitude\">"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<LineStyle>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<width>5</width>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</LineStyle>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</Style>"+"\n")
	# document - metadata - style location point
	outputFile.write(tab(tab_count)+"<Style id=\"pointLocation\">"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<IconStyle>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<color>03ffffff</color>"+"\n")
	outputFile.write(tab(tab_count)+"<scale>0.2</scale>"+"\n")
	outputFile.write(tab(tab_count)+"<Icon>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<href>http://maps.google.com/mapfiles/kml/shapes/airports.png</href>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</Icon>"+"\n")
	outputFile.write(tab(tab_count)+"<hotSpot x=\"0.5\" y=\"0.5\" xunits=\"fraction\" yunits=\"fraction\"/>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</IconStyle>"+"\n")
	outputFile.write(tab(tab_count)+"<LineStyle>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<width>1</width>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</LineStyle>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</Style>"+"\n")
	# document - raw data - simple plot
	outputFile.write(tab(tab_count)+"<Placemark>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<name>data: raw</name>"+"\n")
#	outputFile.write(tab(tab_count)+"<description>!!!</description>"+"\n")
	outputFile.write(tab(tab_count)+"<visibility>0</visibility>"+"\n")
	outputFile.write(tab(tab_count)+"<styleUrl>#simpleRaw</styleUrl>"+"\n")
	outputFile.write(tab(tab_count)+"<LineString>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<extrude>1</extrude>"+"\n")
	outputFile.write(tab(tab_count)+"<tessellate>0</tessellate>"+"\n")
	outputFile.write(tab(tab_count)+"<altitudeMode>absolute</altitudeMode>"+"\n")
	outputFile.write(tab(tab_count)+"<coordinates>"+"\n")
	tab_count += 1
	# document - raw data - simple plot - from csv to kml
	for line in sourceFile_gps :
		# for google earth: longitude, latitude, elevation
		tempString = tab(tab_count)+line.split(',',-1)[2]+","+line.split(',',-1)[1]+","+line.split(',',-1)[3]+"\n"
		outputFile.write(tempString)
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</coordinates>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</LineString>"+"\n")
	# ! document - raw data - simple plot
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</Placemark>"+"\n")
	# document - evaluated data - simple plot
	outputFile.write(tab(tab_count)+"<Placemark>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<name>data: evaluated</name>"+"\n")
#	outputFile.write(tab(tab_count)+"<description>!!!</description>"+"\n")
	outputFile.write(tab(tab_count)+"<visibility>0</visibility>"+"\n")
	outputFile.write(tab(tab_count)+"<styleUrl>#simpleEvaluated</styleUrl>"+"\n")
	outputFile.write(tab(tab_count)+"<LineString>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<extrude>1</extrude>"+"\n")
	outputFile.write(tab(tab_count)+"<tessellate>0</tessellate>"+"\n")
	outputFile.write(tab(tab_count)+"<altitudeMode>absolute</altitudeMode>"+"\n")
	outputFile.write(tab(tab_count)+"<coordinates>"+"\n")
	tab_count += 1
	# document - evaluated data - simple plot - from csv to kml
	sourceFile_gps.seek(0)
	for line in sourceFile_gps :
		geoid_offset = commands.getoutput("echo " + line.split(',',-1)[1] + " " + line.split(',',-1)[2] + " | GeoidEval -n egm84-15")
		# for google earth: longitude, latitude, elevation
		tempString = tab(tab_count)+line.split(',',-1)[2]+","+line.split(',',-1)[1]+","+str(float(line.split(',',-1)[3]) - float(geoid_offset))+"\n"
		outputFile.write(tempString)
		#print str(float(line.split(',',-1)[3]) - float(geoid_offset))
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</coordinates>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</LineString>"+"\n")
	# ! document - evaluated data - simple plot
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</Placemark>"+"\n")
	# document - used data - simple plot
	outputFile.write(tab(tab_count)+"<Placemark>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<name>data: used</name>"+"\n")
#	outputFile.write(tab(tab_count)+"<description>!!!</description>"+"\n")
	outputFile.write(tab(tab_count)+"<visibility>1</visibility>"+"\n")
	outputFile.write(tab(tab_count)+"<styleUrl>#simpleUsed</styleUrl>"+"\n")
	outputFile.write(tab(tab_count)+"<LineString>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<extrude>1</extrude>"+"\n")
	outputFile.write(tab(tab_count)+"<tessellate>0</tessellate>"+"\n")
	outputFile.write(tab(tab_count)+"<altitudeMode>absolute</altitudeMode>"+"\n")
	outputFile.write(tab(tab_count)+"<coordinates>"+"\n")
	tab_count += 1
	# document - raw data - simple plot - from csv to kml
	sourceFile_gps.seek(0)
	for line in sourceFile_gps :
		# for google earth: longitude, latitude, elevation
		tempString = tab(tab_count)+line.split(',',-1)[2]+","+line.split(',',-1)[1]+","+str(float(line.split(',',-1)[3])-float(line.split(',',-1)[4]))+"\n"
		outputFile.write(tempString)
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</coordinates>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</LineString>"+"\n")
	# ! document - used data - simple plot
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</Placemark>"+"\n")
	# document - folder - 
	outputFile.write(tab(tab_count)+"<Folder>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<name>data: position</name>"+"\n")
#	outputFile.write(tab(tab_count)+"<description>!!!</description>"+"\n")
	outputFile.write(tab(tab_count)+"<visibility>0</visibility>"+"\n")
	outputFile.write(tab(tab_count)+"<open>0</open>"+"\n")
	
	sourceFile_gps.seek(0)
	for line in sourceFile_gps :
		outputFile.write(tab(tab_count)+"<Placemark>"+"\n")
		tab_count += 1
		outputFile.write(tab(tab_count)+"<name></name>"+"\n")
#		outputFile.write(tab(tab_count)+"<description>!!!</description>"+"\n")
		outputFile.write(tab(tab_count)+"<visibility>0</visibility>"+"\n")
		outputFile.write(tab(tab_count)+"<styleUrl>#pointLocation</styleUrl>"+"\n")
		outputFile.write(tab(tab_count)+"<Point>"+"\n")
		tab_count += 1
		outputFile.write(tab(tab_count)+"<extrude>1</extrude>"+"\n")
		#outputFile.write(tab(tab_count)+"<tessellate>0</tessellate>"+"\n")
		outputFile.write(tab(tab_count)+"<altitudeMode>absolute</altitudeMode>"+"\n")
		outputFile.write(tab(tab_count)+"<coordinates>"+"\n")
		tab_count += 1
		tempString = tab(tab_count)+line.split(',',-1)[2]+","+line.split(',',-1)[1]+","+str(float(line.split(',',-1)[3])-float(line.split(',',-1)[4]))+"\n"
		outputFile.write(tempString)
		tab_count -= 1
		outputFile.write(tab(tab_count)+"</coordinates>"+"\n")
		tab_count -= 1
		outputFile.write(tab(tab_count)+"</Point>"+"\n")
		tab_count -= 1
		outputFile.write(tab(tab_count)+"</Placemark>"+"\n")
	
	
	# ! document - folder - 
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</Folder>"+"\n")
	# document - folder - speed line
	outputFile.write(tab(tab_count)+"<Folder>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<name>data: speed</name>"+"\n")
#	outputFile.write(tab(tab_count)+"<description>!!!</description>"+"\n")
	outputFile.write(tab(tab_count)+"<visibility>0</visibility>"+"\n")
	outputFile.write(tab(tab_count)+"<open>0</open>"+"\n")
	
	
	previousSpeed_collor = "-1"
	previousSpeed_data = ""
	
	currentSpeed_color = "-1"
	currentSpeed_data = ""
	
	sourceFile_gps.seek(0)
	for line in sourceFile_gps :
		currentSpeed_color = getSpeedLineColor(float(line.split(',',-1)[6]), speed_slice)
		
		if currentSpeed_color == previousSpeed_collor :	# same speed slice
			# if it's the same, just write data
			currentSpeed_data = line.split(',',-1)[2]+","+line.split(',',-1)[1]+","+"0"
			tempString = tab(tab_count)+currentSpeed_data+"\n"
			outputFile.write(tempString)
			
			previousSpeed_data = currentSpeed_data
			previousSpeed_collor = currentSpeed_color
		else :												# different speed slice
			# first new data == last old data
			if previousSpeed_collor != "-1" :						# finish if we started something
				tempString = tab(tab_count)+line.split(',',-1)[2]+","+line.split(',',-1)[1]+","+"0"+"\n"
				outputFile.write(tempString)
				tab_count -= 1
				outputFile.write(tab(tab_count)+"</coordinates>"+"\n")
				tab_count -= 1
				outputFile.write(tab(tab_count)+"</LineString>"+"\n")
				tab_count -= 1
				outputFile.write(tab(tab_count)+"</Placemark>"+"\n")
			# start new data
			outputFile.write(tab(tab_count)+"<Placemark>"+"\n")
			tab_count += 1
			outputFile.write(tab(tab_count)+"<name>speed: ~"+str(round((float(line.split(',',-1)[6])*3.6),2))+" km/h</name>"+"\n")
#			outputFile.write(tab(tab_count)+"<description>!!!</description>"+"\n")
			outputFile.write(tab(tab_count)+"<visibility>0</visibility>"+"\n")
			outputFile.write(tab(tab_count)+"<styleUrl>#lineSpeed</styleUrl>"+"\n")
			outputFile.write(tab(tab_count)+"<Style>"+"\n")
			tab_count += 1
			outputFile.write(tab(tab_count)+"<LineStyle>"+"\n")
			tab_count += 1
			outputFile.write(tab(tab_count)+"<color>"+currentSpeed_color+"</color>"+"\n")
			tab_count -= 1
			outputFile.write(tab(tab_count)+"</LineStyle>"+"\n")
			tab_count -= 1
			outputFile.write(tab(tab_count)+"</Style>"+"\n")
			outputFile.write(tab(tab_count)+"<LineString>"+"\n")
			tab_count += 1
			outputFile.write(tab(tab_count)+"<extrude>0</extrude>"+"\n")
			outputFile.write(tab(tab_count)+"<tessellate>0</tessellate>"+"\n")
			outputFile.write(tab(tab_count)+"<altitudeMode>clampToGround</altitudeMode>"+"\n")
			outputFile.write(tab(tab_count)+"<coordinates>"+"\n")
			tab_count += 1
			
			currentSpeed_data = line.split(',',-1)[2]+","+line.split(',',-1)[1]+","+"0"
			tempString = tab(tab_count)+currentSpeed_data+"\n"
			outputFile.write(tempString)
			
			previousSpeed_data = currentSpeed_data
			previousSpeed_collor = currentSpeed_color
		
	if previousSpeed_collor != "-1" :						# finish if we started something
		tab_count -= 1
		outputFile.write(tab(tab_count)+"</coordinates>"+"\n")
		tab_count -= 1
		outputFile.write(tab(tab_count)+"</LineString>"+"\n")
		tab_count -= 1
		outputFile.write(tab(tab_count)+"</Placemark>"+"\n")
		
	# ! document - folder - speed line
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</Folder>"+"\n")
	# document - folder - altitude line
	outputFile.write(tab(tab_count)+"<Folder>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<name>data: altitude</name>"+"\n")
#	outputFile.write(tab(tab_count)+"<description>!!!</description>"+"\n")
	outputFile.write(tab(tab_count)+"<visibility>0</visibility>"+"\n")
	outputFile.write(tab(tab_count)+"<open>0</open>"+"\n")
	
	
	previousAltitude_collor = "-1"
	previousAltitude_data = ""
	
	currentAltitude_color = "-1"
	currentAltitude_data = ""
	
	sourceFile_gps.seek(0)
	for line in sourceFile_gps :
		
		currentAltitude_color = getAltitudeLineColor((float(line.split(',',-1)[3])-float(line.split(',',-1)[4]) - altitude_min), altitude_slice)
		
		if currentAltitude_color == previousAltitude_collor :	# same speed slice
			# if it's the same, just write data
			currentAltitude_data = line.split(',',-1)[2]+","+line.split(',',-1)[1]+","+str(float(line.split(',',-1)[3])-float(line.split(',',-1)[4]))
			tempString = tab(tab_count)+currentAltitude_data+"\n"
			outputFile.write(tempString)
			
			previousAltitude_data = currentAltitude_data
			previousAltitude_collor = currentAltitude_color
		else :												# different speed slice
			# first new data == last old data
			if previousAltitude_collor != "-1" :						# finish if we started something
				tempString = tab(tab_count)+line.split(',',-1)[2]+","+line.split(',',-1)[1]+","+str(float(line.split(',',-1)[3])-float(line.split(',',-1)[4]))+"\n"
				outputFile.write(tempString)
				tab_count -= 1
				outputFile.write(tab(tab_count)+"</coordinates>"+"\n")
				tab_count -= 1
				outputFile.write(tab(tab_count)+"</LineString>"+"\n")
				tab_count -= 1
				outputFile.write(tab(tab_count)+"</Placemark>"+"\n")
			# start new data
			outputFile.write(tab(tab_count)+"<Placemark>"+"\n")
			tab_count += 1
			outputFile.write(tab(tab_count)+"<name>altitude: ~"+str(round(float(line.split(',',-1)[3])-float(line.split(',',-1)[4])))+" m</name>"+"\n")
#			outputFile.write(tab(tab_count)+"<description>!!!</description>"+"\n")
			outputFile.write(tab(tab_count)+"<visibility>0</visibility>"+"\n")
			outputFile.write(tab(tab_count)+"<styleUrl>#lineAltitude</styleUrl>"+"\n")
			outputFile.write(tab(tab_count)+"<Style>"+"\n")
			tab_count += 1
			outputFile.write(tab(tab_count)+"<LineStyle>"+"\n")
			tab_count += 1
			outputFile.write(tab(tab_count)+"<color>"+currentAltitude_color+"</color>"+"\n")
			tab_count -= 1
			outputFile.write(tab(tab_count)+"</LineStyle>"+"\n")
			tab_count -= 1
			outputFile.write(tab(tab_count)+"</Style>"+"\n")
			outputFile.write(tab(tab_count)+"<LineString>"+"\n")
			tab_count += 1
			outputFile.write(tab(tab_count)+"<extrude>0</extrude>"+"\n")
			outputFile.write(tab(tab_count)+"<tessellate>0</tessellate>"+"\n")
			outputFile.write(tab(tab_count)+"<altitudeMode>absolute</altitudeMode>"+"\n")
			outputFile.write(tab(tab_count)+"<coordinates>"+"\n")
			tab_count += 1
			
			currentAltitude_data = line.split(',',-1)[2]+","+line.split(',',-1)[1]+","+str(float(line.split(',',-1)[3])-float(line.split(',',-1)[4]))
			tempString = tab(tab_count)+currentAltitude_data+"\n"
			outputFile.write(tempString)
			
			previousAltitude_data = currentAltitude_data
			previousAltitude_collor = currentAltitude_color
		
	if previousAltitude_collor != "-1" :						# finish if we started something
		tab_count -= 1
		outputFile.write(tab(tab_count)+"</coordinates>"+"\n")
		tab_count -= 1
		outputFile.write(tab(tab_count)+"</LineString>"+"\n")
		tab_count -= 1
		outputFile.write(tab(tab_count)+"</Placemark>"+"\n")
		
	# ! document - folder - altitude line
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</Folder>"+"\n")
	# document - folder - range error rings
	outputFile.write(tab(tab_count)+"<Folder>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<name>data: error range</name>"+"\n")
#	outputFile.write(tab(tab_count)+"<description>0 - 25 : green - red</description>"+"\n")
	outputFile.write(tab(tab_count)+"<visibility>0</visibility>"+"\n")
	outputFile.write(tab(tab_count)+"<open>0</open>"+"\n")
	# document - folder - range error rings - polygons
	sourceFile_gps.seek(0)
	for line in sourceFile_gps :
		outputFile.write(tab(tab_count)+"<Placemark>"+"\n")
		tab_count += 1
		
		outputFile.write(tab(tab_count)+"<name>error: "+str(float(line.split(',',-1)[7]))+" m</name>"+"\n")
#		outputFile.write(tab(tab_count)+"<description>!!!</description>"+"\n")
		outputFile.write(tab(tab_count)+"<visibility>0</visibility>"+"\n")
		outputFile.write(tab(tab_count)+"<styleUrl>#errorRangeRing</styleUrl>"+"\n")
		outputFile.write(tab(tab_count)+"<Style>"+"\n")
		tab_count += 1
		outputFile.write(tab(tab_count)+"<LineStyle>"+"\n")
		tab_count += 1
		outputFile.write(tab(tab_count)+"<color>"+getRangeRingErrorColor(float(line.split(',',-1)[7]))+"</color>"+"\n")
		tab_count -= 1
		outputFile.write(tab(tab_count)+"</LineStyle>"+"\n")
		tab_count -= 1
		tab_count += 1
		outputFile.write(tab(tab_count)+"<PolyStyle>"+"\n")
		tab_count += 1
		outputFile.write(tab(tab_count)+"<color>"+getRangeRingErrorColor(float(line.split(',',-1)[7]))+"</color>"+"\n")
		tab_count -= 1
		outputFile.write(tab(tab_count)+"</PolyStyle>"+"\n")
		tab_count -= 1
		outputFile.write(tab(tab_count)+"</Style>"+"\n")
		outputFile.write(tab(tab_count)+"<Polygon>"+"\n")
		tab_count += 1
		outputFile.write(tab(tab_count)+"<extrude>0</extrude>"+"\n")
		outputFile.write(tab(tab_count)+"<tessellate>0</tessellate>"+"\n")
		outputFile.write(tab(tab_count)+"<altitudeMode>clampToGround</altitudeMode>"+"\n")
		outputFile.write(tab(tab_count)+"<outerBoundaryIs>"+"\n")
		tab_count += 1
		outputFile.write(tab(tab_count)+"<LinearRing>"+"\n")
		tab_count += 1
		outputFile.write(tab(tab_count)+"<coordinates>"+"\n")
		tab_count += 1
		
		#rrc = rr.getCoordinates(float(line.split(',',-1)[1]), float(line.split(',',-1)[2]), float(line.split(',',-1)[4]), (float(line.split(',',-1)[7]) / 1000), 72)
		rrc = getRangeRing(float(line.split(',',-1)[1]), float(line.split(',',-1)[2]), float(line.split(',',-1)[5]), float(line.split(',',-1)[7]), 72)
		
		for c in range(0,len(rrc),1) :
			# for google earth: longitude, latitude, elevation
			tempString = tab(tab_count)+str(rrc[c][1])+","+str(rrc[c][0])+","+"0"+"\n"
			outputFile.write(tempString)
		# last coordinate again to connect the circle	
		tempString = tab(tab_count)+str(rrc[0][1])+","+str(rrc[0][0])+","+"0"+"\n"
		outputFile.write(tempString)
		tab_count -= 1
		outputFile.write(tab(tab_count)+"</coordinates>"+"\n")
		tab_count -= 1
		outputFile.write(tab(tab_count)+"</LinearRing>"+"\n")
		tab_count -= 1
		outputFile.write(tab(tab_count)+"</outerBoundaryIs>"+"\n")
		tab_count -= 1
		outputFile.write(tab(tab_count)+"</Polygon>"+"\n")
		
		tab_count -= 1
		outputFile.write(tab(tab_count)+"</Placemark>"+"\n")
	# !document - folder - range error rings - polygons
	# ! document - folder - data
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</Folder>"+"\n")
	# ! document - body
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</Document>"+"\n")
	# !
	tab_count -= 1
	outputFile.write("</kml>"+"\n")
	
	sourceFile_gps.close()
	outputFile.close()
	
	#for filename in os.listdir(inputArgs.sessionFolder[0]):
	#	print  filename

def tab(count) :
	tab = ""
	
	for c in range(0,count,1) :
		tab = tab + "\t"
	
	return tab
	
def getRangeRing(center_lat, center_lon, bearing, radius, point_count) :	# degrees, degrees, degrees, meters, integer
	lat_lon = []
	
	for p in range(0,point_count,1) :
		bearing = bearing + (360 / point_count)
		if bearing > 360 :
			bearing = bearing - 360;
			
		line = Geodesic.WGS84.Line(center_lat, center_lon, bearing)
		point = line.Position(radius)
		
		lat_lon.append([point['lat2'], point['lon2']])
	
	return lat_lon
	
def getRangeRingErrorColor(error) :
	if error <= 0 :
		return "7f00ff00"
	elif error <= 10 :
		color_dec = int(round((error*error*error)*(-6.7441e-02) + (error*error)*(2.2751e+00) + error*9.5106e+00 + (-1.3425e-13)))
		
		if color_dec <= 255 :
			 return "7f00ff"+format(color_dec,'02x')
			#return "7f00ff"+str(hex(color_dec))[2:]	
		elif color_dec <= 511 :
			return "7f00"+format((255-(color_dec-255)),'02x')+"ff"
			#return "7f00"+str(hex( 255-(color_dec-255) ))[2:]+"ff"
		else :
			return "7f00ffff"
	elif error <= 25 :
		color_dec = int(round((error*error)*(-0.46421) + error*31.47631 + (-13.36354)))
		
		if color_dec <= 255 :
			return "7f00ff"+format(color_dec,'02x')
			#return "7f00ff"+str(hex(color_dec))[2:]	
		elif color_dec <= 511 :
			return "7f00"+format((255-(color_dec-255)),'02x')+"ff"
			#return "7f00"+str(hex( 255-(color_dec-255) ))[2:]+"ff"
		else :
			return "7f00ffff"
	else :
		return "7f0000ff"
	
def getSpeedLineColor(speed, speed_slice) :
	#speed_slice = int(round(speed / 0.027126736))	# 1/1024 slice of 0-27.7778 m/s (0-100 km/h) speed range
	#speed_slice = int(round(speed / 0.034722222))	# 1/1024 slice of 0-35.5556 m/s (0-128 km/h) speed range
	
	speed_slice = int(round(speed / speed_slice))	# 1/1024 slice of 0-max speed range
	
	if speed_slice <= 0 :
		return "ffff0000"
	elif speed_slice <= 255 :
		return "ffff"+format(speed_slice,'02x')+"00"
	elif speed_slice <= 511 :
		return "ff"+format((256-(speed_slice-255)),'02x')+"ff00"
		#return "ff"+str(hex( 255-(speed_slice-255) ))[2:]+"ff00"
	elif speed_slice <= 767 :
		return "ff00ff"+format((speed_slice-511),'02x')
		#return "ff00ff"+str(hex(speed_slice-511))[2:]
	elif speed_slice <= 1023 :
		return "ff00"+format((256-(speed_slice-767)),'02x')+"ff"
		#return "ff00"+str(hex( 255-(speed_slice-767) ))[2:]+"ff"
	else :
		return "ff0000ff"
	
def getAltitudeLineColor(altitude, altitude_slice) :
	#altitude_slice = int(round(altitude))	# 1/1024 slice altutide (0-1024 m) altitude range
	
	altitude_slice = int(round(altitude / altitude_slice))	# 1/1024 slice of 0-max altitude range
	
	if altitude_slice <= 0 :
		return "ffff0000"
	elif altitude_slice <= 255 :
		return "ffff"+format(altitude_slice,'02x')+"00"
	elif altitude_slice <= 511 :
		return "ff"+format((256-(altitude_slice-255)),'02x')+"ff00"
		#return "ff"+str(hex( 255-(altitude_slice-255) ))[2:]+"ff00"
	elif altitude_slice <= 767 :
		return "ff00ff"+format((altitude_slice-511),'02x')
		#return "ff00ff"+str(hex(altitude_slice-511))[2:]
	elif altitude_slice <= 1023 :
		return "ff00"+format((256-(altitude_slice-767)),'02x')+"ff"
		#return "ff00"+str(hex( 255-(altitude_slice-767) ))[2:]+"ff"
	else :
		return "ff0000ff"
	
def test() :
	rr = gpsRangeRing.rangeRing()
	#rrc = rr.getCoordinates(45.00, 15.00, 90.0, 0.005, 72)
	rrc = rr.getCoordinates(46.5556925023, 15.6349965837, 90.0, 0.005, 72)
	for i in range(0, len(rrc), 1) :
		print "%f,%f,0" % (rrc[i][1],rrc[i][0])
	print "%f,%f,0" % (rrc[0][1],rrc[0][0])
	
if __name__ == "__main__":			# this is not a module
	
	parseInput()	# what do we have to do
	
	processInput()	# doing what we have to do
	#test()
	print ""		# for estetic output

