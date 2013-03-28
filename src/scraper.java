import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import java.sql.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

// stop.db Database Structure
// Agency Name | Agency Tag | Route Name | Route Tag | Stop Name | Stop ID


//Get the Transit Agency Information
public class scraper {
	static HashMap <String,String> agencyList = new HashMap <String,String>();// Agency Tag and Agency Formal Name
	static HashMap <String,String> routeList = new HashMap <String,String>();// Route Tag and Route Formal Name
	static HashMap <String,String> directionList = new HashMap <String,String>();
	static HashMap <String,String> stopList = new HashMap <String,String>();
	
	static String agency = new String();
	static String route = new String();
	static String direction = new String();
	static String stop = new String();
	
	public static void main(String[] args) throws Exception {
		Class.forName("org.sqlite.JDBC");
		Connection connection = DriverManager.getConnection("jdbc:sqlite:stops.db");
		Statement statement = connection.createStatement();
		statement.executeUpdate("drop table if exists stop;");
		statement.executeUpdate("create table stop (agency_name string, agency_tag string, route_name string, route_tag string, stop_name string, stop_id string);");
		
		printAgencies();
		for (String individualAgency : agencyList.keySet()) {
			printRoutes(individualAgency);
			for (String individualRoute : routeList.keySet()) {
				printDirection(individualAgency,individualRoute);
				for (String individualDirection : directionList.keySet()) {
					printStops(individualAgency,individualRoute,individualDirection);
					for (String individualStop : stopList.keySet()) {
						System.out.println(agencyList.get(individualAgency)+" | "+individualAgency+" | "+routeList.get(individualRoute)+" | "+individualRoute+" | "+stopList.get(individualStop)+" | "+individualStop);
						statement.executeUpdate("insert into stop values('"+agencyList.get(individualAgency)+"', '"+individualAgency+"', '"+routeList.get(individualRoute)+"', '"+individualRoute+"', '"+stopList.get(individualStop).replace("'", "")+"', '"+individualStop+"');");
					}
					stopList.clear();
				}
				directionList.clear();
			}
			routeList.clear();
		}
		connection.close();
	}
	
	public static void printRoutes(String agency) {
		URL url;
		URLConnection connection;
		DocumentBuilder dBuilder;
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		try {
			url = new URL("http://webservices.nextbus.com/service/publicXMLFeed?command=routeList&a="+agency);
			connection = url.openConnection();
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(connection.getInputStream());
			
			NodeList nList = doc.getElementsByTagName("route");
			
			for (int i=0; i<nList.getLength(); i++){
				Node nNode = nList.item(i);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					routeList.put(eElement.getAttribute("tag"), eElement.getAttribute("title"));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static boolean printDirection(String agency, String route) {
		URL url;
		URLConnection connection;
		DocumentBuilder dBuilder;
		boolean useForUI = false;
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		try {
			url = new URL("http://webservices.nextbus.com/service/publicXMLFeed?command=routeConfig&a="+agency+"&r="+route);
			connection = url.openConnection();
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(connection.getInputStream());
			
			NodeList nList = doc.getElementsByTagName("direction");
			
			for (int i=0; i<nList.getLength(); i++){
				Node nNode = nList.item(i);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					if (!"".equals(eElement.getAttribute("title")) && eElement.getAttribute("useForUI").equals("true")){
						useForUI = true;
						directionList.put(eElement.getAttribute("tag"), eElement.getAttribute("title"));
					}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return useForUI;
	}
	
	public static void printStops(String agency, String route, String direction) {
		HashMap<String,String> stops = new HashMap<String,String>();//tag and stop Name
		HashMap<String,String> ids = new HashMap<String,String>();//tag and stop id
		URL url;
		URLConnection connection;
		DocumentBuilder dBuilder;
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		try {
			url = new URL("http://webservices.nextbus.com/service/publicXMLFeed?command=routeConfig&a="+agency+"&r="+route);
			connection = url.openConnection();
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(connection.getInputStream());
			
			NodeList nList = doc.getElementsByTagName("stop");
			
			for (int i=0; i<nList.getLength(); i++){
				Node nNode = nList.item(i);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					if (!"".equals(eElement.getAttribute("title"))){
						stops.put(eElement.getAttribute("tag"), eElement.getAttribute("title"));
						ids.put(eElement.getAttribute("tag"), eElement.getAttribute("stopId"));
					}
				}
				
			}
			
			nList = doc.getElementsByTagName("direction");
			for (int i=0; i<nList.getLength(); i++) {
				Node nNode = nList.item(i);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					if (eElement.getAttribute("tag").equals(direction)) {
						NodeList stop = nNode.getChildNodes();
						for (int n=0; n<stop.getLength(); n++) {
							Node stopData = stop.item(n);
							if (stopData.getNodeType() == Node.ELEMENT_NODE) {
								Element individualStop = (Element) stopData;
								stopList.put(ids.get(individualStop.getAttribute("tag")), stops.get(individualStop.getAttribute("tag")));
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
	public static void printAgencies() {
		URL url;
		URLConnection connection;
		DocumentBuilder dBuilder;
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		try {
			url = new URL("http://webservices.nextbus.com/service/publicXMLFeed?command=agencyList");
			connection = url.openConnection();
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(connection.getInputStream());
			
			NodeList nList = doc.getElementsByTagName("agency");
			
			for (int i=0; i<nList.getLength(); i++){
				Node nNode = nList.item(i);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					agencyList.put(eElement.getAttribute("tag"), eElement.getAttribute("title"));
				}
				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
