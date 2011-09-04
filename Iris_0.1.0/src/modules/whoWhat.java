package modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/** whoWhat : Class 
 * 
 * This class is a base class for implementing getWiki getBing and getDDG objects
 * 
 * Time : Dependant on the source. But anywhere between 200 (duckduckgo) and 1000 ms (bing to wiki) on average
 * 
 * Description:  This class will use the executorService passed to it search for results by querying multiple sources
 * DuckDuckGo.com Mobile.Wikipedia.Org www.Bing.com (+ Site:wikipedia.org)
 * 
 * Tests with ~200 search results have shown 95+ Percent accuracy, assuming correct utterance
 * see optimizedResults.txt in /misc folder
 * 
 * **/
public class whoWhat implements IrisModule{

	/** whoWhat testing with main **/
	public static void main(String[] args) {
		// Test execution...
		ExecutorService exs= Executors.newFixedThreadPool(20);
		whoWhat ww = new whoWhat(exs);
		ww.setUtterance("what is lilypad");
		String Answer[] = ww.call();
		System.out.println(Answer[0]+"\n"+Answer[1]);
		exs.shutdown();
	}

	/** Constructor will take in the executor service and create the HashSets fro**/
	public whoWhat(ExecutorService ExServ){
		exs = ExServ;
		nextWord  = new HashSet<String>(Arrays.asList(verbsArray));
		firstWord =  new HashSet<String>(Arrays.asList(interrogativeArray));
	}

	/** Set the Utterance **/
	public void setUtterance(String utterance){
		command = utterance;
	}

	/** call - to implement callable interface we use this public facing function**/
	public String[] call(){
		return new String [] {getAnswer(),openURL};
	}

	/** Main Execution function, will create threads and poll them in the order of preference **/
	private String getAnswer(){
		// query = parseCommand(query);
		long start = System.currentTimeMillis();
		this.query = parseUtterance(command);
		if ( query == null || query.equals("") || query.matches("\\s*?the\\s*?")){
			openURL = googleURL+command;
			return whoWhat.noAnswer;
		}

		generateList();
		try{
			isFound = false;
			ArrayList<Future<String>> futureList = new ArrayList<Future<String>> (taskList.size());
			int count = 0;
			for (Callable<String> task : taskList){
				futureList.add(count++,exs.submit(task));
			}
			// = exs.invokeAll(taskList, 5000, TimeUnit.MILLISECONDS);

			count = 1;
			for(Future<String> future : futureList)
			{
				if(isFound){
					future.cancel(true);
					//System.out.println("Cancelled threaded task " + count);
				}
				else{
					String response = future.get();
					//System.out.println("Polling threaded task "+ count);
					decide(response);
				}
				count++;
			}

			System.out.println("Source: Wikipedia.org - Query Response Time: " + (System.currentTimeMillis()-start) + " ms");
			return Answer;	
		}
		catch(Exception ee){
			ee.printStackTrace(); 
			Answer = "Oh my! I am experiencing technical difficulties...";
			openURL = googleURL+command;
			return Answer;
		}
	}

	/**Extact the desired query from an utterance**/
	private String parseUtterance(String command){
		// Remove interrogativePronouns from first word

		commandArray = command.split("\\s");
		int startIndex= 1 ;

		// Parse trying to find target query
		if (commandArray.length < 2){return "";}
		else if (!firstWord.contains(commandArray[0])){ return "";}
		while(nextWord.contains(commandArray[startIndex])){
			if (startIndex+1 < commandArray.length)
			{startIndex++;}
		}
		if(commandArray[startIndex-1].matches("\\s*the\\s*")){startIndex--;}

		// Combine rest of the string into single query variable and return
		String content="";
		while(startIndex != commandArray.length) {
			content+=commandArray[startIndex]+" ";
			startIndex++;
		}
		content=content.substring(0,content.length()-1);
		return content;
	}

	/** Generate a List of Tasks in order of priority**/
	private ArrayList<Callable<String> > generateList(){

		if (command.matches("\\s*the\\s.*")){
			taskList = new ArrayList<Callable<String> >(6);
			query2 = query.replaceFirst("\\s*the\\s","");
			//System.out.println("Putting in the");
			taskList.add(0, new getDDG(query));
			taskList.add(1, new getWiki(query));
			taskList.add(2, new getBing(query));
			taskList.add(3, new getDDG(query2));
			taskList.add(4, new getWiki(query2));
			taskList.add(5, new getBing(query2));
		}
		else{
			taskList = new ArrayList<Callable<String> >(3);
			taskList.add(0, new getDDG(query));
			taskList.add(1, new getWiki(query));
			taskList.add(2, new getBing(query));
		}		
		return taskList;
	}

	/** Generates answer based on the result from each thread **/
	private void decide(String taskResponse){

		if (taskResponse== null || taskResponse.equals("")){
			Answer = "Oh my goodness. I have no answer! Sorry!";
			openURL = googleURL+command;
		}
		else if(taskResponse.equals(whoWhat.failed)){
			Answer = "Oops... I am having technical difficulties. Let us try a Google Search instead?";
			openURL = googleURL+command;
		}
		else if (taskResponse.equals(whoWhat.noAnswer)){
			Answer = "I am stumped! But let us see what Google says!";
			openURL = googleURL+command;
		}
		else if (taskResponse.equals(whoWhat.isAmbiguous)){
			Answer = "Query is too ambiguous! I will bring up a Google Search!";
			openURL = googleURL+command;
		}
		else {
			Answer = taskResponse;
			isFound = true;
			System.out.print("Answer Found! ");
			openURL = "";
		}
	}

	/** DATAMEMBERS **/
	public final String[] interrogativeArray = {
			"what",
			"whats",
			"who",
			"look",
			"lookup",
			"whose",
			"whos",
			"what's",
			"who's",
			//"is" // TODO: Incase the who is not heard
	};

	public final String[] verbsArray={
			"is",
			"a",
			"are",
			"were",
			"was",
			"an",
			"for",
			"the",
	};

	public String[] getPrimaryKeys(){
		// Return all the possible first words from interrogative  array
		return interrogativeArray;
	}

	public String[] getRegExKeys(){
		// Return all the keys about with anything after them

		String [] RegExKeys = new String[interrogativeArray.length];
		int index = 0;
		for (String keys : interrogativeArray){
			RegExKeys[index++] = keys+".*";
		}
		return RegExKeys;
	}

	private final String googleURL= "http://www.google.com/search?q=";

	private String openURL;
	private HashSet<String> firstWord;
	private HashSet<String> nextWord;
	private String commandArray [];
	private String query;
	private String query2;
	private String Answer;
	private String command;
	private ArrayList<Callable<String> > taskList;
	private boolean isFound;

	/** Immutable Data Members **/
	public static final String noAnswer = "_noAnswer_";
	public static final String isAmbiguous = "_isAmbiguous_";
	public static final String failed = "_failed_";
	public static final String cancelled = "_connectionInterrupted_";
	private final ExecutorService exs;

}
