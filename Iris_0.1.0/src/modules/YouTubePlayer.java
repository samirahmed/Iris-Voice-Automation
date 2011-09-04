package modules;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import utils.httpGET;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import core.Synthesizer;
import core.openBrowser;

public class YouTubePlayer implements IrisModule{

	/**
	 *  Valid Commands
	 * 
	 *  will search for play.
	 *  if we have the term song we flag for music
	 *  if we have the term music video we flag for music
	 *  if we have the term trailer
	 *	play
		play the song | <target>
		play the music video
		play the music video of 
		play the trailer for
		play the trailer
		play the clip
		play the video
	 * 
	 * 
	 * **/
	private String parseUtterance() throws IOException{
		String Query="";
		tag="";
		if(utterance == null || utterance.equals("")){
			throw new IOException();
		}
		else if(utterance.matches("youtube .*?")){
			Query = utterance.replaceFirst("youtube ","");
		}
		else if(utterance.matches("play the song .*?")){
			tag = YouTubePlayer.musicTag;
			Query = utterance.replaceFirst("play the song ","");
		}
		else if (utterance.matches("play the video.*?")){
			Query = utterance.replaceFirst("play the video ","");
		}
		else if (utterance.matches("play the clip.*?")){
			Query = utterance.replaceFirst("play the clip ","");
		}		
		else if (utterance.matches("play .*?")){			

			if (utterance.contains("music video")){
				tag = YouTubePlayer.musicTag;
			}
			if (utterance.contains("trailer for")){
				tag = YouTubePlayer.movieTag;
			}
			Query = utterance.replaceFirst("play","");
		}
		return Query;
	}

	public YouTubePlayer(){
	}

	private String[] player() throws IOException{
		String Query = parseUtterance();
		System.out.println(Query);
		String JSON = tubeSearch(Query);

		if (JSON.equals(YouTubePlayer.failed)){
			throw new IOException();
		}

		String info[] = extraction(JSON);

		if (info[0].equals(YouTubePlayer.failed) || info[1].equals(YouTubePlayer.failed)){
			throw new IOException();
		}
		else if (info[0].equals(YouTubePlayer.noAnswer) || info[1].equals(YouTubePlayer.noAnswer)){
			return new String[] {"I'm Sorry, I couldn't find anything relevant to what you are looking for",""};
		}
		String playURL = YouTubePlay+info[1];
		String Answer = "Playing " + info[0];
		return new String[] {Answer,playURL};
	}

	public String[] call(){
		try{
			return player();
		}
		catch(Exception ee){
			ee.printStackTrace();
			return new String[] {"Ooops. Looks like I am having technical difficulties",""};
		}
	}

	private String[] extraction(String JSON){
		try{

			if (JSON.contains("title") && JSON.contains("comments")){
				JsonObject YouTubeJO = new JsonParser().parse(JSON).getAsJsonObject();

				JsonObject entry = YouTubeJO
				.getAsJsonObject("feed")
				.getAsJsonArray("entry")
				.get(0)
				.getAsJsonObject();

				String title = entry
				.getAsJsonObject("title")
				.get("$t")
				.toString();

				String videoID = entry
				.getAsJsonObject("gd$comments")	
				.getAsJsonObject("gd$feedLink")
				.get("href")
				.toString();

				// Check if we have nulls and empty strings, if not, remove all content within parenthesis
				if (title == null || title.equals("")){
					title = YouTubePlayer.failed;
				}
				else {
					title= title.replaceAll("\\(.*?\\)","");
					title= title.replaceAll("\"","");
				}

				// Check if the video ID failed, if not, extracted the ID from the URL
				if(videoID == null || videoID.equals("")){
					videoID = YouTubePlayer.failed;
				}
				else{
					int startIndex = videoID.indexOf(spliceStart);
					int stopIndex = videoID.indexOf(spliceEnd);
					videoID = videoID.substring(startIndex+spliceStart.length(), stopIndex);
				}	

				return new String[] { title,videoID};
			}
			else{
				return new String[] { YouTubePlayer.noAnswer,YouTubePlayer.noAnswer};
			}
		}
		catch(Exception ee){
			ee.printStackTrace(); return new String[] {YouTubePlayer.failed,YouTubePlayer.failed};
		}
	}

	private String tubeSearch(String Query){
		// Create URL and safely encode
		try{
			String xQuery = URLEncoder.encode(Query,"UTF-8");
			String YouTubeURL = YouTubePrefix+xQuery+YouTubeSuffix+tag;
			String YouTubeJSON = "";
			try{
				YouTubeJSON = httpGET.download(YouTubeURL, this);
			}
			catch(InterruptedException InterEE){}
			return YouTubeJSON;
		}
		catch(Exception ee){
			ee.printStackTrace();
			return YouTubePlayer.failed;
		}
	}

	public void setUtterance(String newUtterance){
		if (newUtterance!=null && !newUtterance.equals("")){
			this.utterance = newUtterance;
			//this.originalUtterance = utterance;
		}
	}

	public static void main(String[] args){

		try{
			long start = System.currentTimeMillis();
			ExecutorService exs = Executors.newFixedThreadPool(20);
			openBrowser browser = new openBrowser();
			Synthesizer voice = new Synthesizer(exs);
			YouTubePlayer player = new YouTubePlayer();
			player.setUtterance("play the transformers 3 trailer");
			Future<String[]> answer = exs.submit(player);
			String [] info = answer.get();
			System.out.println(info[1]);
			System.out.println(System.currentTimeMillis()-start);
			voice.setAnswer(info[0]);
			Future<?> wait1 = exs.submit(voice);
			browser.setBrowserURL(info[1]);
			Future<?> wait2 = exs.submit(browser);
			wait1.get();
			wait2.get();
			exs.shutdown();
		}
		catch (Exception ee){
			ee.printStackTrace();
		}
	}

	private String utterance;
	//private String originalUtterance; 
	// Youtube Search Prefix
	private final String YouTubePrefix = "http://gdata.youtube.com/feeds/api/videos?q=";

	/*Youtube API Parameters include,
	 *Sort by relevance;
	 *Get one results; 
	 *Format as json; 
	 *Return only title and comments data;
	 **/
	private final String YouTubeSuffix = "&alt=json&orderby=relevance&max-results=1&v=2&fields=entry(title,gd:comments)";

	private final static String failed = "_failed_";
	private final static String noAnswer = "_noAnswer_";
	private final String spliceStart = "videos/";
	private final String spliceEnd = "/comment";
	private String tag;
	// Youtube Play video prefix
	private final String YouTubePlay = "http://www.youtube.com/watch?v=";
	private final static String musicTag = "&category=Music";
	private final static String movieTag = "&category=Movies";


	public String[] getPrimaryKeys() {
		return new String[]{
				"play",
				"youtube"
		};
	}

	public String[] getRegExKeys() {
		// TODO Auto-generated method stub
		return new String[]{
				"play/.*",
				"youtube/.*"
		};
	}
}
