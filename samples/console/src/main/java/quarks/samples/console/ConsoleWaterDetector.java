/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2016 
*/
package quarks.samples.console;

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import quarks.console.server.HttpServer;
import quarks.metrics.Metrics;
import quarks.providers.development.DevelopmentProvider;
import quarks.providers.direct.DirectProvider;


import quarks.topology.TStream;
import quarks.topology.Topology;

/**
 * 
 * Demonstrates some of the features of the console.
 * <P>
 * The topology graph in the console currently allows for 3 distinct "views" of the topology:
 * <ul>
 * <li>Static flow</li>
 * <li>Tuple count</li>
 * <li>Oplet kind</li>
 * </ul>
 * </P>
 * <P>
 * Selecting any of these, with the exception of "Static flow", adds a legend to the topology which
 * allows the user to identify elements of the view. 
 * <P> The "Static flow" view shows the toology in an unchanging state - that is if tuple counts are available the
 * lines (connections) representing the edges of the topology are not updated, nor are the circles (representing the vertices) dimensions updated.  
 * The purpose of this view is to give the user an indication of the topology map of the application. 
 * <P>
 * The "Oplet kind" view colors the oplets or vertices displayed in the topology graph (the circles) by their
 * corresponding Oplet kind.
 * </P>
 * If "Tuple count" is selected the legend reflects ranges of tuple counts measured since the application was started.
 * </P>
 * <P>
 * Note: The DevelopmentProvider class overrides the submit method of the DirectProvider class
 * and adds a Metrics counter to the submitted topology.
 * If a counter is not added to the topology (or to an individual oplet), the "Tuple count" view selection is not enabled.
 * </P>
 * 
 * <P>
 * In the lower half of the quarks console is a chart displaying metrics, if available.  In this example metrics
 * are available since the DevelopmentProvider class is being used.  Note that the DevelopmentProvider class adds a Metrics counter
 * to all oplets in the topology, with the exception of certain oplet types.  For further information
 * about how metrics are added to a topology, see the details in the quarks.metrics.Metrics class and the counter method.
 * <br/>
 * A counter can be added to an individual oplet, and not the entire topology.  For an example of this
 * see the quarks.samples.utils.metrics.DevelopmentMetricsSample.
 * </P>
 * <P>
 * The quarks.metric.Metrics class also provides a rate meter.  Rate meters must be added to individual oplets and are not currently
 * available for the entire topology.
 * </P>

 * <P>
 * The metrics chart displayed is a bar chart by default.  If a rate meter is added to an oplet it will be displayed
 * as a line chart over the last 20 measures (the interval to refresh the line chart is every 2 1/2 seconds).
 * If a counter is added to a single oplet, the tuple count can also be displayed as a line chart.
 * </P>
 * 
 * <P>
 * ConsoleWaterDetector scenario:
 * A county agency is responsible for ensuring the safety of residents well water.  Each well they monitor has four different 
 * sensor types:
 * <ul>
 * <li>Temperature</li>
 * <li>Acidity</li>
 * <li>Ecoli</li>
 * <li>Lead</li>
 * </ul>
 * </P>
 * <P>
 * This application topology monitors 3 wells:
 * <ul>
 * <li>
 * Each well that is to be measured is added to the topology.  The topology polls each sensor for each well as a unit.  
 * All the sensor readings for a single well are 'unioned' into a single TStream&lt;JsonObject&gt;.
 * </li>
 * <li>
 * Now, each well has a single stream with each of the sensors readings as a property with a name and value in the JsonObject.  
 * Each well's sensors are then checked to see if their values are in an acceptable range.  The filter oplet is used to check each sensor's range.  
 * If any of the sensor's readings are out of the acceptable range the tuple is passed along. Those that are within an acceptable range 
 * are discarded.
 * </li>
 * <li>
 * If the tuples in the stream for the well are out of range they are then passed to the split oplet. The split oplet breaks the single
 * TStream&lt;JsonObject&gt; into individual streams for each sensor type for the well.  
 * </li>
 * <li>
 * Well1 and Well3's temperature sensor streams have rate meters placed on them.  This will be used to compare the rate of tuples flowing through these
 * streams that are a result of out of range readings for Well1 and Well3 respectively.
 * </li>
 * <li>
 * Each stream that is produced from the split prints out the value for the sensor reading that it is monitoring along with the wellId.
 * </li>
 * </ul>
 * </P>
 *
 */

public class ConsoleWaterDetector {

	/**
	 * Hypothetical values for all the sensor types: temp, acidity, ecoli and Lead
	 */
	static int TEMP_ALERT_MIN = 49;
	static int TEMP_ALERT_MAX = 81;
	static int TEMP_RANDOM_LOW = 40;
	static int TEMP_RANDOM_HIGH = 90;
	static String TEMP_ALERT_TAG = "TEMP out of range";
	
	static int ACIDITY_ALERT_MIN = 4;
	static int ACIDITY_ALERT_MAX = 9;
	static int ACIDITY_RANDOM_LOW = 1;
	static int ACIDITY_RANDOM_HIGH = 11;
	static String ACIDITY_ALERT_TAG = "ACIDITY out of range";
	
	static int ECOLI_ALERT = 1;
	static int ECOLI_RANDOM_LOW = 0;
	static int ECOLI_RANDOM_HIGH = 3;
	static String ECOLI_ALERT_TAG = "ECOLI out of range";

	static int LEAD_ALERT_MAX = 10;
	static int LEAD_RANDOM_LOW = 0;
	static int LEAD_RANDOM_HIGH = 15;
	static String LEAD_ALERT_TAG = "LEAD out of range";
	
	public static void main(String[] args) throws Exception {
		DirectProvider dp = new DevelopmentProvider();
		
		System.out.println(dp.getServices().getService(HttpServer.class).getConsoleUrl());
		
		Topology wellTopology = dp.newTopology("ConsoleWaterDetector");
		
		TStream<JsonObject> well1 = waterDetector(wellTopology, 1);
		TStream<JsonObject> well2 = waterDetector(wellTopology, 2);
		TStream<JsonObject> well3 = waterDetector(wellTopology, 3);
				
		TStream<JsonObject> filteredReadings1 = alertFilter(well1, 1, false);
		TStream<JsonObject> filteredReadings2 = alertFilter(well2, 2, true);
		TStream<JsonObject> filteredReadings3 = alertFilter(well3, 3, false);
		
		List<TStream<JsonObject>> individualAlerts1 = splitAlert(filteredReadings1, 1);
		
		// Put a rate meter on well1's temperature sensor output
		Metrics.rateMeter(individualAlerts1.get(0));
		individualAlerts1.get(0).tag(TEMP_ALERT_TAG, "well1").sink(tuple -> System.out.println("\n" + formatAlertOutput(tuple, "1", "temp")));
		individualAlerts1.get(1).tag(ACIDITY_ALERT_TAG, "well1").sink(tuple -> System.out.println(formatAlertOutput(tuple, "1", "acidity")));
		individualAlerts1.get(2).tag(ECOLI_ALERT_TAG, "well1").sink(tuple -> System.out.println(formatAlertOutput(tuple, "1", "ecoli")));
		individualAlerts1.get(3).tag(LEAD_ALERT_TAG, "well1").sink(tuple -> System.out.println(formatAlertOutput(tuple, "1", "lead")));
		
		List<TStream<JsonObject>> individualAlerts2 = splitAlert(filteredReadings2, 2);
		
	    TStream<JsonObject> alert0Well2 = individualAlerts2.get(0);
		alert0Well2  = Metrics.counter(alert0Well2);
		alert0Well2.tag("well2", "temp");
		
		TStream<JsonObject> alert1Well2 = individualAlerts2.get(1);
		alert1Well2  = Metrics.counter(alert1Well2);
		alert1Well2.tag("well2", "acidity");
		
		TStream<JsonObject> alert2Well2 = individualAlerts2.get(2);
		alert2Well2  = Metrics.counter(alert2Well2);
		alert2Well2.tag("well2", "ecoli");
		
		TStream<JsonObject> alert3Well2 = individualAlerts2.get(3);
		alert3Well2  = Metrics.counter(alert3Well2);
		alert3Well2.tag("well2", "lead");	

		List<TStream<JsonObject>> individualAlerts3 = splitAlert(filteredReadings3, 3);
		
		// Put a rate meter on well3's temperature sensor output
		Metrics.rateMeter(individualAlerts3.get(0));
		individualAlerts3.get(0).tag(TEMP_ALERT_TAG, "well3").sink(tuple -> System.out.println(formatAlertOutput(tuple, "3", "temp")));
		individualAlerts3.get(1).tag(ACIDITY_ALERT_TAG, "well3").sink(tuple -> System.out.println(formatAlertOutput(tuple, "3", "acidity")));
		individualAlerts3.get(2).tag(ECOLI_ALERT_TAG, "well3").sink(tuple -> System.out.println(formatAlertOutput(tuple, "3", "ecoli")));
		individualAlerts3.get(3).tag(LEAD_ALERT_TAG, "well3").sink(tuple -> System.out.println(formatAlertOutput(tuple, "3", "lead")));
		
		dp.submit(wellTopology);

		while (true) {
	            MetricRegistry metricRegistry = dp.getServices().getService(MetricRegistry.class);
                    SortedMap<String, Counter> counters = metricRegistry.getCounters();

                    Set<Entry<String, Counter>> values = counters.entrySet();
                    for (Entry<String, Counter> e : values) {
            		if (e.getValue().getCount() == 0) {
            		   System.out.println("Counter Op:" + e.getKey() + " has a tuple count of zero!");
            		}
            	    }
                    Thread.sleep(2000);
        	}
	}
	
	/**
	 * Creates a TStream&ltJsonObject&gt; for each sensor reading for each well. Unions all the TStream&lt;JsonObject&gt into a 
	 * single one representing all readings on the well.
	 * @param topology Topology providing the tuples for the sensors
	 * @param wellId Id of the well sending the measurements
	 * @return TStream&lt;JsonObject&gt; containing a measurement from each sensor type.
	 * Creates a single TStream&lt;JsonObject&gt; from polling the four sensor types as TStream&lt;Integer&gt;
	 */
	public static TStream<JsonObject> waterDetector(Topology topology, int wellId) {
		Random rNum = new Random();
		TStream<Integer> temp = topology.poll(() -> rNum.nextInt(TEMP_RANDOM_HIGH - TEMP_RANDOM_LOW) + TEMP_RANDOM_LOW, 1, TimeUnit.SECONDS);
		TStream<Integer> acidity = topology.poll(() -> rNum.nextInt(ACIDITY_RANDOM_HIGH - ACIDITY_RANDOM_LOW) + ACIDITY_RANDOM_LOW, 1, TimeUnit.SECONDS); 
		TStream<Integer> ecoli = topology.poll(() -> rNum.nextInt(ECOLI_RANDOM_HIGH - ECOLI_RANDOM_LOW) + ECOLI_RANDOM_LOW, 1, TimeUnit.SECONDS);
		TStream<Integer> lead = topology.poll(() -> rNum.nextInt(LEAD_RANDOM_HIGH - LEAD_RANDOM_LOW) + LEAD_RANDOM_LOW, 1, TimeUnit.SECONDS);
		TStream<Integer> id = topology.poll(() -> wellId, 1, TimeUnit.SECONDS);
		
		// add tags to each sensor
		temp.tag("temperature", "well" + wellId);
		acidity.tag("acidity", "well" + wellId);
		ecoli.tag("ecoli", "well" + wellId);
		lead.tag("lead", "well" + wellId);
		id.tag("well" + wellId);
		
		TStream<JsonObject> tempObj = temp.map(t -> {
			JsonObject jObj = new JsonObject();
			jObj.addProperty("temp", t);
			return jObj;
		});
		
		TStream<JsonObject> acidityObj = acidity.map(a -> {
			JsonObject jObj = new JsonObject();
			jObj.addProperty("acidity", a);
			return jObj;
		});

		TStream<JsonObject> ecoliObj = ecoli.map(e -> {
			JsonObject jObj = new JsonObject();
			jObj.addProperty("ecoli", e);
			return jObj;
		});
		
		TStream<JsonObject> leadObj = lead.map(l -> {
			JsonObject jObj = new JsonObject();
			jObj.addProperty("lead", l);
			return jObj;
		});

		TStream<JsonObject> idObj = id.map(i -> {
			JsonObject jObj = new JsonObject();
			jObj.addProperty("id", i);
			return jObj;
		});

		// ArrayAsList
		HashSet<TStream<JsonObject>> set = new HashSet <TStream<JsonObject>>();
		set.add(acidityObj);
		set.add(acidityObj);
		set.add(ecoliObj);
		set.add(leadObj);
		set.add(idObj);
		
		TStream<JsonObject> allReadings = tempObj.union(set);

		return allReadings;
	}
	
	/**
	 * Look through the stream and check to see if any of the measurements cause concern.
	 * Only a TStream that has one or more of the readings at "alert" level are passed through
	 * @param readingsDetector The TStream&lt;JsonObject&gt; that represents all of the different sensor readings for the well
	 * @param wellId The id of the well
	 * @param simulateNormal Make this stream simulate all readings within the normal range, and therefore will not pass through the filter
	 * @return TStream&lt;JsonObject&gt; that contain readings that could cause concern.  Note: if any reading is out of range the tuple 
	 * will be returned
	 */
	
	public static TStream<JsonObject> alertFilter(TStream<JsonObject> readingsDetector, int wellId, boolean simulateNormal) {
		readingsDetector = readingsDetector.filter(r -> {
			if (simulateNormal == true) {
				return false;
			}
			
			JsonElement tempElement = r.get("temp");
			if (tempElement != null) {			
				int temp = tempElement.getAsInt();
				return (temp <= TEMP_ALERT_MIN || temp >= TEMP_ALERT_MAX);
			}
			
			JsonElement acidElement = r.get("acidity");
			if (acidElement != null) {
				int acid = acidElement.getAsInt();
				return  (acid <= ACIDITY_ALERT_MIN || acid >= ACIDITY_ALERT_MAX);
			}
			
			JsonElement ecoliElement = r.get("ecoli");
			if (ecoliElement != null) {
				int ecoli = ecoliElement.getAsInt();
				return ecoli >= ECOLI_ALERT;
			}
			
			JsonElement leadElement = r.get("lead");
			if (leadElement != null) {
				int lead = leadElement.getAsInt();
				return lead >= LEAD_ALERT_MAX;
			}
			
			return false;
		});
		
		return readingsDetector;
	}
	/**
	 * Splits the incoming TStream&lt;JsonObject&gt; into individual TStreams based on the sensor type
	 * @param alertStream The TStream&lt;JsonObject&gt; that we know has some out of range condition - it could be temp, acidity, ecoli or lead 
	 * - or all of them
	 * @param wellId The id of the well that has the out of range readings
	 * @return List&lt;TStream&lt;JsonObject&gt;&gt; - one for each sensor.   
	 */
	public static List<TStream<JsonObject>> splitAlert(TStream<JsonObject> alertStream, int wellId) {
		
		List<TStream<JsonObject>> allStreams = alertStream.split(5, tuple -> {
            if (tuple.get("temp") != null) {
            	JsonObject tempObj = new JsonObject();
		int temp = tuple.get("temp").getAsInt();
            	if (temp <= TEMP_ALERT_MIN || temp >= TEMP_ALERT_MAX) {
            		tempObj.addProperty("temp", temp);
            		return 0;
            	} else {
            		return -1;
            	}
            } else if (tuple.get("acidity") != null){
            	JsonObject acidObj = new JsonObject();
              	int acid = tuple.get("acidity").getAsInt();
            	if (acid <= ACIDITY_ALERT_MIN || acid >= ACIDITY_ALERT_MAX) {
            		acidObj.addProperty("acidity", acid);
            		return 1;
            	} else {
            		return -1;
            	}
            } else if (tuple.get("ecoli") != null) {
            	JsonObject ecoliObj = new JsonObject();
            	int ecoli = tuple.get("ecoli").getAsInt();
            	if (ecoli >= ECOLI_ALERT) {
            		ecoliObj.addProperty("ecoli", ecoli);
            		return 2;
            	} else {
            		return -1;
            	}
            } else if (tuple.get("lead") != null) {
            	JsonObject leadObj = new JsonObject();
            	int lead = tuple.get("lead").getAsInt();
            	if (lead >= LEAD_ALERT_MAX) {
            		leadObj.addProperty("lead", lead);
            		return 3;
            	} else {
            		return -1;
            	}
            } else {
            	 return -1;
            }
        });
		
		return allStreams;
	}
	
	/**
	 * Formats the output of the alert, containing the well id, sensor type and value of the sensor
	 * @param alertObj The tuple that contains out of range readings
	 * @param wellId The id of the well
	 * @param alertType The type of sensor that has the possible alert on it
	 * @return String containing the wellId, sensor type and sensor value
	 */
	public static String formatAlertOutput(JsonObject alertObj, String wellId, String alertType) {
		return "Well" + wellId + " alert, " + alertType + " value is " + alertObj.get(alertType).getAsInt();
	}
}
