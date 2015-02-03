package com.actelion.research.gui.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLEncoder;

import com.actelion.research.util.Config;

/**
 * Wrapper to the Logon service
 * @author freyssj
 */
public class ApplicationErrorLog {

	private static boolean activated = true;
	public static final String ACTION_LOGON = "Logon";

	public static void setActivated(boolean activated) {
		ApplicationErrorLog.activated = activated;
	}
	public static boolean isActivated() {
		return activated;
	}
	
	public static void logException(final Throwable exception) {
		logException("Error", exception);
	}
	public static void logException(final String application, final Throwable exception) {
		if(application==null) throw new IllegalArgumentException("Application cannot be null");
		if(exception==null) throw new IllegalArgumentException("exception cannot be null");
		
		System.out.println("ApplicationErrorLog.logException() "+exception);
		
		new Thread() {
			@Override
			public void run() {
				try {
					StringWriter s = new StringWriter(100);
					PrintWriter pw = new PrintWriter(s);
					exception.printStackTrace(pw);
					String userId = System.getProperty("user.name");
					java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
					String pcName = localMachine.getHostName();

					String error = URLEncoder.encode(s.toString(), "UTF-8");
					final int maxLength = 2000;
					if(error.length()>maxLength) {
						int index = error.lastIndexOf("%0A", maxLength);
						error = error.substring(0, index<0? maxLength: index);
					}
					
					URL url = new URL("http://" + Config.SERVER + ":8080/dataCenter/errorLog.do?app=" + URLEncoder.encode(application, "UTF-8") 
							+ "&user="+URLEncoder.encode(userId, "UTF-8")
							+ "&pcname=" + URLEncoder.encode(pcName, "UTF-8")
							+ "&stacktrace="+error);
					System.out.println("URL="+url.toString().length()+":"+url);
					url.getContent();
				} catch(Throwable e) {
					e.printStackTrace();
				}				
			}
		}.start();
	}
	
	public static void main(String[] args) throws Exception {
		try {
			//Should record
			recError(100);
			
		} catch(Throwable e) {
			JExceptionDialog.show(e);
		}
		
	}
	
	public static void recError(int n) {
		if(n==0) throw new IllegalArgumentException("TEST");
		if(n%2==0) recError(n-1);
		else recError(n-1);
	}
		
	
}
