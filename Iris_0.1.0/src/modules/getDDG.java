package modules;

import java.net.URLEncoder;
import java.util.concurrent.Callable;

import org.apache.commons.lang.StringEscapeUtils;

import utils.httpGET;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


/**
 * getDDG : Callable class for querying duckduckgo.com
 * 
 * Time: Ranges from 100ms to 400ms
 *
 * Description: This is a callable class. Any external references are static to ensure safe concurrency
 * getDDG is intended to be run as a threaded task and has only one public facing function: String call()
 * 
 * Only use this class with an executorService.
 */

public class getDDG implements Callable<String> {

	/**
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//Toolkit.getDefaultToolkit().beep();
		getDDG ddg = new getDDG("Port Moresby");
		System.out.println(ddg.call()); 
	}
	 */


	/**
	 * ALL PRIVATE DATA MEMBERS
	 **/
	private String ddgJSON;
	private String xQuery;
	private String ddgURL;
	private String Answer;
	private String Query;

	/**Constructor : Takes in a query string **/
	public getDDG(String Query){
		// This Constructor does not check if the query is not null of empty
		// Ensure this check is done before hand
		this.Query = Query;
	}

	/** extraction: Uses GSON to extract Abstract Text **/
	private void extraction(){

		// Create a new jSON object and get the type
		JsonObject ddgJO = new JsonParser().parse(ddgJSON).getAsJsonObject();
		String ddgType = ddgJO.get("Type").toString();

		// Case 1: It is ambiguous
		if(ddgType.contains("\"D\"")){
			Answer = whoWhat.isAmbiguous;
		}
		// Case 2: It is not ambiguous
		else if(ddgType.contains("\"A\"")){

			// Extract answer
			Answer = ddgJO.get("AbstractText").toString();

			// If we have a definition in the answer, discard the answer
			if(Answer=="" || Answer.contains("definition:")){
				Answer = whoWhat.noAnswer;
			}

			// If the source is not wikipedia, we discard the answer
			else if(!ddgJO.get("AbstractSource").toString().equalsIgnoreCase("\"Wikipedia\"")){
				Answer = whoWhat.noAnswer;
			}

			// Straight forward escaping of any html residue
			else{
				Answer = StringEscapeUtils.unescapeHtml(Answer);				//Remove html escape characters
				Answer = Answer.replaceAll("\\<.*?>","");						//Remove any html tags remaining
				Answer = Answer.replaceAll("\"","");							//Remove enclosing "" marks
				Answer = Answer.replaceAll("\\(.*?\\)","");						//Remove anything in parenthesis
			}
			// Any other case we assume that we have no answer
		}
		else{
			Answer = whoWhat.noAnswer;
		}
	}

	/** duckduckget :  Method for downloading ddg JSON **/
	private String duckduckGet() {

		// Uncomment for timing
		//long start = System.currentTimeMillis();
		try {
			// Safely encode the query as a URL
			xQuery = URLEncoder.encode(Query,"UTF-8");
			ddgURL = "http://api.duckduckgo.com/?q="+xQuery+"&format=json";
			
			// Use Blocking IO with httpGET class
			try{
				ddgJSON = httpGET.download(ddgURL,this);
			}
			catch(InterruptedException InterE){
				return whoWhat.cancelled;
			}

			// If we have null or empty string, httpGET failed
			if (ddgJSON== null || ddgJSON.equals("")){
				Answer = whoWhat.failed;
			}
			// If we dont have any type; Download has failed 
			else if (ddgJSON.contains("\"Type\":\"\"")){
				Answer = whoWhat.failed;
			}
			// Assume true and pass to extraction method
			else{
				extraction();
			}
			//Uncomment for timing
			//System.out.println("Duck-Duck-Go Response Time: " + (System.currentTimeMillis()-start));
		}
		catch (Exception Ee){Ee.printStackTrace(); return whoWhat.failed ; }
		return Answer;
	}

	/** call :  Method implementing Callable interface, returns a future <string> **/
	public String call(){
		try{
			duckduckGet();
			return Answer;
		}
		catch (Exception ee){ee.printStackTrace(); return whoWhat.failed;}
	}

}
