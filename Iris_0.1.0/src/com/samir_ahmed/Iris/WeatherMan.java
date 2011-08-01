package com.samir_ahmed.Iris;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WeatherMan implements IrisModule{

	public static void main(String[] args) {
		try{
			ExecutorService exs = Executors.newFixedThreadPool(10);
			System.out.println(System.currentTimeMillis());
			Synthesizer voice = new Synthesizer(exs);
			WeatherMan weather = new WeatherMan();
			//		Synthesizer voice1 = new Synthesizer(exs,"Looking up weather for Albany New York");		
			//		exs.submit(voice1);
			weather.setUtterance("what is the weather in singapore");
			Future<String[]> answer = exs.submit(weather);
			voice.setAnswer(answer.get()[0]);
			Future<?> wait = exs.submit(voice);
			wait.get();
			exs.shutdown();
		}
		catch(Exception ee){ee.printStackTrace();}
	}	

//	public WeatherMan(ExecutorService ExServ, Synthesizer Voice){
//		this.exs = ExServ;
//		this.voice = Voice;
//	}
//
//	private final ExecutorService exs;
//	private final Synthesizer voice;
	
	private  String Answer;
	private String utterance;


	public  String USER_LOCATION = "Boston";
	private final String weatherPath = System.getProperty("user.dir")+"/res/weather.txt";
	
//	private  final String[] keyTerms = {
//			"weather",
//			"forecast",
//			"current weather",
//			"weather right now",
//			"what is it like out",
//			"what is the weather",
//			"weather for"
//	};

	/** Primary Keys **/
	private final String[] PrimaryKeys = {
			"weather",
			"what",
			"whats",
			"what's",
			"forecast",
			"current"
	};

	/** RegExKeys Keys **/
	private final String[] RegExKeys = {
			// RegEx that match weather specific input with POS Tagged
			"weather/.*",
			"what/(.*?)weather/NN((.*?)/)IN.*",		// Will match weather followed by a preposition only!
			"whats/(.*?)weather/NN((.*?)/)IN.*",
			"what's/(.*?)weather/NN((.*?)/)IN.*",
			"what/(.*?)weather/NN\\s*",
			"what's/(.*?)weather/NN\\s*",
			"whats/(.*?)weather/NN\\s*",
			"forecast/.*",
			"current/(.*?)(weather|forecast).*"
	};
	
	
//	private final String[] parseTags = {"tomorrow","next few days","this week","day after tomorrow",
//			"monday", "tuesday","wednesday","thursday","friday","saturday","sunday",
//			"this weekend","next week"};

	
	public String getUSER_LOCATION(){
		return this.USER_LOCATION;
	}
	
	/**Allows the user to set the user location, true if the new location has been set, false if not**/
	public boolean setUSER_LOCATION(String newLocation){
		
		// Only check if this is a valid string not if this string is a valid location searchable via google weather
		if (newLocation == null || newLocation.equals("")){
			return false;	
		}
		else{
			this.USER_LOCATION=newLocation;
			return true;
		}
	}

	/** Required for the Interface IrisModule : returns the valid primary keys for this module*/
	public String[] getPrimaryKeys()  {
		return this.PrimaryKeys;
	};

	
	/** Required for the Interface IrisModule : returns the valid RegEx for this module*/
	public String[] getRegExKeys() {
		return this.RegExKeys;
	}
	
	
	/** Set the utterance for the module */
	public void setUtterance (String Utterance) throws NullPointerException{
		if (Utterance ==null || Utterance.equals("")){
			throw new NullPointerException("Not a valid utterance");
		}
		else {
			this.utterance = Utterance;
		}
	}
	
	/** Connect to the internet and extract the XML weather file, that takes about the current location */
	private  String getXML(String location)
	{
		// Set up Strings, URL
		//String buffer;
		String locationURL =location.replaceAll("\\s","+"); // Replace via 
		String weatherURL = "http://www.google.com/ig/api?weather="+locationURL+"/";
		//System.out.println(weatherURL);
		try{			
			String weatherXML = httpGET.download(weatherURL,this);

			if (weatherXML.contains("problem")){weatherXML = "_failed_";}
			return weatherXML;
		}
		catch(Exception ee){
			ee.printStackTrace();
			System.err.println("Weather API: Error Reading WeatherXML File");
			return "_failed_";
		}
	}

	/** Determine location by striping common words**/
	private  String getLocation(String raw){
		// Take in the input and determine location
		String words;
		String[] query  = raw.toLowerCase().split("\\s");
		String result = "";

		try {
			words = readFile(weatherPath);
			//System.out.println(words); // For testing correct file reading

			String[] deleteStrings = words.split("\\s");
			Set<String> deleteTerms = new HashSet<String>(Arrays.asList(deleteStrings));

			for (String queryTerm : query){ 
				if (!deleteTerms.contains(queryTerm))
				{result += queryTerm+" ";}
			}

			String location = result.toString();
			if (location == "" ){location = USER_LOCATION;}
			else {location = location.substring(0,location.length()-1);}		

			//System.out.println("Weather Module: Location: "+location);
			return location;
		} catch (IOException e) {
			
			e.printStackTrace();
			System.out.println("Weather Module: Failed to Read File "+weatherPath);
			return "_failed_";
		}
	}

	/** Read in the weather.txt common words file*/
	private  String readFile( String file ) throws IOException {
		BufferedReader reader = new BufferedReader( new FileReader (file));
		String line  = null;
		StringBuilder stringBuilder = new StringBuilder();
		String ls = System.getProperty("line.separator");
		while( ( line = reader.readLine() ) != null ) {
			stringBuilder.append( line );
			stringBuilder.append( ls );
		}
		return stringBuilder.toString();
	}

	/** Parse XML with good ole REGEX because I hate XML would rather do it by hand*/
	public  String[] getConditions(String xmlfile){
		String [] results=new String[5];

		// REGEX Patterns Extracts the current, temp, high, low and forecast
		Pattern[] statsPattern= new Pattern[5];
		statsPattern[0] = Pattern.compile("condition data=\"[\\w\\s]+\"");
		statsPattern[1] = Pattern.compile("temp_f data=\"[\\w\\s]+\"");
		statsPattern[2] = Pattern.compile("condition data=\"[\\w\\s]+\"/></f");
		statsPattern[3] = Pattern.compile("high data=\"[\\w\\s]+\"");
		statsPattern[4] = Pattern.compile("low data=\"[\\w\\s]+\"");


		// Pattern compiled for extracting form quotations
		Pattern extract = Pattern.compile("\"[\\w\\s]+\"");

		// Parse the XMLfile via REGEX matching.
		// Extract the quoted attributes via REGEX matching and store in Cell Array 

		try {

			//Pattern wind = Pattern.compile("wind_condition data=\"[\\w\\s]+\"");			

			int counter = 0;
			for(Pattern pp : statsPattern){
				// Create Matcher, Find in form data="TARGET"
				Matcher mm1 = pp.matcher(xmlfile);
				mm1.find();

				// Create second Matcher for extraction.
				Matcher mm2 = extract.matcher(mm1.group());
				mm2.find();

				// Index Substring
				String res = mm2.group();
				results[counter] = res.substring(1, res.length()-1);
				counter ++;

				//System.out.println(results[counter-1]);
			}
			return results;
		}
		catch (Exception ee){String[] rr = {"_failed_"}; return rr;} 
	}

	/** Check if the weatherman module has failed if so return _failed_*/
	public boolean failCheck(String status){
		if (status=="_failed_"){
			System.err.println("Weather Module: Failed to determine/extract/parse Location.");
			System.err.println("Weather Module: Module Terminated.");
			Answer= "Sorry, I am unable to find the weather for your location";
			//TODO:answerSequence.main(error);
			return true;}
		else{
			return false;
		}

	}

	/** Implements the call interface by calling the getWeather Function*/
	public String[] call(){
		try{
			return new String[] {getWeather(),""};
		}
		catch (Exception ee){ee.printStackTrace();
		return new String[]{"Oops, I am having technical difficulties, check the internet connection please",""};
		}
	}

	private String getWeather() {
		// Determine location
		String location = getLocation(utterance);
		if (!failCheck(location)){
			// Download xml file goes here, then check status		
			Answer  = ("Weather for "+location+ ", ");
			String xml = getXML(location);
			if (!failCheck(xml)){

				// Parse the xml file to get the conditions, then check status
				String[] conditions = getConditions(xml);
				failCheck(conditions[0]);

				// Send the weather conditions through synthesizer
				Answer +=  "Currently "+conditions[0]+", at "+ conditions[1] +
				" degrees. " + "Forecast says, "+conditions[2]+". A high of "+conditions[3]+
				" and A low of "+conditions[4]; 
			}
		}
		return Answer; 
		
		
		//TODO: answerSequence.main(currentCondition);
		//TODO: answerSequence.main(forecastCondition);


	}	

}
