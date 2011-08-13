package com.samir_ahmed.Iris;

import java.net.MalformedURLException;

public class openBrowser implements Runnable{
	
	/**Constructor : sets browserURL to "" **/
	public openBrowser(){
		this.browserURL="";
		
		// Extract OS Name and check for win, mac or linux, 
		// Selecting the appropriate default browser executing command
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("win")){ this.DEFAULT_BROWSER = this.DEFAULT_BROWSER_WINDOWS; }
		else if (osName.contains("mac")) {this.DEFAULT_BROWSER = this.DEFAULT_BROWSER_MAC;}
		else if (osName.contains("nix")) {this.DEFAULT_BROWSER = this.DEFAULT_BROWSER_LINUX;}
	}
	
	private String browserURL;

	// These commands have been tested on macs and linux only. I assume the pc one will work correctly ...
	private final String DEFAULT_BROWSER_LINUX = "x-www-browser";
	private final String DEFAULT_BROWSER_MAC = "open";
	private final String DEFAULT_BROWSER_WINDOWS = "start";
	private String DEFAULT_BROWSER;
	
	/**Sets the desired url for the browser
	 * will throw malformed url exception in the event it things the URL is malformed
	 * */
	public void setBrowserURL(String newBrowserURL) throws MalformedURLException{
		if (newBrowserURL != null && !newBrowserURL.equals("")){
			if (newBrowserURL.matches("http.*?")){
				this.browserURL=newBrowserURL;
			}
			else{
				throw new MalformedURLException("Woah Invalied URL");
			}
		}
		else{
			throw new MalformedURLException("Woah Invalied URL");
		}
	}

	/**Private getter for extracting desired URL the desired url for the browser
	 * will throw malformed url exception in the event it things the URL is malformed
	 * */
	private String getBrowserURL() throws MalformedURLException{
		if (browserURL != null && !browserURL.equals("")){
			if (browserURL.matches("http.*?")){
				return browserURL;
			}
			else{
				throw new MalformedURLException("Woah Invalied URL");
			}
		}
		else{
			throw new MalformedURLException("Woah Invalied URL");
		}
	}

	/** This function will create a browser with the following commands and let it be*/
	private synchronized void executeBrowser(){

		try{
			String[] browserCommand ={DEFAULT_BROWSER,getBrowserURL()};
			Process browser = Runtime.getRuntime().exec(browserCommand);
			browser.waitFor();		
		}
		catch(Exception ee){
			ee.printStackTrace();
		}
	}

	/**Function used for implementing the runnable interface*/
	public void run(){
		executeBrowser();
	}

	 //Main function, uncomment and include correct imports...
//	public static void main(String[] args) {
//		try{
//			ExecutorService exs = Executors.newFixedThreadPool(20);
//			openBrowser browser = new openBrowser();
//			browser.setBrowserURL("http://www.youtube.com/watch?v=YlUKcNNmywk");
//			Future<?> wait = exs.submit(browser);
//			wait.get();
//			exs.shutdown();	
//		}
//		catch(Exception ee){
//			ee.printStackTrace();
//		}
//	}
	
}

