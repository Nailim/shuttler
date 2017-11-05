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
		public String name;
		
		public int type;		// 0 = fly | 1 = level | 2 = loiter
		
		public double lat;
		public double lon;
		
		public double alt;
		public double altDiff;
		
		public int dir;			// -1 = left | 0 = closest | 1 = right
		
		public double distAcq;
		public double altAcq;
		
		public int loiterCount;
		
		public int next;
		
		public WayPoint() {
			this.name = "";
			
			this.type = 0;
			
			this.lat = 0.0;
			this.lon = 0.0;
			
			this.alt = 0.0;
			this.altDiff = 0.0;
			
			this.dir = 0;
			
			this.distAcq = 50.0;
			this.altAcq = 50.0;
			
			this.loiterCount = 1;
			
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
	
public class Settings {
		
		public double pid_p_ail;
		public double pid_p_elv;
		public double pid_p_rud;
		
		public double pid_i_ail;
		public double pid_i_elv;
		public double pid_i_rud;
		
		public double pid_d_ail;
		public double pid_d_elv;
		public double pid_d_rud;
		
		public double kal_q_ail;
		public double kal_q_elv;
		public double kal_q_rud;
		public double kal_q_thr;
		
		public double kal_r_ail;
		public double kal_r_elv;
		public double kal_r_rud;
		public double kal_r_thr;
		
		public double kal_p_ail;
		public double kal_p_elv;
		public double kal_p_rud;
		public double kal_p_thr;

		
		
		
		public Settings() {
			this.pid_p_ail = 0.1111;
			this.pid_p_elv = -0.019;
			this.pid_p_rud = 0.1;
			
			this.pid_i_ail = 0.0;
			this.pid_i_elv = -0.025;
			this.pid_i_rud = 0.0;
			
			this.pid_d_ail = 0.5;
			this.pid_d_elv = 0.75;
			this.pid_d_rud = 0.5;
			
			this.kal_q_ail = 1.0;
			this.kal_q_elv = 0.1;
			this.kal_q_rud = 0.1;
			this.kal_q_thr = 0.01;
			
			this.kal_r_ail = 0.1;
			this.kal_r_elv = 31.0;
			this.kal_r_rud = 65.0;
			this.kal_r_thr = 35.0;
			
			this.kal_p_ail = 1.0;
			this.kal_p_elv = 1.0;
			this.kal_p_rud = 1.0;
			this.kal_p_thr = 1.0;
		}
	}
	
	myFly() {
	}
	
	public WayPoint returnNewWayPoint() {
		return new WayPoint();
	}
	
	public WayPoint getStartWayPoint(String flightPlan) {
		WayPoint wpStart = new WayPoint();
		
		int wpCount = 0;
		
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
		
		NodeList nFPList = doc.getElementsByTagName("start");
		Log.i("anemoi", "myfly length: " + nFPList.getLength());
		
		for (int nfp = 0; nfp < nFPList.getLength(); nfp++) {
			Node nFPNode = nFPList.item(nfp);
			if (nFPNode.getNodeType() == Node.ELEMENT_NODE) {
				NodeList nFPChildren = nFPNode.getChildNodes();
				for (int nfpc = 0; nfpc < nFPChildren.getLength(); nfpc++) {
					Node nFPChild = nFPChildren.item(nfpc);
					Log.i("anemoi", "!!! WP search: " + nFPChild.getNodeName());
					if ((nFPChild.getNodeType() == Node.ELEMENT_NODE) && (nFPChild.getNodeName().equals("waypoint"))) {
						Element eFPC = (Element) nFPChild;
						Log.i("anemoi", "GOT WP!!!");
						wpCount++;
						WayPoint tmpWP = new WayPoint();
						
						// parse waypoint ...
						
						tmpWP.name = eFPC.getElementsByTagName("id").item(0).getTextContent();
						
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
							tmpWP.loiterCount =  Integer.parseInt(eFPC.getElementsByTagName("loiterCount").item(0).getTextContent());
						}
						
						wpStart = tmpWP;
					}
				}
	 
			}
		}
		
		if (wpCount > 0) {
			return wpStart;
		} else {
			return null;
		}
	}
	
	
	
	public WayPoint getStopWayPoint(String flightPlan) {
		WayPoint wpStop = new WayPoint();
		
		int wpCount = 0;
		
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
		
		NodeList nFPList = doc.getElementsByTagName("stop");
		Log.i("anemoi", "myfly length: " + nFPList.getLength());
		
		for (int nfp = 0; nfp < nFPList.getLength(); nfp++) {
			Node nFPNode = nFPList.item(nfp);
			if (nFPNode.getNodeType() == Node.ELEMENT_NODE) {
				NodeList nFPChildren = nFPNode.getChildNodes();
				for (int nfpc = 0; nfpc < nFPChildren.getLength(); nfpc++) {
					Node nFPChild = nFPChildren.item(nfpc);
					Log.i("anemoi", "!!! WP search: " + nFPChild.getNodeName());
					if ((nFPChild.getNodeType() == Node.ELEMENT_NODE) && (nFPChild.getNodeName().equals("waypoint"))) {
						Element eFPC = (Element) nFPChild;
						Log.i("anemoi", "GOT WP!!!");
						wpCount++;
						WayPoint tmpWP = new WayPoint();
						
						// parse waypoint ...
						
						tmpWP.name = eFPC.getElementsByTagName("id").item(0).getTextContent();
						
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
							tmpWP.loiterCount =  Integer.parseInt(eFPC.getElementsByTagName("loiterCount").item(0).getTextContent());
						}
						
						wpStop = tmpWP;
					}
				}
	 
			}
		}
		
		if (wpCount > 0) {
			return wpStop;
		} else {
			return null;
		}
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
						
						tmpWP.name = eFPC.getElementsByTagName("id").item(0).getTextContent();
						
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
							tmpWP.loiterCount =  Integer.parseInt(eFPC.getElementsByTagName("loiterCount").item(0).getTextContent());
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
		
		NodeList nFPList = doc.getElementsByTagName("envelope");
		Log.i("anemoi", "myfly length: " + nFPList.getLength());
		
		for (int nfp = 0; nfp < nFPList.getLength(); nfp++) {
			Node nFPNode = nFPList.item(nfp);
			if (nFPNode.getNodeType() == Node.ELEMENT_NODE) {

				Element eFPC = (Element) nFPNode;
				
				// parse envelope ...
				
				if (eFPC.getElementsByTagName("roll").item(0) != null) {
					env.roll = Double.parseDouble(eFPC.getElementsByTagName("roll").item(0).getTextContent());
				}
				if (eFPC.getElementsByTagName("roll").item(0) != null) {
					env.pitch = Double.parseDouble(eFPC.getElementsByTagName("pitch").item(0).getTextContent());
				}
				
				if (eFPC.getElementsByTagName("ceiling").item(0) != null) {
					env.ceiling = Double.parseDouble(eFPC.getElementsByTagName("ceiling").item(0).getTextContent());
				}
				
				if (eFPC.getElementsByTagName("topSpeed").item(0) != null) {
					env.topSpeed = Double.parseDouble(eFPC.getElementsByTagName("topSpeed").item(0).getTextContent());
				}
				if (eFPC.getElementsByTagName("stallSpeed").item(0) != null) {
					env.stallSpeed = Double.parseDouble(eFPC.getElementsByTagName("stallSpeed").item(0).getTextContent());
				}
				if (eFPC.getElementsByTagName("cruiseSpeed").item(0) != null) {
					env.cruiseSpeed = Double.parseDouble(eFPC.getElementsByTagName("cruiseSpeed").item(0).getTextContent());
				}
				
				if (eFPC.getElementsByTagName("topThrottle").item(0) != null) {
					env.topThrottle = Double.parseDouble(eFPC.getElementsByTagName("topThrottle").item(0).getTextContent());
				}
				if (eFPC.getElementsByTagName("stallThrottle").item(0) != null) {
					env.stallThrottle = Double.parseDouble(eFPC.getElementsByTagName("stallThrottle").item(0).getTextContent());
				}
				if (eFPC.getElementsByTagName("cruiseThrottle").item(0) != null) {
					env.cruiseThrottle = Double.parseDouble(eFPC.getElementsByTagName("cruiseThrottle").item(0).getTextContent());
					Log.i("anemoi", "myfly cruise throttle: " + eFPC.getElementsByTagName("cruiseThrottle").item(0).getTextContent());
				}	 
			}
		}
		
		
		return env;
	}

	public Settings returnSettings(String flightPlan) {
		
		Settings set = new Settings();
		
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
		
		NodeList nFPList = doc.getElementsByTagName("settings");
		Log.i("anemoi", "myfly length: " + nFPList.getLength());
		
		for (int nfp = 0; nfp < nFPList.getLength(); nfp++) {
			Node nFPNode = nFPList.item(nfp);
			if (nFPNode.getNodeType() == Node.ELEMENT_NODE) {
	
				Element eFPC = (Element) nFPNode;
				
				// parse settings ...
				
				if (eFPC.getElementsByTagName("pid_p_ail").item(0) != null) {
					set.pid_p_ail = Double.parseDouble(eFPC.getElementsByTagName("pid_p_ail").item(0).getTextContent());
				}
				if (eFPC.getElementsByTagName("pid_p_elv").item(0) != null) {
					set.pid_p_elv = Double.parseDouble(eFPC.getElementsByTagName("pid_p_elv").item(0).getTextContent());
				}
				if (eFPC.getElementsByTagName("pid_p_rud").item(0) != null) {
					set.pid_p_rud = Double.parseDouble(eFPC.getElementsByTagName("pid_p_rud").item(0).getTextContent());
				}
				
				if (eFPC.getElementsByTagName("pid_i_ail").item(0) != null) {
					set.pid_i_ail = Double.parseDouble(eFPC.getElementsByTagName("pid_i_ail").item(0).getTextContent());
				}
				if (eFPC.getElementsByTagName("pid_i_elv").item(0) != null) {
					set.pid_i_elv = Double.parseDouble(eFPC.getElementsByTagName("pid_i_elv").item(0).getTextContent());
				}
				if (eFPC.getElementsByTagName("pid_i_rud").item(0) != null) {
					set.pid_i_rud = Double.parseDouble(eFPC.getElementsByTagName("pid_i_rud").item(0).getTextContent());
				}
				
				if (eFPC.getElementsByTagName("pid_d_ail").item(0) != null) {
					set.pid_d_ail = Double.parseDouble(eFPC.getElementsByTagName("pid_d_ail").item(0).getTextContent());
				}
				if (eFPC.getElementsByTagName("pid_d_elv").item(0) != null) {
					set.pid_d_elv = Double.parseDouble(eFPC.getElementsByTagName("pid_d_elv").item(0).getTextContent());
				}
				if (eFPC.getElementsByTagName("pid_d_rud").item(0) != null) {
					set.pid_d_rud = Double.parseDouble(eFPC.getElementsByTagName("pid_d_rud").item(0).getTextContent());
				}
				
				if (eFPC.getElementsByTagName("kal_q_ail").item(0) != null) {
					set.kal_q_ail = Double.parseDouble(eFPC.getElementsByTagName("kal_q_ail").item(0).getTextContent());
				}
				if (eFPC.getElementsByTagName("kal_q_elv").item(0) != null) {
					set.kal_q_elv = Double.parseDouble(eFPC.getElementsByTagName("kal_q_elv").item(0).getTextContent());
				}
				if (eFPC.getElementsByTagName("kal_q_rud").item(0) != null) {
					set.kal_q_rud = Double.parseDouble(eFPC.getElementsByTagName("kal_q_rud").item(0).getTextContent());
				}
				if (eFPC.getElementsByTagName("kal_q_thr").item(0) != null) {
					set.kal_q_thr = Double.parseDouble(eFPC.getElementsByTagName("kal_q_thr").item(0).getTextContent());
				}
				
				if (eFPC.getElementsByTagName("kal_r_ail").item(0) != null) {
					set.kal_r_ail = Double.parseDouble(eFPC.getElementsByTagName("kal_r_ail").item(0).getTextContent());
				}
				if (eFPC.getElementsByTagName("kal_r_elv").item(0) != null) {
					set.kal_r_elv = Double.parseDouble(eFPC.getElementsByTagName("kal_r_elv").item(0).getTextContent());
				}
				if (eFPC.getElementsByTagName("kal_r_rud").item(0) != null) {
					set.kal_r_rud = Double.parseDouble(eFPC.getElementsByTagName("kal_r_rud").item(0).getTextContent());
				}
				if (eFPC.getElementsByTagName("kal_r_thr").item(0) != null) {
					set.kal_r_thr = Double.parseDouble(eFPC.getElementsByTagName("kal_r_thr").item(0).getTextContent());
				}
				
				if (eFPC.getElementsByTagName("kal_p_ail").item(0) != null) {
					set.kal_p_ail = Double.parseDouble(eFPC.getElementsByTagName("kal_p_ail").item(0).getTextContent());
				}
				if (eFPC.getElementsByTagName("kal_p_elv").item(0) != null) {
					set.kal_p_elv = Double.parseDouble(eFPC.getElementsByTagName("kal_p_elv").item(0).getTextContent());
				}
				if (eFPC.getElementsByTagName("kal_p_rud").item(0) != null) {
					set.kal_p_rud = Double.parseDouble(eFPC.getElementsByTagName("kal_p_rud").item(0).getTextContent());
				}
				if (eFPC.getElementsByTagName("kal_p_thr").item(0) != null) {
					set.kal_p_thr = Double.parseDouble(eFPC.getElementsByTagName("kal_p_thr").item(0).getTextContent());
				}
			}
		}
		
		
		return set;
	}
}
