package com.samir_ahmed.Iris;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Iris {

	/**
	 * DataMembers of Iris
	 */

	// Immutable DataMember
	private final ExecutorService exs;
	private final PoSTagger Tagger;
	private final openBrowser browser;
	private final Synthesizer synthesizer;
	private final Recognizer recognizer;

	// Futures
	private Future<?> TaggerFuture;
	private Future<String[]> recognizerFuture;
	private Future<?> synthesizerFuture;
	private Future<?> browserFuture;

	// Data Structures for holding Keys and IrisModules
	private HashMap<String,ArrayList<Integer> > PrimaryKeyMap;
	private HashMap <Integer, String[]> RegExKeyMap;
	private ArrayList<IrisModule> Modules;

	// Static Data Members
	private static long StartTime;

	// The Rest ...
	private String utterance;
	private String[] Answer;
	private Double confidence;
	private String voice;
	private String openURL;

	/** Constructor for this class**/
	public Iris(){
		// Initialize various objects
		this.exs = Executors.newFixedThreadPool(20);

		this.recognizer = new Recognizer(exs);
		this.recognizerFuture = exs.submit(recognizer);

		this.Tagger = new PoSTagger();		
		this.TaggerFuture = exs.submit(Tagger);

		this.synthesizer = new Synthesizer(exs);
		this.browser = new openBrowser();
		this.Modules = getIrisModules();
		Iris.StartTime = 0;
		LoadAllKeys();
	}

	public void go(){
		try {
			this.utterance = recognizerFuture.get()[0];
			this.confidence = Double.parseDouble(recognizerFuture.get()[1]);

			if (confidence < 0.700){
				this.synthesizer.setAnswer("I'm Sorry. I Am Not Sure I Hear You Correctly");
				this.openURL = "";
			}
			else{
				int ModuleNo = getModuleNumber(this.utterance);
				if (ModuleNo<0){
					voice = "Invalid Input.";
					openURL = "";
				}
				else{
					IrisModule SelectedModule = Modules.get(ModuleNo);
					SelectedModule.setUtterance(utterance);
					Future<String[]> AnswerFuture = exs.submit(SelectedModule);
					this.Answer = AnswerFuture.get();
					this.voice = Answer[0]; 
					this.openURL = Answer[1]; 
				}
			}

			boolean voiceOut=false;
			boolean browserOut=false;

			if(voice != null && !voice.equals("")){
				this.synthesizer.setAnswer(voice);
				this.synthesizerFuture = this.exs.submit(synthesizer);
				voiceOut=true;
			}

			if(openURL != null && !openURL.equals("")){ 
				this.browser.setBrowserURL(openURL);
				this.browserFuture = this.exs.submit(this.browser);
				browserOut=true;
			}

			if(voiceOut){
				this.synthesizerFuture.get();
			}

			if(browserOut){
				this.browserFuture.get();
			}


		} 
		catch (InterruptedException ee) {
			ee.printStackTrace();
			exit();
		} 
		catch (ExecutionException ee) {
			ee.printStackTrace();
			exit();
		}
		catch (MalformedURLException ee){
			ee.printStackTrace();
			exit();
		}
		catch (Exception ee){
			ee.printStackTrace();
			exit();
		}
	}

	private void exit(){
		exs.shutdown();
		System.out.println("Iris is Closing");
		//System.exit(0);
	}

	private void LoadAllKeys(){

		// Load all the primary keys to the primary key hash map and the regex ones to the regex hashmao
		Integer counter = 0;
		PrimaryKeyMap = new HashMap <String, ArrayList<Integer> >(); 
		RegExKeyMap = new HashMap <Integer, String[]>();
		for (IrisModule im : this.Modules){

			String [] pkeys = im.getPrimaryKeys();
			ArrayList<Integer> intList;

			for (String pkey : pkeys){
				if (PrimaryKeyMap.containsKey(pkey)){
					intList=PrimaryKeyMap.get(pkey);
					PrimaryKeyMap.remove(pkey);
					intList.add(counter);
				}
				else{
					intList=new ArrayList<Integer>();
					intList.add(counter);

				}
				PrimaryKeyMap.put(pkey,intList);
			}

			RegExKeyMap.put(counter++, im.getRegExKeys()) ;
		}
	}

	private ArrayList<IrisModule> getIrisModules(){

		// Construct all the interface modules to be put into the ArrayList modules
		ArrayList<IrisModule> modules = new ArrayList<IrisModule>();

		whoWhat WW =  new whoWhat(exs);
		modules.add(WW);

		WeatherMan WM = new WeatherMan();
		modules.add(WM);

		YouTubePlayer YTP = new YouTubePlayer();
		modules.add(YTP);

		return modules;
	}

	private Integer getModuleNumber(String utterance) throws Exception{

		// Split the utterance by whitespaces create an Array List to store valid Modules that match Primary Key
		String [] UtteranceArray = utterance.split("\\s");
		ArrayList <Integer> ValidModules = new ArrayList<Integer>();

		// Check if any of the words in the utterance are valid keys. If so add the modules number to the ArrayList
		for (String word : UtteranceArray){
			if(PrimaryKeyMap.containsKey(word)){
				ValidModules.addAll(PrimaryKeyMap.get(word));
				System.out.println("valid = " + word);
			}
		}

		// Assume we have no valid module
		Integer ModuleNumber = -1;

		// Case 1: We found no valid primary keys. Results in returning negative one 
		if (ValidModules.size() == 0){
		}

		// Case 2: We found exactly 1 valid module, which we return
		else if (ValidModules.size() == 1) {
			ModuleNumber = ValidModules.get(0);
		}

		/* Case 3: We find more than 1 valid module
		 * We will use natural language parsing and regex matching ...
		 * 
		 * Tag the utterance for with Stanford Part of Speech Tagger
		 * For every valid module, we extract the valid RegEx
		 * For every RegEx we match it to the POS Tagged Utterance and if we find a match we return it
		 * 
		 */

		else {
			this.TaggerFuture.get();
			String taggedUtterance = Tagger.Tag(utterance);
			String longestRegEx="";
			for (Integer vm : ValidModules){
				for(String rgx : RegExKeyMap.get(vm)){
					if(taggedUtterance.matches(rgx)){
						System.out.println(taggedUtterance + " matches : " + rgx);
						if(rgx.length() > longestRegEx.length()){
							ModuleNumber = vm;	
						}
					}
				}
			}
		}

		return ModuleNumber;
	}
	public static void main(String[] args) {
		//  IRIS Point of Entry
		try{
			Iris iris = new Iris();
			iris.go();
			iris.exit();
		}
		catch(Exception ee){ee.printStackTrace();}
	}

	public static void setStartTime(){
		Iris.StartTime = System.currentTimeMillis(); 
	}

	public static void printTime(){
		if (Iris.StartTime != 0){
			System.out.println("Time Elapsed : " + (System.currentTimeMillis()-Iris.StartTime) );
		}
		else{
			System.err.println("No Start Time Set");
		}
	}

	/* Tester Function for the getModuleNumber function
	 * 
	public static void main(String[] args) {
		//  IRIS Point of Entry
		try{
			Iris iris = new Iris();
			//iris.go();
			Scanner sc =  new Scanner(System.in);
			System.out.print( "Type Utterance : ");
			String utterance = sc.nextLine();
			System.out.println(utterance);

			while (!utterance.equals("done")){
				long start= System.currentTimeMillis();
				Integer mno = iris.getModuleNumber(utterance);
				String print="Match: ";
				System.out.println(mno);
				if(mno == 0){print+="WhoWhat";}
				else if(mno ==1){print+="WeatherMan";}
				else if(mno ==2){print+="YouTube";}
				else if (mno ==-1){print+="None";}
				long time = (System.currentTimeMillis()-start);
				print += " \nTime: " +  time;
				System.out.println(print);
				System.out.println( "Next Utterance : "); 
				utterance = sc.nextLine();
			}
			iris.exit();
		}catch(Exception ee){ee.printStackTrace();}
	}
	 **/
}