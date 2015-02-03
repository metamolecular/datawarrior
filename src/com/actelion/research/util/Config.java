/*
 * Created on May 4, 2004
 *
 */
package com.actelion.research.util;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.prefs.Preferences;

/**
 * Singleton used to store the config relative to a project.<br>
 * The configuration is stored in ${user.home}/.${project}.properties
 * 
 * @author freyssj
 */
public class Config {
	
	/** Store the Singletons project->Config*/
	
	private static final Map<String, Config> configs = new HashMap<String, Config>();
	public static final String SERVER = "ares";
	public static final String SERVER_PORT = "http://ares:8080/";
	public static final String SERVER_DATA = "/u01/data/JBossData";
	
	
//	private File file;
//	private Properties properties;
	private Preferences preferences;
	
	private Config() {}
	
	/**
	 * Constructor. 
	 * Loads the properties from ${user.home}/.${project}.properties
	 * @param project
	 */
	private Config(String project) {
		Properties properties = new Properties();
		this.preferences = Preferences.userRoot().node("com.actelion.research."+project);
				
		//Import previous data
		File file = new File(System.getProperty("user.home"), "." + project + ".properties");
		if(file.exists()) {
			System.out.println("Config.Config() retrieve properties from legacy "+file);
			//Legacy System with properties
			try {
				FileInputStream is = new FileInputStream(file); 
				properties.load(is);
				for(Object key: properties.keySet()) {
					Object o = properties.get(key);
					if(o==null) continue;
					String s = o.toString();
					preferences.put((String) key, s);
				}
				is.close();
				boolean res = file.delete();
				System.out.println("Config.Config() file deleted="+res);
			} catch(Exception e) {
				//Nothing, new file
			}			
		}		
	}
	
	
	/**
	 * Gets or Creates the Config instance for the given project.<br>
	 * This method is synchronized
	 * @param project
	 * @return
	 */
	public static Config getInstance(String project) {
		Config config = configs.get(project);
		if(config==null) {
			synchronized(configs) {
				config = configs.get(project);
				if(config==null) {			 
					config = new Config(project);
					configs.put(project, config);
				}
			}
		} 
		return config;
	}

	public String getProperty(String key, String def) {
		return preferences.get(key, def);
	}
	  
	public int getProperty(String key, int def) {
		return preferences.getInt(key, def);
	}
	public long getProperty(String key, long def) {
		return preferences.getLong(key, def);
	}
	public boolean getProperty(String key, boolean def) {
		return preferences.getBoolean(key, def);
	}
	public File getProperty(String key, File def) {
		String v = getProperty(key, ""+def);		
		return new File(v); 
	}

	public List<String> getProperty(String key, List<String> def) {
		List<String> res = new ArrayList<String>();
		for(int i=0; ;i++) {
			String v = preferences.get(key + "." + i, null);
			if(v==null) break; 
			res.add(v);
		}
		if(res.size()==0) res = def;
		
		return res;
	}
	  
	public void setProperty(String key, boolean value) {
		preferences.putBoolean(key, value);
	}
	public void setProperty(String key, File file) {
		setProperty(key, file==null?"": file.getAbsolutePath());
	}
	public void setProperty(String key, String value) {
		preferences.put(key, value);
	}
	public void setProperty(String key, int value) {
		preferences.putInt(key, value);
	}
	public void setProperty(String key, long value) {
		preferences.putLong(key, value);
	}
	public void setProperty(String key, List<String> value) {
		for (int i = 0; i < value.size(); i++) {
			String s = value.get(i);
			preferences.put(key+"."+i, ""+s);			
		}
		for(int i=value.size(); preferences.get(key+"."+i, null)!=null; i++) {
			preferences.remove(key+"."+i);			
		}
	}
	
	
}
