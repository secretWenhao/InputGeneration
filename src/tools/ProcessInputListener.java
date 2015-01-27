package tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.Callable;

public class ProcessInputListener implements Callable<String>{

	private BufferedReader in;
	
	public ProcessInputListener(BufferedReader in) {
		this.in = in;
	}
	
	public String call() {
		String result = "";
		try {

			result = in.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	
}
