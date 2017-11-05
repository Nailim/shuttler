#!/usr/bin/env python

import sys
import socket
import getopt
import string

# Just a couple of helpful 
def help():
	print "Usage: python gpsTrackingClient.py [OPTIONS]"
	print ""
	print "   -h, --help           this menu"
	print "   -a, --address [IP]   IP address of the server"
	print "   -s, --style [STYLE]  style of tracking (point, trail, line)"
	print "   -t, --trail [LENGTH] length of trail"
	print "   -v, --version        print program version"
	print ""

def version():
	print ""
	print "gpsTrackingClient 0.8 - cleaned up edition"
	print ""

# The main body of the code
def main(argv):
	
	# Setting default values
	hostAddress = []
	hostPort = 4646
	
	dataFilename = "/dev/shm/gpsData.kml"
	trackingStyle = "point"
	
	trailLength = 10
	trailData = []
	
	# Getting and checking arguments	
	try:                                
		opts, args = getopt.getopt(argv, "h:a:s:t:v", ["help", "address=", "style=", "trail=", "version"])
	except getopt.GetoptError, (value, message):
		help()
		sys.exit(1)
	
	# Parsing trough and acting upon arguments
	for opt, arg in opts:
		if opt in ("-h", "--help"):
			help()
			sys.exit()
		elif opt in ("-a", "--address"):
			hostAddress = arg
		elif opt in ("-s", "--style"):
			trackingStyle = arg
		elif opt in ("-t", "--trail"):
			trailLength = arg
		elif opt in ("-v", "--version"):
			version()
			sys.exit()
	
	# Connecting to telemetry data source
	host = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	# ... this part could go in another loop for automatic reconnection? (work on it)
	try:	
		host.connect((hostAddress, hostPort))
	except socket.error, (value, message):
		print message
		sys.exit(2)
	
	while 1:
		data = host.recv(512)
		if data:
		
			# Wafles
			#print 'Received:', data
			
			values = data.split(";")
		
			speed = (string.atof(values[3]) * 3600 / 1000)
			
			range = 330
			tilt = 30
			heading = values[5]
			
			kmlHead = '<?xml version="1.0" encoding="UTF-8"?>\n<kml xmlns="http://earth.google.com/kml/2.0">\n<Document>\n'
			
			kmlBody = (
					'\t<Placemark>\n'
					'\t\t<name></name>\n'
					'\t\t<LookAt>\n'
					'\t\t\t<longitude>%s</longitude>\n'
					'\t\t\t<latitude>%s</latitude>\n'
					'\t\t\t<range>%s</range>\n'
					'\t\t\t<tilt>%s</tilt>\n'
					'\t\t\t<heading>%s</heading>\n'
					'\t\t</LookAt>\n'
					'\t\t<Style>\n'
				 	'\t\t\t<IconStyle>\n'
				 	'\t\t\t\t<Icon>\n'
				 	'\t\t\t\t\t<href>http://maps.google.com/mapfiles/kml/shapes/arrow.png</href>\n'
				 	'\t\t\t\t</Icon>\n'
				 	'\t\t\t\t<hotSpot x="32" y="2" xunits="pixels" yunits="pixels"/>\n'
				 	'\t\t\t</IconStyle>\n'
					'\t\t</Style>\n'
					'\t\t<Point>\n'
					'\t\t\t<extrude>1</extrude>\n'
					'\t\t\t<tessellate>1</tessellate>\n'
					'\t\t\t<altitudeMode>absolute</altitudeMode>\n'
					'\t\t\t<coordinates>%s,%s,%s</coordinates>\n'
					'\t\t</Point>\n'
					'\t</Placemark>\n'
					) % (values[7], values[8], range, tilt, heading, values[7], values[8], values[2])
			
			if trackingStyle == "point":
				kmlBody = kmlBody + (
					'\t<Placemark>\n'
					'\t\t<name>%s | %.2f km/h - %.2f m</name>\n'
					'\t\t<LookAt>\n'
					'\t\t\t<longitude>%s</longitude>\n'
					'\t\t\t<latitude>%s</latitude>\n'
					'\t\t\t<range>%s</range>\n'
					'\t\t\t<tilt>%s</tilt>\n'
					'\t\t\t<heading>%s</heading>\n'
					'\t\t</LookAt>\n'
					'\t\t<Style>\n'
				 	'\t\t\t<IconStyle>\n'
				 	'\t\t\t\t<Icon>\n'
				 	'\t\t\t\t\t<href>http://maps.google.com/mapfiles/kml/paddle/red-stars.png</href>\n'
				 	'\t\t\t\t</Icon>\n'
				 	'\t\t\t\t<hotSpot x="32" y="2" xunits="pixels" yunits="pixels"/>\n'
				 	'\t\t\t</IconStyle>\n'
					'\t\t</Style>\n'
					'\t\t<Point>\n'
					'\t\t\t<extrude>1</extrude>\n'
					'\t\t\t<tessellate>1</tessellate>\n'
					'\t\t\t<altitudeMode>absolute</altitudeMode>\n'
					'\t\t\t<coordinates>%s,%s,%s</coordinates>\n'
					'\t\t</Point>\n'
					'\t</Placemark>\n'
					) % (values[6], speed, float(values[2]), values[0], values[1], range, tilt, heading, values[0], values[1], values[2])
			elif trackingStyle == "trail":
				if len(trailData) < int(trailLength):
					trailData.append(values[0] + ',' + values[1] + ',' + values[2])
				else:
					del trailData[0]
					trailData.append(values[0] + ',' + values[1] + ',' + values[2])
				
				#kmlBody = ""
				
				kmlBody = kmlBody + '\t<Placemark>\n'
				
				kmlBody = kmlBody + '\t\t<Style>\n'
				kmlBody = kmlBody + '\t\t\t<LineStyle>\n'
				kmlBody = kmlBody + '\t\t\t\t<color>7f00ffff</color>\n'
				kmlBody = kmlBody + '\t\t\t\t<width>4</width>\n'
				kmlBody = kmlBody + '\t\t\t</LineStyle>\n'
				kmlBody = kmlBody + '\t\t\t<PolyStyle>\n'
				kmlBody = kmlBody + '\t\t\t\t<color>7f00ff00</color>\n'
				kmlBody = kmlBody + '\t\t\t</PolyStyle>\n'
				kmlBody = kmlBody + '\t\t</Style>\n'
				kmlBody = kmlBody + '\t\t<LineString>\n'
				kmlBody = kmlBody + '\t\t\t<extrude>1</extrude>\n'
				kmlBody = kmlBody + '\t\t\t<tessellate>1</tessellate>\n'
				kmlBody = kmlBody + '\t\t\t<altitudeMode>absolute</altitudeMode>\n'
				kmlBody = kmlBody + '\t\t\t<coordinates>\n'
				
				for item in trailData:
					kmlBody = kmlBody + '\t\t\t\t' + item + '\n'
				
				kmlBody = kmlBody + '\t\t\t</coordinates>\n'
				kmlBody = kmlBody + '\t\t</LineString>\n'
				kmlBody = kmlBody + '\t</Placemark>\n'
				
				kmlBody = kmlBody + (
					'\t<Placemark>\n'
					'\t\t<name>%s | %.2f km/h - %.2f m</name>\n'
					'\t\t<LookAt>\n'
					'\t\t\t<longitude>%s</longitude>\n'
					'\t\t\t<latitude>%s</latitude>\n'
					'\t\t\t<range>%s</range>\n'
					'\t\t\t<tilt>%s</tilt>\n'
					'\t\t\t<heading>%s</heading>\n'
					'\t\t</LookAt>\n'
					'\t\t<Style>\n'
				 	'\t\t\t<IconStyle>\n'
				 	'\t\t\t\t<Icon>\n'
				 	'\t\t\t\t\t<href>http://maps.google.com/mapfiles/kml/paddle/red-stars.png</href>\n'
				 	'\t\t\t\t</Icon>\n'
				 	'\t\t\t\t<hotSpot x="32" y="2" xunits="pixels" yunits="pixels"/>\n'
				 	'\t\t\t</IconStyle>\n'
					'\t\t</Style>\n'
					'\t\t<Point>\n'
					'\t\t\t<extrude>0</extrude>\n'
					'\t\t\t<tessellate>0</tessellate>\n'
					'\t\t\t<altitudeMode>absolute</altitudeMode>\n'
					'\t\t\t<coordinates>%s,%s,%s</coordinates>\n'
					'\t\t</Point>\n'
					'\t</Placemark>\n'
					) % (values[6], speed, float(values[2]), values[0], values[1], range, tilt, heading, values[0], values[1], values[2])
			elif trackingStyle == "line":
				trailData.append(values[0] + ',' + values[1] + ',' + values[2])
				#kmlBody = ""
				
				kmlBody = kmlBody + '\t<Placemark>\n'
				
				kmlBody = kmlBody + '\t\t<Style>\n'
				kmlBody = kmlBody + '\t\t\t<LineStyle>\n'
				kmlBody = kmlBody + '\t\t\t\t<color>c0088ff0</color>\n'
				kmlBody = kmlBody + '\t\t\t\t<width>7</width>\n'
				kmlBody = kmlBody + '\t\t\t</LineStyle>\n'
				kmlBody = kmlBody + '\t\t\t<PolyStyle>\n'
				kmlBody = kmlBody + '\t\t\t\t<color>c0088ff0</color>\n'
				kmlBody = kmlBody + '\t\t\t</PolyStyle>\n'
				kmlBody = kmlBody + '\t\t</Style>\n'
				kmlBody = kmlBody + '\t\t<LineString>\n'
				kmlBody = kmlBody + '\t\t\t<tessellate>1</tessellate>\n'
				kmlBody = kmlBody + '\t\t\t<altitudeMode>absolute</altitudeMode>\n'
				kmlBody = kmlBody + '\t\t\t<coordinates>\n'
				
				for item in trailData:
					kmlBody = kmlBody + '\t\t\t\t' + item + '\n'
				
				kmlBody = kmlBody + '\t\t\t</coordinates>\n'
				kmlBody = kmlBody + '\t\t</LineString>\n'
				kmlBody = kmlBody + '\t</Placemark>\n'
				
				kmlBody = kmlBody + '\t<Placemark>\n'
				
				kmlBody = kmlBody + '\t\t<LineString>\n'
				kmlBody = kmlBody + '\t\t\t<tessellate>1</tessellate>\n'
				kmlBody = kmlBody + '\t\t\t<coordinates>\n'
				
				for item in trailData:
					kmlBody = kmlBody + '\t\t\t\t' + item + '\n'
				
				kmlBody = kmlBody + '\t\t\t</coordinates>\n'
				kmlBody = kmlBody + '\t\t</LineString>\n'
				kmlBody = kmlBody + '\t</Placemark>\n'
				
				kmlBody = kmlBody + (
					'\t<Placemark>\n'
					'\t\t<name>$s | %.2f km/h - %.2f m</name>\n'
					'\t\t<LookAt>\n'
					'\t\t\t<longitude>%s</longitude>\n'
					'\t\t\t<latitude>%s</latitude>\n'
					'\t\t\t<range>%s</range>\n'
					'\t\t\t<tilt>%s</tilt>\n'
					'\t\t\t<heading>%s</heading>\n'
					'\t\t</LookAt>\n'
					'\t\t<Style>\n'
				 	'\t\t\t<IconStyle>\n'
				 	'\t\t\t\t<Icon>\n'
				 	'\t\t\t\t\t<href>http://maps.google.com/mapfiles/kml/paddle/red-stars.png</href>\n'
				 	'\t\t\t\t</Icon>\n'
				 	'\t\t\t\t<hotSpot x="32" y="2" xunits="pixels" yunits="pixels"/>\n'
				 	'\t\t\t</IconStyle>\n'
					'\t\t</Style>\n'
					'\t\t<Point>\n'
					'\t\t\t<extrude>1</extrude>\n'
					'\t\t\t<tessellate>1</tessellate>\n'
					'\t\t\t<altitudeMode>absolute</altitudeMode>\n'
					'\t\t\t<coordinates>%s,%s,%s</coordinates>\n'
					'\t\t</Point>\n'
					'\t</Placemark>\n'
					) % (values[6], speed, float(values[2]), values[0], values[1], range, tilt, heading, values[0], values[1], values[2])
			
			kmlTail = '</Document>\n</kml>'
			
			f=open(dataFilename, 'w')
			f.write(kmlHead + kmlBody + kmlTail)
			f.close()
			
		else:
			break
	host.close()

if __name__ == "__main__": main(sys.argv[1:]) 
