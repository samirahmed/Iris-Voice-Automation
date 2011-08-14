package com.samir_ahmed.Iris;

import java.net.URLEncoder;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;

/**
 * Class - getWiki:  This class queries mobile.wikipedia and extracts the first sentence
 * 
 * Time: Ranges from 400ms to 800ms
 * 
 * Description: This is a callable class. Any external references are static to ensure safe concurrency
 * getWiki is intended to be run as a threaded task and has only one public facing function: String call()
 * 
 * Only use this class with an executorService.
 * 
 **/
public class getWiki implements Callable<String>{

	/**
	public static void main(String[] args) {
		try{
			getWiki wiki = new getWiki("snow peas" );
			System.out.println(wiki.call());
		}
		catch(Exception ee){ee.printStackTrace();}
	}
	 */

	/**Class data members**/

	private String wikiURL;		// URL to be passed to httpGET
	private String wikiHTML;	// HTML response from httpGET
	private String xQuery;		// modified original query
	private String Answer;		// Answer to query
	public String Query;		// Original constructor query
	private boolean boldFLAG;	// Flag whether we have any bold in extracted sentence

	/** Constructor : Takes in a query term **/
	public getWiki(String Query) {

		// Constructor will not check for null or empty string.
		// This must be done prior to construction
		this.Query = Query;
	}

	/** normalize : Quick and Dirty way to remove excess characters and normalize the Answer**/
	private String normalize(String sentence){

		//sentence = sentence.replaceAll("\\(.*\\)","");					// Remove () and anything inside
		sentence = sentence.replaceAll("\\s+", " ");					// Remove extra White Space
		sentence = sentence.replaceAll("\\s,", ",");					// Remove extra White Space before comma
		sentence = sentence.replaceAll("[\\]\\[]","");					// Remove any brackets []

		return sentence;
	}

	/** removeSpecificPunctuation : Brute force way to remove any Abbreviations **/
	private String removeSpecificPunctuated(String content){

		// Brute force removal and replacement of certain key punctuated abbreviations
		// for removing any Abreviations that will hinder sentence recoginition
		// NOTE: This is efficient enough as it does not even take 1ms to go through this.

		Pattern pp = Pattern.compile("([A-Za-z]\\.){2,}+");
		Matcher mm = pp.matcher(content);
		while(mm.find()){
			content = content.replace(mm.group(),mm.group().replaceAll("\\.","-"));
		}
		
		if (content.contains("Inc.")){ content = content.replaceAll("Inc\\.","") ;}
		if (content.contains("Corp.")){ content = content.replaceAll("Corp\\.","");}
		if (content.contains("Co.")){content = content.replaceAll("Co\\."," company ");}
		if (content.contains("St.")){content = content.replaceAll("St\\."," Saint ");}
		if (content.contains("M.D.")){content = content.replaceAll("M\\.[dD]\\.","");}
		if (content.contains("Ph.D.")){content = content.replaceAll("Ph\\.D\\.","");}
		if (content.contains("Dr.")){content = content.replaceAll("Dr\\."," doctor ");}
		if (content.contains("Sr.")){content = content.replaceAll("Sr\\."," senior ");}
		if (content.contains("Jr.")){content = content.replaceAll("Jr\\."," junior ");}
		if (content.contains("U.S.A.")){content = content.replaceAll("U\\.S\\."," United States ");}
		if (content.contains("U.K.")){content = content.replaceAll("U\\.K\\."," United Kingdom ");}
		if (content.contains("Mt.")){content = content.replaceAll("Mt\\."," Mount ");}
		if (content.contains("vs.")){content = content.replaceAll("vs\\."," versus ");}

		

		return content;
	}

	/** getSentence: Extracts first sentence from html **/
	private String getSentence(String content){

		// Remove anything in parenthesis, remove specific abbreviations and remove any uppercase letters

		content = content.replaceAll("\\(.*?\\)","");			// Remove () and anything inside
		content = removeSpecificPunctuated(content);

		content = content.replaceAll("[A-Z]\\."," ");

		// Get everything from Contents]<TARGET>. to the first full stop
		Pattern sentencePattern  = Pattern.compile("Contents\\][\\W\\w]+?\\.[ \\[]"); 
		Matcher sentenceMatch = sentencePattern.matcher(content);

		if (sentenceMatch.find()){
			// Collect First matched Expression and filter content
			String sentence = sentenceMatch.group();

			// In the event we have a for other ... see | Very Unlikely but possible
			// We proceed to remove the for other sentence and give back a
			if (sentence.contains("For other") &&  sentence.contains("see")){

				//Repeat Regex process and move onto next sentence ... 
				content = content.replaceAll("For other[\\w\\W]+see[\\w\\W]+?\\. ","");
				content = content.replaceFirst("[\\w\\W]*?Contents\\]", "");

				sentencePattern  = Pattern.compile("[\\w\\W]+?\\. ");
				sentenceMatch = sentencePattern.matcher(content);
				if (sentenceMatch.find()){sentence = sentenceMatch.group();}
				else {return whoWhat.noAnswer;}
			}
			else{
				// Remove Contents part
				sentence = sentence.substring("Contents\\]".length());	// Remove Contents] and period
			}
			// Normalize and return
			sentence = this.normalize(sentence);
			return sentence;
		}
		else{
			return whoWhat.noAnswer;
		}


	}

	/** extraction: Determines the nature of the wikipedia html**/
	private void extraction(){

		// Determine if we have bold characters in the first 200 characters.
		// If so we set a flag to true and replace bold characters with asterisks
		// To identify the keep sentence
		if(wikiHTML.substring(0,200).contains("<b>")){
			boldFLAG = true;
			wikiHTML = wikiHTML.replaceAll("\\<b\\>|\\</b\\>","*");			//Use bold tags to mark target sentence
		}
		else{
			boldFLAG = false;
		}

		// Check for <img src=... until the first <\a>]
		// Without this we can't remove the img src it gets pretty fucked up...
		if(wikiHTML.contains("<img src")){
			wikiHTML = wikiHTML.replaceAll("\\<img src[^\\>]*?\\>\\[\\<a[\\w\\W]*?\\</a\\>\\]","");
		}

		// Convert from HTML to plain text
		wikiHTML = Jsoup.parse(wikiHTML).text();								//Convert from HTML to plain text

		// Case 1: We have no article for this term
		if (wikiHTML.contains("No wikipedia article found"))
		{
			Answer = whoWhat.noAnswer;
		}
		// Case 2: We have an abiguous article or search results
		else if (isAmbiguous(wikiHTML)){
			Answer = whoWhat.isAmbiguous;
		}
		// Case 3: We assume we have a valid parsable answer
		else{

			// Attempt to extract the answer
			Answer = getSentence(wikiHTML);

			// If we have a boldflag but no asterisk, parsing has failed
			if (boldFLAG && !Answer.contains("*")){
				Answer = whoWhat.noAnswer;
			}
			// If we have no boldflag and the answer does not contain any asterisks
			// We append the query to the Answer to make it seem a more natural response
			//			else if (!boldFLAG && !Answer.contains("*")){
			//				Answer = Query + " " +Answer;
			//				Answer = Answer.replaceAll("\\*","");
			//			}
			// Otherwise just remove the asterisk
			else{
				Answer = Answer.replaceAll("\\*","");
			}
		}
	}

	/** isAmbiguous: Determines if the html response is ambiguous+unparsable **/
	private boolean isAmbiguous(String html) {

		// Assume it is not ambiguous
		boolean isUnsure =false;

		// make new string of first 200 if possible
		String first200 = html;
		if (html.length()>=200){
			first200 = html.substring(0,200);
		}

		// Keep or-ing terms and if any are true we return an isAmbiguous true boolean
		isUnsure = isUnsure || first200.contains("may refer to");		// First 200 characters contain may refer to
		isUnsure = isUnsure || first200.contains("can stand for"); 		// First 200 characters contain can stand for
		isUnsure = isUnsure || first200.contains("can refer to");  		// First 200 characters contain can refer to
		isUnsure = isUnsure || first200.contains("can mean:");			// First 200 characters contains can mean:
		isUnsure = isUnsure || first200.contains("mean:");				// First 200 characters contains mean:
		isUnsure = isUnsure || first200.contains("refer to:");			// First 200 characters contains refer to:
		isUnsure = isUnsure || first200.contains("(disambiguation)");	// First 200 characters contain (disambiguation):	
		isUnsure = isUnsure || first200.contains("Found articles for:");// Indication of a search results page.

		return isUnsure;
	}


	/** wikiSearch: Function for search wikipedia and calling helper functions **/
	private String wikiSearch(){

		// Start time of query
		//long start = System.currentTimeMillis();

		try {

			// Convert first letter to uppercase. Wikipedia convention
			xQuery = Character.toUpperCase(Query.charAt(0))+Query.substring(1);

			// Create URL and safely encode
			xQuery = URLEncoder.encode(xQuery,"UTF-8");
			wikiURL = "http://mobile.wikipedia.org/transcode.php?go="+xQuery;
			//System.out.println("whoWhat Module: Querying " + wikiURL);

			//Use Blocking IO httpGET function
			try{
				wikiHTML = httpGET.download(wikiURL,this);
			}
			// In the event of an interrupted exception, the thread should stop executing
			catch(InterruptedException InterE){
				return whoWhat.cancelled;
			}

			//Uncomment below to measure response time
			//System.out.println("Wiki Response Time: " + (System.currentTimeMillis()-start));

			// Determine if the wikiHTML result has failed. If not extract an Answer and return it.
			if (wikiHTML ==null || wikiHTML.equals("")){
				return whoWhat.failed;	
			}
			else{
				extraction();
			}
			return Answer;
		}
		catch (Exception ee){ee.printStackTrace(); return whoWhat.failed;}
	}

	/** call: Callable implementation of this class. Returns a future<string> **/
	public String call(){
		try{
			return wikiSearch();
		}
		catch(Exception ee){ee.printStackTrace(); return whoWhat.failed;}
	}

}