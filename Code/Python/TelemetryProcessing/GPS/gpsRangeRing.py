# based on http://www.movable-type.co.uk/scripts/latlong.html
# based on http://www.barnabu.co.uk/geapi/polyplot/

import math

class rangeRing:
	
	def __init__(self) :
		pass
		
	def getCoordinates(self, center_lat, center_lon, bearing, radius, point_count) :	# degrees, degrees, degrees, meters, integers
		
		lat_lon = []
		
		center_lat_rad = (center_lat * math.pi) / 180	# deg to rad conversion
		center_lon_rad = (center_lon * math.pi) / 180	# deg to rad conversion
		
		radius_lon_correction_factor = 1.0#0.8	# earth is not round
		radius_lat_correction_factor = 1.0#1.0006	# earth is not round
		
		for p in range(0,point_count,1) :			
			bering_rad = ((bearing*math.pi) / 180) + p * 2.0 * math.pi / point_count
			
			lat_rad = math.asin( math.sin(center_lat_rad) * math.cos((radius*radius_lat_correction_factor)/6371) + math.cos(center_lat_rad) * math.sin(radius/6371) * math.cos(bering_rad) )
			lon_rad = center_lon_rad + math.atan2( math.sin(bering_rad) * math.sin((radius*radius_lon_correction_factor)/6371) * math.cos(center_lat_rad),  math.cos((radius*radius_lon_correction_factor)/6371) - math.sin(center_lat) * math.sin(lat_rad) )
			
			lat_deg = 180 * lat_rad / math.pi	# rad to deg
			lon_deg = 180 * lon_rad / math.pi	# rad to deg
			
			lat_lon.append([lat_deg, lon_deg])
		return lat_lon
