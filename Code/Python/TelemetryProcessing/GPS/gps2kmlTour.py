#import sys
#import os
from datetime import datetime

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
	outputFile = open(inputArgs.sessionFolder[0]+"_processed/logs/gpsTour.kml", 'w')
	
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
	
	# document - metadata - style altitude line
	outputFile.write(tab(tab_count)+"<Style id=\"lineTour\">"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<LineStyle>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<width>1</width>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</LineStyle>"+"\n")
	outputFile.write(tab(tab_count)+"<LabelStyle>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<scale>0</scale>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</LabelStyle>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</Style>"+"\n")
	
	# document - track raw
	outputFile.write(tab(tab_count)+"<Placemark>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<name>data: raw</name>"+"\n")
	outputFile.write(tab(tab_count)+"<visibility>0</visibility>"+"\n")
	outputFile.write(tab(tab_count)+"<styleUrl>#lineTour</styleUrl>"+"\n")
	# document - track - icon
	outputFile.write(tab(tab_count)+"<Style>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<Icon>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<href>http://maps.google.com/mapfiles/kml/shapes/airports.png</href>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</Icon>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</Style>"+"\n")
	# document - track - track
	outputFile.write(tab(tab_count)+"<gx:Track>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<altitudeMode>absolute</altitudeMode>"+"\n")
	# document - track - track - when
	sourceFile_gps.seek(0)
	for line in sourceFile_gps :
		# for google earth: <when></when>
		tempString = tab(tab_count) + "<when>" + datetime.fromtimestamp(float(line.split(',',-1)[0][:10])).strftime('%Y-%m-%dT%H:%M:%SZ') + "</when>" + "\n"
		outputFile.write(tempString)
	# document - track - track - angles
	sourceFile_gps.seek(0)
	for line in sourceFile_gps :
		geoid_offset = commands.getoutput("echo " + line.split(',',-1)[1] + " " + line.split(',',-1)[2] + " | GeoidEval -n egm84-15")
		# for google earth: <gx:coord></gx:coord>
		tempString = tab(tab_count)+"<gx:coord>"+line.split(',',-1)[2]+" "+line.split(',',-1)[1]+" "+str(float(line.split(',',-1)[3]) - float(geoid_offset))+"</gx:coord>"+"\n"
		outputFile.write(tempString)
	# document - track - track - when
	sourceFile_gps.seek(0)
	for line in sourceFile_gps :
		# for google earth: <gx:angles></gx:angles>
		tempString = tab(tab_count)+"<gx:angles>"+line.split(',',-1)[5]+" "+"0"+" "+"0"+"</gx:angles>"+"\n"
		outputFile.write(tempString)
	# document - track - track - coord
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</gx:Track>"+"\n")
	# ! document - track
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</Placemark>"+"\n")
	
	# document - track evaluated
	outputFile.write(tab(tab_count)+"<Placemark>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<name>data: evaluated</name>"+"\n")
	outputFile.write(tab(tab_count)+"<visibility>0</visibility>"+"\n")
	outputFile.write(tab(tab_count)+"<styleUrl>#lineTour</styleUrl>"+"\n")
	# document - track - icon
	outputFile.write(tab(tab_count)+"<Style>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<Icon>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<href>http://maps.google.com/mapfiles/kml/shapes/airports.png</href>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</Icon>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</Style>"+"\n")
	# document - track - track
	outputFile.write(tab(tab_count)+"<gx:Track>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<altitudeMode>absolute</altitudeMode>"+"\n")
	# document - track - track - when
	sourceFile_gps.seek(0)
	for line in sourceFile_gps :
		# for google earth: <when></when>
		tempString = tab(tab_count) + "<when>" + datetime.fromtimestamp(float(line.split(',',-1)[0][:10])).strftime('%Y-%m-%dT%H:%M:%SZ') + "</when>" + "\n"
		outputFile.write(tempString)
	# document - track - track - angles
	sourceFile_gps.seek(0)
	for line in sourceFile_gps :
		geoid_offset = commands.getoutput("echo " + line.split(',',-1)[1] + " " + line.split(',',-1)[2] + " | GeoidEval -n egm84-15")
		# for google earth: <gx:coord></gx:coord>
		tempString = tab(tab_count)+"<gx:coord>"+line.split(',',-1)[2]+" "+line.split(',',-1)[1]+" "+str(float(line.split(',',-1)[3]) - float(geoid_offset))+"</gx:coord>"+"\n"
		outputFile.write(tempString)
	# document - track - track - when
	sourceFile_gps.seek(0)
	for line in sourceFile_gps :
		# for google earth: <gx:angles></gx:angles>
		tempString = tab(tab_count)+"<gx:angles>"+line.split(',',-1)[5]+" "+"0"+" "+"0"+"</gx:angles>"+"\n"
		outputFile.write(tempString)
	# document - track - track - coord
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</gx:Track>"+"\n")
	# ! document - track
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</Placemark>"+"\n")
	
	# document - track used
	outputFile.write(tab(tab_count)+"<Placemark>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<name>data: used</name>"+"\n")
	outputFile.write(tab(tab_count)+"<visibility>1</visibility>"+"\n")
	outputFile.write(tab(tab_count)+"<styleUrl>#lineTour</styleUrl>"+"\n")
	# document - track - icon
	outputFile.write(tab(tab_count)+"<Style>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<Icon>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<href>http://maps.google.com/mapfiles/kml/shapes/airports.png</href>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</Icon>"+"\n")
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</Style>"+"\n")
	# document - track - track
	outputFile.write(tab(tab_count)+"<gx:Track>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<altitudeMode>absolute</altitudeMode>"+"\n")
	# document - track - track - when
	sourceFile_gps.seek(0)
	for line in sourceFile_gps :
		# for google earth: <when></when>
		tempString = tab(tab_count) + "<when>" + datetime.fromtimestamp(float(line.split(',',-1)[0][:10])).strftime('%Y-%m-%dT%H:%M:%SZ') + "</when>" + "\n"
		outputFile.write(tempString)
	# document - track - track - angles
	sourceFile_gps.seek(0)
	for line in sourceFile_gps :
		geoid_offset = commands.getoutput("echo " + line.split(',',-1)[1] + " " + line.split(',',-1)[2] + " | GeoidEval -n egm84-15")
		# for google earth: <gx:coord></gx:coord>
		tempString = tab(tab_count)+"<gx:coord>"+line.split(',',-1)[2]+" "+line.split(',',-1)[1]+" "+str(float(line.split(',',-1)[3]) - float(line.split(',',-1)[4]))+"</gx:coord>"+"\n"
		outputFile.write(tempString)
	# document - track - track - when
	sourceFile_gps.seek(0)
	for line in sourceFile_gps :
		# for google earth: <gx:angles></gx:angles>
		tempString = tab(tab_count)+"<gx:angles>"+line.split(',',-1)[5]+" "+"0"+" "+"0"+"</gx:angles>"+"\n"
		outputFile.write(tempString)
	# document - track - track - coord
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</gx:Track>"+"\n")
	# ! document - track
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</Placemark>"+"\n")
	
	# document - tour - top
	outputFile.write(tab(tab_count)+"<gx:Tour>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<name>tour: top</name>"+"\n")
	outputFile.write(tab(tab_count)+"<visibility>0</visibility>"+"\n")
	outputFile.write(tab(tab_count)+"<gx:Playlist>"+"\n")
	tab_count += 1
	
	# magic
	durationCounter = 0.0
	timeStamp = 0
	timeStampFirst = 0
	sourceFile_gps.seek(0)
	for line in sourceFile_gps :
		timeStamp = line.split(',',-1)[0]
		timeStampFirst = line.split(',',-1)[0]
		break
	sourceFile_gps.seek(0)
	for line in sourceFile_gps :
		durationCounter = (float(line.split(',',-1)[0]) - float(timeStamp)) / 1000
		timeStamp = line.split(',',-1)[0]
		
		outputFile.write(tab(tab_count)+"<gx:FlyTo>"+"\n")
		tab_count += 1
		outputFile.write(tab(tab_count)+"<gx:duration>"+str(durationCounter)+"</gx:duration>"+"\n")
		outputFile.write(tab(tab_count)+"<gx:flyToMode>smooth</gx:flyToMode>"+"\n")
		outputFile.write(tab(tab_count)+"<Camera>"+"\n")
		tab_count += 1
		outputFile.write(tab(tab_count)+"<gx:TimeSpan>"+"\n")
		tab_count += 1
		outputFile.write(tab(tab_count)+"<begin>"+datetime.fromtimestamp(float(timeStampFirst[:10])).strftime('%Y-%m-%dT%H:%M:%SZ')+"</begin>"+"\n")
		outputFile.write(tab(tab_count)+"<end>" + datetime.fromtimestamp(float(line.split(',',-1)[0][:10])).strftime('%Y-%m-%dT%H:%M:%SZ') + "</end>"+"\n")
		tab_count -= 1
		outputFile.write(tab(tab_count)+"</gx:TimeSpan>"+"\n")
		outputFile.write(tab(tab_count)+"<altitudeMode>absolute</altitudeMode>"+"\n")
		outputFile.write(tab(tab_count)+"<longitude>"+line.split(',',-1)[2]+"</longitude>"+"\n")
		outputFile.write(tab(tab_count)+"<latitude>"+line.split(',',-1)[1]+"</latitude>"+"\n")
		outputFile.write(tab(tab_count)+"<altitude>"+str(float(line.split(',',-1)[3])+1000.0)+"</altitude>"+"\n")
		#outputFile.write(tab(tab_count)+"<heading>"+line.split(',',-1)[5]+"</heading>"+"\n")
		outputFile.write(tab(tab_count)+"<heading>"+str(0)+"</heading>"+"\n")
		outputFile.write(tab(tab_count)+"<tilt>"+str(0.0)+"</tilt>"+"\n")
		outputFile.write(tab(tab_count)+"<roll>"+str(0.0)+"</roll>"+"\n")
		
		tab_count -= 1
		outputFile.write(tab(tab_count)+"</Camera>"+"\n")
		tab_count -= 1
		outputFile.write(tab(tab_count)+"</gx:FlyTo>"+"\n")
		
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</gx:Playlist>"+"\n")
	# ! document - tour
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</gx:Tour>"+"\n")
	
	# document - tour - ground
	outputFile.write(tab(tab_count)+"<gx:Tour>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<name>tour: ground</name>"+"\n")
	outputFile.write(tab(tab_count)+"<visibility>0</visibility>"+"\n")
	outputFile.write(tab(tab_count)+"<gx:Playlist>"+"\n")
	tab_count += 1
	
	# magic
	durationCounter = 0.0
	timeStamp = 0
	timeStampFirst = 0
	cbf = []
	sourceFile_gps.seek(0)
	for line in sourceFile_gps :
		timeStamp = line.split(',',-1)[0]
		timeStampFirst = line.split(',',-1)[0]
		#firstLatitude = line.split(',',-1)[1]
		#firstLongitude = line.split(',',-1)[2]
		heading = float(line.split(',',-1)[5]) + 180.0
		if heading > 360.0 :
			heading = heading - 360
		cfb = getCoordinateFromBearing(float(line.split(',',-1)[1]), float(line.split(',',-1)[2]), heading, 10)
		break
	
	sourceFile_gps.seek(0)
	for line in sourceFile_gps :
		durationCounter = (float(line.split(',',-1)[0]) - float(timeStamp)) / 1000
		timeStamp = line.split(',',-1)[0]
		
		inverse = Geodesic.WGS84.Inverse(float(cfb[0][0]), float(cfb[0][1]), float(line.split(',',-1)[1]), float(line.split(',',-1)[2]))
		
		outputFile.write(tab(tab_count)+"<gx:FlyTo>"+"\n")
		tab_count += 1
		outputFile.write(tab(tab_count)+"<gx:duration>"+str(durationCounter)+"</gx:duration>"+"\n")
		outputFile.write(tab(tab_count)+"<gx:flyToMode>smooth</gx:flyToMode>"+"\n")
		outputFile.write(tab(tab_count)+"<Camera>"+"\n")
		tab_count += 1
		outputFile.write(tab(tab_count)+"<gx:TimeSpan>"+"\n")
		tab_count += 1
		outputFile.write(tab(tab_count)+"<begin>"+datetime.fromtimestamp(float(timeStampFirst[:10])).strftime('%Y-%m-%dT%H:%M:%SZ')+"</begin>"+"\n")
		outputFile.write(tab(tab_count)+"<end>" + datetime.fromtimestamp(float(line.split(',',-1)[0][:10])).strftime('%Y-%m-%dT%H:%M:%SZ') + "</end>"+"\n")
		tab_count -= 1
		outputFile.write(tab(tab_count)+"</gx:TimeSpan>"+"\n")
		outputFile.write(tab(tab_count)+"<altitudeMode>relativeToGround</altitudeMode>"+"\n")
		outputFile.write(tab(tab_count)+"<longitude>"+str(cfb[0][1])+"</longitude>"+"\n")
		outputFile.write(tab(tab_count)+"<latitude>"+str(cfb[0][0])+"</latitude>"+"\n")
		outputFile.write(tab(tab_count)+"<altitude>"+str(1.0)+"</altitude>"+"\n")
		outputFile.write(tab(tab_count)+"<heading>"+str(inverse['azi1'])+"</heading>"+"\n")
		outputFile.write(tab(tab_count)+"<tilt>"+str(100.0)+"</tilt>"+"\n")
		outputFile.write(tab(tab_count)+"<roll>"+str(0.0)+"</roll>"+"\n")
		
		#tempString = tab(tab_count)+"<gx:angles>"+line.split(',',-1)[5]+" "+"0"+" "+"0"+"</gx:angles>"+"\n"
		#outputFile.write(tempString)
		
		tab_count -= 1
		outputFile.write(tab(tab_count)+"</Camera>"+"\n")
		tab_count -= 1
		outputFile.write(tab(tab_count)+"</gx:FlyTo>"+"\n")
		
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</gx:Playlist>"+"\n")
	# ! document - tour
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</gx:Tour>"+"\n")
	
	# document - tour - follow
	outputFile.write(tab(tab_count)+"<gx:Tour>"+"\n")
	tab_count += 1
	outputFile.write(tab(tab_count)+"<name>tour: follow</name>"+"\n")
	outputFile.write(tab(tab_count)+"<visibility>1</visibility>"+"\n")
	outputFile.write(tab(tab_count)+"<gx:Playlist>"+"\n")
	tab_count += 1
	
	# magic
	durationCounter = 0.0
	timeStamp = 0
	timeStampFirst = 0
	sourceFile_gps.seek(0)
	for line in sourceFile_gps :
		timeStamp = line.split(',',-1)[0]
		timeStampFirst = line.split(',',-1)[0]
		break
	sourceFile_gps.seek(0)
	for line in sourceFile_gps :
		durationCounter = (float(line.split(',',-1)[0]) - float(timeStamp)) / 1000
		timeStamp = line.split(',',-1)[0]
		
		outputFile.write(tab(tab_count)+"<gx:FlyTo>"+"\n")
		tab_count += 1
		outputFile.write(tab(tab_count)+"<gx:duration>"+str(durationCounter)+"</gx:duration>"+"\n")
		outputFile.write(tab(tab_count)+"<gx:flyToMode>smooth</gx:flyToMode>"+"\n")
		outputFile.write(tab(tab_count)+"<Camera>"+"\n")
		tab_count += 1
		outputFile.write(tab(tab_count)+"<gx:TimeSpan>"+"\n")
		tab_count += 1
		outputFile.write(tab(tab_count)+"<begin>"+datetime.fromtimestamp(float(timeStampFirst[:10])).strftime('%Y-%m-%dT%H:%M:%SZ')+"</begin>"+"\n")
		outputFile.write(tab(tab_count)+"<end>" + datetime.fromtimestamp(float(line.split(',',-1)[0][:10])).strftime('%Y-%m-%dT%H:%M:%SZ') + "</end>"+"\n")
		tab_count -= 1
		outputFile.write(tab(tab_count)+"</gx:TimeSpan>"+"\n")
		outputFile.write(tab(tab_count)+"<altitudeMode>absolute</altitudeMode>"+"\n")
		# calculate point behind the plane
		heading = float(line.split(',',-1)[5]) + 180.0
		if heading > 360.0 :
			heading = heading - 360;
		cfb = getCoordinateFromBearing(float(line.split(',',-1)[1]), float(line.split(',',-1)[2]), heading, 500)
		
		outputFile.write(tab(tab_count)+"<longitude>"+str(cfb[0][1])+"</longitude>"+"\n")
		outputFile.write(tab(tab_count)+"<latitude>"+str(cfb[0][0])+"</latitude>"+"\n")
		outputFile.write(tab(tab_count)+"<altitude>"+str(float(line.split(',',-1)[3])+500.0)+"</altitude>"+"\n")
		outputFile.write(tab(tab_count)+"<heading>"+line.split(',',-1)[5]+"</heading>"+"\n")
		outputFile.write(tab(tab_count)+"<tilt>"+str(45.0)+"</tilt>"+"\n")
		outputFile.write(tab(tab_count)+"<roll>"+str(0.0)+"</roll>"+"\n")
		
		tab_count -= 1
		outputFile.write(tab(tab_count)+"</Camera>"+"\n")
		tab_count -= 1
		outputFile.write(tab(tab_count)+"</gx:FlyTo>"+"\n")
		
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</gx:Playlist>"+"\n")
	# ! document - tour
	tab_count -= 1
	outputFile.write(tab(tab_count)+"</gx:Tour>"+"\n")
	
	
	
	# ! document
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
	
def getCoordinateFromBearing(center_lat, center_lon, bearing, distance) :	# degrees, degrees, degrees, meters
	lat_lon = []
			
	line = Geodesic.WGS84.Line(center_lat, center_lon, bearing)
	point = line.Position(distance)
	
	lat_lon.append([point['lat2'], point['lon2']])
	
	return lat_lon
	
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
	
if __name__ == "__main__":			# this is not a module
	
	parseInput()	# what do we have to do
	
	processInput()	# doing what we have to do
	#test()
	print ""		# for estetic output

