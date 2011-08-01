package com.samir_ahmed.Iris;

import java.awt.Toolkit;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Class Recognizer
 * 
 *  This is a threadSafe Callable class that will return a future String array
 * 	Currently the class will call an external process for audio recording httpPOST requests
 * 	
 * 	Time : 2 Seconds response time from google;
 * 
 * */

public class Recognizer implements Callable<String[]> {


	
	public static void main(String[] args) throws InterruptedException, ExecutionException {

		// An executable method that will create a Recognizer Object and Run it, It gives the time take for this process too.
		ExecutorService exs =  Executors.newFixedThreadPool(20);
		Recognizer recognizer = new Recognizer(exs);
		Future <String[]> futureresults = exs.submit(recognizer);
		String [] results = futureresults.get();
		System.out.println(results[0] + " : " +results[1]);
		Iris.printTime();
		exs.shutdownNow();


	  //If necessary Uncomment to Write Results to File
//		try {
//			Files.append( "New: "+ Long.toString(time) + " " + results[0] , new File(System.getProperty("user.dir")+"/res/voicerecognition.txt"), Charsets.UTF_8 );
//		} catch (IOException e) {
//		}
	}
	 

	/** Recognizer's Data Fields
	 *	Confidence: String:  indicating Google Confidence of the result
	 *	wGetResponse: String: Raw response from Google Speech API 
	 *	String command: String: Voice Command
	 *	Confidence: Double: Represents Google Speech API Confidence in results
	 *	recordShell : String: Name of shell file used for getting the 
	 */

	private String googleSpeechAPIResponse;
	private String command;
	private Double confidence;
	//	private final String recordSox =  "-r 16000 -t alsa default recording.flac silence ";
	//	private final String recordShell =System.getProperty("user.dir")+"/res/record.sh";
	private final String[] errorArray = {"Unable to Capture Speech","0.0"};
	private final ExecutorService exs;
	private final httpPOST SendFile;
	private final Recorder recorder;

	/** Default Constructor **/
	public Recognizer(ExecutorService ExServ){
		this.exs = ExServ;
		this.recorder = new Recorder(exs);
		this.SendFile = new httpPOST();
		googleSpeechAPIResponse = null ;
		command = null ;
		confidence  =  0.0;
	}

	/** capture : Will Call the recordShell file and parse the jSON results. **/
	public String[] capture(){
		// Send file to Google's Speech Servers using the Record Command

		try{
			Toolkit.getDefaultToolkit().beep();     

			if (recorder.isActive()){
				recorder.record();
			}
			else{
				return new String[] {"No Speech Heard, Check Microphone Settings","0.0"};
			}


			googleSpeechAPIResponse= SendFile.postFile(System.getProperty("user.dir")+"/recording.flac");
		}
		catch(Exception EE){
			EE.printStackTrace();
			return this.errorArray;
		}

		// Parse Response by finding
		try{
			if (!googleSpeechAPIResponse.contains("\"utterance\":"))
			{
				System.err.println("Recognizer: No Command Could Be Captured");
				String[] error = {"Could not hear Speech","0.0"};
				//answerSequence.main(error);
				return error;
			}
			else
			{
				// Include -> System.out.println(wGetResponse); // to view the Raw output
				int startIndex = googleSpeechAPIResponse.indexOf("\"utterance\":") +13; //Account for term "utterance":"<TARGET>","confidence"
				int stopIndex = googleSpeechAPIResponse.indexOf(",\"confidence\":") -1; //End position
				command = googleSpeechAPIResponse.substring(startIndex,stopIndex);

				// Determine Confidence
				startIndex = stopIndex + 15;
				stopIndex =  googleSpeechAPIResponse.indexOf("}]}") - 1;
				confidence = Double.parseDouble(googleSpeechAPIResponse.substring(startIndex,stopIndex));
				System.out.println("Recognizer: Utterance : " + command+ " : Confidence Level: "+confidence);
			}
		}
		catch (NullPointerException npE){
			// Event that no speech is captured
			return new String[] {"_failed_","-1"};
		}

		return new String[] {command,confidence.toString()}; 
	}	    

	public String[] call(){
		try{
			return capture();
		}
		catch(Exception ee){return errorArray;}
	}

}
