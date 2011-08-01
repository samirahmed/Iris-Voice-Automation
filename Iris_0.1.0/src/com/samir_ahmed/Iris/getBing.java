package com.samir_ahmed.Iris;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.concurrent.Callable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


/** Class - getBing:  This class queries Bing.com and extracts the first wikipedia result
 * 
 * Time: Ranges from 200ms to 600ms
 * 
 * Description: This is a callable class. Any external references are static to ensure safe concurrency
 * getBing is intended to be run as a threaded task and has only one public facing function: String call()
 * 
 * Only use this class with an executorService.
 * 
 **/

public class getBing implements Callable<String>{

	/**
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		getBing bing = new getBing("lollipop");
		System.out.println(bing.call()); 
	}	
	*/
	
	/**
	 * All PRIVATE DATA MEMEBERS
	 */

	private final String bingPrefix = "http://api.bing.net/json.aspx?AppId=5E516C186917986F8872DC015E02C8130C98CE6E&Version=2.2&Market=en-US&Query=" ;
	private final String bingSuffix = "+site:wikipedia.org&Sources=web+spell&Web.Count=1";
	private String bingURL;
	private String bingJSON;
	private String Answer;
	private String wikiURL;
	private String Query;

	/**Consructor : takes in a query string**/
	public getBing(String Query){
		// This constructor does not check if 
		// Query is null or empty. This must be done before hand!
		this.Query = Query;
	}

	/** extraction : Uses GSON to parse Bing JSON result **/
	public void extraction(){

		try{
			// Extract the URL from the JSON object bingJO.
			JsonObject bingJO = new JsonParser().parse(bingJSON).getAsJsonObject();
			wikiURL = bingJO.getAsJsonObject("SearchResponse")
			.getAsJsonObject("Web")
			.getAsJsonArray("Results")
			.get(0).getAsJsonObject()
			.get("Url")
			.toString();

			// If we have a null;
			if (wikiURL == null || wikiURL.equals("")){
				throw  new IOException();
			}
			// Else we assume we got the answer, and we extract the last part of the wikiURL 
			else {
				wikiURL = wikiURL.substring(wikiURL.lastIndexOf("/")+1,wikiURL.length()-1);
			}
		}
		catch (Exception ee){ee.printStackTrace(); Answer = whoWhat.failed;}
	}

	/** searchBing : Uses Microsofts bing brains to search site: wikipedia **/
	public String searchBing(){
		// Uncomment for timing
		//long start = System.currentTimeMillis();
		
		try {
			// Safely encode the bing URL
			String xQuery = URLEncoder.encode(Query,"UTF-8");
			bingURL = bingPrefix+xQuery+bingSuffix;
			
			//System.out.println("whoWhat Module: Querying " + bingURL);

			// Try to download page with httpGET In the event of an intertupt we return "_connectionCancelled_"
			try{
				bingJSON = httpGET.download(bingURL,this);
			}
			catch(InterruptedException InterE){
				return whoWhat.cancelled;
			}
			
			// Uncomment for timing
			//System.out.println("Bing Response Time: " + (System.currentTimeMillis()-start));
			
			// Case 1:  If we don't contain the category Description. Answer = failed
			if (!bingJSON.contains("\"Description\":\"")){
				Answer= whoWhat.noAnswer;
			}
			// Case 2: We assume we have a valid response and attempt to extract the URL and redirect to wikipedia
			else{
				extraction();
				// If empty or null we failed.
				if(wikiURL == null || wikiURL.equals("")){
					Answer = whoWhat.failed;
				}
				// If not we call the create a getWiki object and call it
				else{
					getWiki gw= new getWiki(wikiURL);
					Answer = gw.call();
				}
			}
			return Answer;
		}
		catch (Exception Ee){Ee.printStackTrace(); return whoWhat.failed ; }
	}

	/** call : This method implements the Callable interface **/
	public String call(){
		try{
			searchBing();
			return Answer;
		}
		catch(Exception ee){ee.printStackTrace(); return whoWhat.failed;}
	}

}
