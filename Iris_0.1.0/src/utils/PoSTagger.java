package utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class PoSTagger implements Runnable{

	private boolean loaded;
	private MaxentTagger tagger;
	public PoSTagger(){
	}

	public String Tag(String Utterance) throws Exception{
		if(tagger == null || loaded ==false){
			throw new Exception("No Grammar Loaded Exception");
		}

		return tagger.tagString(Utterance);
	}

	public void run(){

		// Redirect the error Stream Temporarily ...
		try {
			PrintStream oldErr = System.err;
			PrintStream newErr = new PrintStream(new ByteArrayOutputStream());
			System.setErr(newErr);

			tagger = new MaxentTagger(System.getProperty("user.dir")+"/misc/left3words-wsj-0-18.tagger");
			//@SuppressWarnings("unchecked")
			System.setErr(oldErr);
			loaded = true;
		} 
		catch (IOException e) {
			e.printStackTrace();
			loaded = false;
		} 
		catch (ClassNotFoundException e) {
			e.printStackTrace();
			loaded = false;
		}

	}


}
