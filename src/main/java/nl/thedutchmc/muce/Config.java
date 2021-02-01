package nl.thedutchmc.muce;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.yaml.snakeyaml.Yaml;

public class Config {
 
	private File configDirectory;
	private HashMap<String, Object> configData = null;
	
	public Config() {
		try {
			final File jarPath = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
			configDirectory = new File(jarPath.getParentFile().getPath());
		} catch(URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Read the configuration file
	 */
	public void read() {
		Yaml yaml = new Yaml();
		File configFile = new File(configDirectory, "config.yml");
		
		App.logInfo("Configuration file at: " + configFile.getAbsolutePath());
		
		if(!configFile.exists()) {
			App.saveResource("config.yml", configDirectory.getAbsolutePath());
		}
		
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(configFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		configData = yaml.load(fis);
	}
	
	/**
	 * Get a configuration value
	 * @param name The name of the key-value pair
	 * @return Returns the value associated with the key. Null if the key does not exist
	 */
	public Object getConfigValue(String name) {
		return this.configData.get(name);
	}
}
