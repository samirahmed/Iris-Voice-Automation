package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;

/**
 *  Using HTTPClient to POST .flac recording file to Google.com Speech API
 *  Expect Response Times ~ 2 seconds.  But vary based on the length of the recorded file
 *  Do not create multiple objects for concurrent use.
 */

public class httpPOST {

	/**Constructor will setup httpclient, post request method and useragent information as required*/
	public httpPOST() {
		httpclient = new DefaultHttpClient();
		System.setProperty("http.agent","");
		httppost = new HttpPost(speechAPIURL);
		HttpProtocolParams.setUserAgent(httpclient.getParams(), User_Agent);
		httppost.setHeader(HeaderType,HeaderContent);
	}

	/** This file will post the flac file to google and store the Json String in jsonResponse data member*/
	private void postFLAC(){
		try{
			//long start = System.currentTimeMillis();

			// Load the file stream from the given filename
			File file = new File(FLACFileName);

			InputStreamEntity reqEntity = new InputStreamEntity(
					new FileInputStream(file), -1);

			// Set the content type of the request entity to binary octet stream.. Taken from the chunked post example HTTPClient 
			reqEntity.setContentType("binary/octet-stream");
			//reqEntity.setChunked(true); // Uncomment this line, but I feel it slows stuff down... Quick Tests show no difference


			// set the POST request entity...
			httppost.setEntity(reqEntity);

			//System.out.println("executing request " + httppost.getRequestLine());

			// Create an httpResponse object and execute the POST
			HttpResponse response = httpclient.execute(httppost);

			// Capture the Entity and get content
			HttpEntity resEntity = response.getEntity();

			//System.out.println(System.currentTimeMillis()-start);

			String buffer;
			jsonResponse = "";

			br = new BufferedReader(new InputStreamReader(resEntity.getContent()));
			while ((buffer = br.readLine()) != null) {
				jsonResponse += buffer;
			}


			//System.out.println("Content: "+jsonResponse);

			// Close Buffered Reader and content stream.
			EntityUtils.consume(resEntity);
			br.close();
		}
		catch(Exception ee){
			// In the event this POST Request FAILED
			ee.printStackTrace();
			jsonResponse = "_failed_";
		}
		finally{
			// Finally shut down the client
			httpclient.getConnectionManager().shutdown();
		}
	}

	/** postFile - Only public facing method of HTTPPOST, requires that you pass to it the filename*/
	public String postFile(String fileName){
		
		// Assuming we have a valid file name we call private postFLAC method
		if (fileName == null || fileName.equals("") || !fileName.contains(".flac")){
			jsonResponse = "_failed_";
		}
		else {
			FLACFileName= fileName;
			postFLAC();
		}
		return jsonResponse;
	}
	
	/**Data Members*/

	private HttpClient httpclient;
	private HttpPost httppost;
	private BufferedReader br;
	private String jsonResponse;
	private String FLACFileName;

	// Immutable data members
	private final String speechAPIURL = "http://www.google.com/speech-api/v1/recognize?lang=en-us&client=chromium";
	private final String HeaderType = "Content-Type";
	private final String HeaderContent = "audio/x-flac; rate=16000";
	private final String User_Agent = "Mozilla/5.0";
	
	/**Test function main...*/
	public static void main(String[] args) {
		// Test the post Request ...
		httpPOST flacPost = new httpPOST();
		flacPost.postFile(System.getProperty("user.dir")+"/recording.flac");
	}
}