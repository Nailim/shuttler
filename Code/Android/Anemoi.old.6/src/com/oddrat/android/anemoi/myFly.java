package com.oddrat.android.anemoi;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.util.Log;

public class myFly {
	
	// horrible mess, half is here half is in autopilot service //
	
	public class WayPoint {
		public int type;		// 0 = fly | 1 = level | 2 = loiter
		
		public double lat;
		public double lon;
		
		public double alt;
		public double altDiff;
		
		public int dir;			// -1 = left | 0 = closest | 1 = right
		
		public double distAcq;
		public double altAcq;
		
		public int louterCount;
		
		public int next;
		
		public WayPoint() {
			this.type = 0;
			
			this.lat = 0.0;
			this.lon = 0.0;
			
			this.alt = 0.0;
			this.altDiff = 0.0;
			
			this.dir = 0;
			
			this.distAcq = 50.0;
			this.altAcq = 50.0;
			
			this.louterCount = 1;
			
			this.next = -1;
		}
	}
	
	public class Envelope {
		
		public double roll;
		public double pitch;
		
		public double ceiling;
		
		public double topSpeed;
		public double stallSpeed;
		public double cruiseSpeed;
		
		public double topThrottle;
		public double stallThrottle;
		public double cruiseThrottle;
		
		
		public Envelope() {
			this.roll = -1.0;
			this.pitch = -1.0;
			
			this.ceiling = -1.0;
			
			this.topSpeed = -1.0;
			this.stallSpeed = -1.0;
			this.cruiseSpeed = -1.0;
			
			this.topThrottle = -1.0;
			this.stallThrottle = -1.0;
			this.cruiseThrottle = -1.0;
		}
	}
	
	myFly() {
	}
	
	public ArrayList<WayPoint> returnFlightPlan(String flightPlan) {
		
		ArrayList<WayPoint> wpList = new ArrayList<myFly.WayPoint>();	// waypoint list
		
		// this is done badly, can only reference wp that was already read
		Map<String, Integer> wpID = new HashMap<String, Integer>();		// waypoint id to number map
		
		int wpCount = 0;												// waypoint count(start from zero)
		
		String filePath = "";
		
		Log.i("anemoi", "myfly return: " + flightPlan);
		
		// check if the path starts with file:/// or /
		if (flightPlan.contains(":///")) {
			String[] separated = flightPlan.split("://");
			filePath = separated[1];
		} else {
			filePath = flightPlan;
		}
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		Document doc;
		
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(new File(filePath));
		} catch (Exception e) {
			Log.i("anemoi", "myfly error " + e.getMessage());
			return null;
		}
		
		NodeList nFPList = doc.getElementsByTagName("path");
		Log.i("anemoi", "myfly length: " + nFPList.getLength());
		
		for (int nfp = 0; nfp < nFPList.getLength(); nfp++) {
			Node nFPNode = nFPList.item(nfp);
			if (nFPNode.getNodeType() == Node.ELEMENT_NODE) {
				NodeList nFPChildren = nFPNode.getChildNodes();
				for (int nfpc = 0; nfpc < nFPChildren.getLength(); nfpc++) {
					Node nFPChild = nFPChildren.item(nfpc);
					
					if (nFPChild.getNodeType() == Node.ELEMENT_NODE) {
						Element eFPC = (Element) nFPChild;
						
						WayPoint tmpWP = new WayPoint();
						
						// parse waypoint ...
						
						wpID.put(eFPC.getElementsByTagName("id").item(0).getTextContent(), wpCount);
						
						// int type		// 0 = fly | 1 = level | 2 = loiter
						if (eFPC.getElementsByTagName("type").item(0).getTextContent().equals("loiter")) {
							tmpWP.type = 2;
						} else if (eFPC.getElementsByTagName("id").item(0).getTextContent().equals("level")) {
							tmpWP.type = 1;
						} else {
							tmpWP.type = 0;
						}
						
						// double lat
						tmpWP.lat = Double.parseDouble(eFPC.getElementsByTagName("latitude").item(0).getTextContent());
						
						// double lon
						tmpWP.lon = Double.parseDouble(eFPC.getElementsByTagName("longitude").item(0).getTextContent());
						
						// double alt
						tmpWP.alt = Double.parseDouble(eFPC.getElementsByTagName("altitude").item(0).getTextContent());
						
						// double altDiff
						if (eFPC.getElementsByTagName("altDiff").item(0) != null) {
							tmpWP.altDiff = Double.parseDouble(eFPC.getElementsByTagName("altDiff").item(0).getTextContent());
						}
						
						// int direction	// -1 = left | 0 = closest | 1 = right
						if (eFPC.getElementsByTagName("direction").item(0) != null) {
							Log.i("anemoi", "myfly direction: " + eFPC.getElementsByTagName("direction").item(0).getTextContent());
							if (eFPC.getElementsByTagName("direction").item(0).getTextContent().equals("left")) {
								tmpWP.dir = -1;
								Log.i("anemoi", "myfly direction: -1");
							} else if (eFPC.getElementsByTagName("direction").item(0).getTextContent().equals("right")) {
								tmpWP.dir = 1;
								Log.i("anemoi", "myfly direction: 1");
							} else {
								tmpWP.dir = 0;
								Log.i("anemoi", "myfly direction: 0");
							}
						}
						
						// double distAcq
						if (eFPC.getElementsByTagName("distAcq").item(0) != null) {
							tmpWP.distAcq =  Double.parseDouble(eFPC.getElementsByTagName("distAcq").item(0).getTextContent());
						}
						
						// double altAcq
						if (eFPC.getElementsByTagName("altAcq").item(0) != null) {
							tmpWP.altAcq =  Double.parseDouble(eFPC.getElementsByTagName("altAcq").item(0).getTextContent());
						}
						
						// int loiterCount
						if (eFPC.getElementsByTagName("loiterCount").item(0) != null) {
							tmpWP.louterCount =  Integer.parseInt(eFPC.getElementsByTagName("loiterCount").item(0).getTextContent());
						}
						
						// int next
						if (eFPC.getElementsByTagName("next").item(0) != null) {
							Log.i("anemoi", "myfly next: " + eFPC.getElementsByTagName("next").item(0).getTextContent() + " - " + wpID.get(eFPC.getElementsByTagName("next").item(0).getTextContent()));
							tmpWP.next = wpID.get(eFPC.getElementsByTagName("next").item(0).getTextContent());
						}
						
						wpList.add(tmpWP);
						wpCount++;
					}
				}
	 
			}
		}
		
		
		return wpList;
	}
	
public Envelope returnEnvelope(String flightPlan) {
		
		Envelope env = new Envelope();
		
		String filePath = "";
		
		Log.i("anemoi", "myfly return: " + flightPlan);
		
		// check if the path starts with file:/// or /
		if (flightPlan.contains(":///")) {
			String[] separated = flightPlan.split("://");
			filePath = separated[1];
		} else {
			filePath = flightPlan;
		}
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		Document doc;
		
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(new File(filePath));
		} catch (Exception e) {
			Log.i("anemoi", "myfly error " + e.getMessage());
			return null;
		}
		
		NodeList nFPList = doc.getElementsByTagName("path");
		Log.i("anemoi", "myfly length: " + nFPList.getLength());
		
		for (int nfp = 0; nfp < nFPList.getLength(); nfp++) {
			Node nFPNode = nFPList.item(nfp);
			if (nFPNode.getNodeType() == Node.ELEMENT_NODE) {
				NodeList nFPChildren = nFPNode.getChildNodes();
				for (int nfpc = 0; nfpc < nFPChildren.getLength(); nfpc++) {
					Node nFPChild = nFPChildren.item(nfpc);
					
					if (nFPChild.getNodeType() == Node.ELEMENT_NODE) {
						Element eFPC = (Element) nFPChild;
						
						WayPoint tmpWP = new WayPoint();
						
						// parse waypoint ...
						
						wpID.put(eFPC.getElementsByTagName("id").item(0).getTextContent(), wpCount);
						
						// int type		// 0 = fly | 1 = level | 2 = loiter
						if (eFPC.getElementsByTagName("type").item(0).getTextContent().equals("loiter")) {
							tmpWP.type = 2;
						} else if (eFPC.getElementsByTagName("id").item(0).getTextContent().equals("level")) {
							tmpWP.type = 1;
						} else {
							tmpWP.type = 0;
						}
						
						// double lat
						tmpWP.lat = Double.parseDouble(eFPC.getElementsByTagName("latitude").item(0).getTextContent());
						
						// double lon
						tmpWP.lon = Double.parseDouble(eFPC.getElementsByTagName("longitude").item(0).getTextContent());
						
						// double alt
						tmpWP.alt = Double.parseDouble(eFPC.getElementsByTagName("altitude").item(0).getTextContent());
						
						// double altDiff
						if (eFPC.getElementsByTagName("altDiff").item(0) != null) {
							tmpWP.altDiff = Double.parseDouble(eFPC.getElementsByTagName("altDiff").item(0).getTextContent());
						}
						
						// int direction	// -1 = left | 0 = closest | 1 = right
						if (eFPC.getElementsByTagName("direction").item(0) != null) {
							Log.i("anemoi", "myfly direction: " + eFPC.getElementsByTagName("direction").item(0).getTextContent());
							if (eFPC.getElementsByTagName("direction").item(0).getTextContent().equals("left")) {
								tmpWP.dir = -1;
								Log.i("anemoi", "myfly direction: -1");
							} else if (eFPC.getElementsByTagName("direction").item(0).getTextContent().equals("right")) {
								tmpWP.dir = 1;
								Log.i("anemoi", "myfly direction: 1");
							} else {
								tmpWP.dir = 0;
								Log.i("anemoi", "myfly direction: 0");
							}
						}
						
						// double distAcq
						if (eFPC.getElementsByTagName("distAcq").item(0) != null) {
							tmpWP.distAcq =  Double.parseDouble(eFPC.getElementsByTagName("distAcq").item(0).getTextContent());
						}
						
						// double altAcq
						if (eFPC.getElementsByTagName("altAcq").item(0) != null) {
							tmpWP.altAcq =  Double.parseDouble(eFPC.getElementsByTagName("altAcq").item(0).getTextContent());
						}
						
						// int loiterCount
						if (eFPC.getElementsByTagName("loiterCount").item(0) != null) {
							tmpWP.louterCount =  Integer.parseInt(eFPC.getElementsByTagName("loiterCount").item(0).getTextContent());
						}
						
						// int next
						if (eFPC.getElementsByTagName("next").item(0) != null) {
							Log.i("anemoi", "myfly next: " + eFPC.getElementsByTagName("next").item(0).getTextContent() + " - " + wpID.get(eFPC.getElementsByTagName("next").item(0).getTextContent()));
							tmpWP.next = wpID.get(eFPC.getElementsByTagName("next").item(0).getTextContent());
						}
						
						wpList.add(tmpWP);
						wpCount++;
					}
				}
	 
			}
		}
		
		
		return wpList;
	}
}
