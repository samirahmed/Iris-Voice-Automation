package modules;

import java.util.concurrent.Callable;

public interface IrisModule extends Callable<String[]>{

	public String[] getPrimaryKeys();

	public String[] getRegExKeys();
	
	public void setUtterance(String Utterance);
}
