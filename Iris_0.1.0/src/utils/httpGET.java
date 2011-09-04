package utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javazoom.jl.decoder.JavaLayerException;


/*** 
 * A Simple Class with only the method download, for getting http responses with JAVA.net class
 * 
 * Throws IOException in the event that the connection is not established
 * Warning: Do not rely on the IOException to determine whether or not the content is correct
 * 
 * Download is a thread-safe static method
 * 
 * **/

public class httpGET {

	/**Executes an http GET request to the given url, with the specified User Agent String
	 * Returns response as a string
	 **/

	public static String download(String url2get, Object lock, String UserAgent) 
	throws IOException, InterruptedException{

		// Safe approach to setting http.agent system property
		synchronized(lock){
			System.setProperty("http.agent","");
		}
		// Set up Strings
		String buffer;
		String response="";

		// Set up URL connection
		java.net.URLConnection urlConnect = new URL(url2get).openConnection();
		urlConnect.setRequestProperty("User-Agent", UserAgent);

		// URL connection inputstream is read into response
		BufferedReader br = new BufferedReader(new InputStreamReader(urlConnect.getInputStream()));
		while ((buffer = br.readLine()) != null) {
			response += buffer;
			if (Thread.interrupted()){
				br.close();
				throw new InterruptedException("Thread Task Cancelled");
			}
		}
		br.close();
		return response;
	}

	/**Quick Way to call the download method, setting default UserAgent to Mozilla\5.0**/

	public static String download(String url2get,Object lock) throws IOException, InterruptedException{
		// Set default UserAgent property to Mozilla\5.0
		String UserAgent = "Mozilla/5.0";
		return httpGET.download(url2get,lock,UserAgent);
	}

	public static byte[] downloadToByteArray(String url2get,Object lock) throws IOException, InterruptedException, JavaLayerException{
		// Set default UserAgent property to Mozilla\5.0
		String UserAgent = "Mozilla/5.0";
		return httpGET.downloadToByteArray(url2get,lock,UserAgent);
	}

	public static byte [] downloadToByteArray(String url2get, Object lock, String UserAgent) 
	throws IOException, InterruptedException{

		// Safe approach to setting http.agent system property
		synchronized(lock){
			System.setProperty("http.agent","");
		}

		// Set up URL connection
		int readLength;
		byte [] buffer = new byte[1024];
		java.net.URLConnection urlConnect = new URL(url2get).openConnection();
		urlConnect.setRequestProperty("User-Agent", UserAgent);

		// URL connection inputstream is read into byte array
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

		// Open connection and stream to inputStream
		InputStream iStream = new BufferedInputStream(urlConnect.getInputStream());

		// Read the GET Response into the ByteArray Stream
		while (-1!=(readLength=iStream.read(buffer))){
			byteStream.write(buffer, 0, readLength);
			if (Thread.interrupted()){
				byteStream.close();
				iStream.close();
				throw new InterruptedException("Thread Task Cancelled");
			}
		}

		// Close the Streams
		byteStream.close();
		iStream.close();

		// Return byte array with MP3 files
		return byteStream.toByteArray();
	}

	public static InputStream downloadInputStream(String url2get,Object lock) throws IOException, InterruptedException, JavaLayerException{
		// Set default UserAgent property to Mozilla\5.0
		String UserAgent = "Mozilla/5.0";
		return httpGET.downloadInputStream(url2get,lock,UserAgent);
	}

	public static InputStream downloadInputStream(String url2get, Object lock, String UserAgent) throws IOException{ 

		// Safe approach to setting http.agent system property
		synchronized(lock){
			System.setProperty("http.agent","");
		}

		// Set up URL connection
		java.net.URLConnection urlConnect = new URL(url2get).openConnection();
		urlConnect.setRequestProperty("User-Agent", UserAgent);

		// Open connection and stream to inputStream
		InputStream iStream = new BufferedInputStream(urlConnect.getInputStream());
		return iStream;
 	}
}
