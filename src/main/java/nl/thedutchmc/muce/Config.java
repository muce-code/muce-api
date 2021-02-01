package nl.thedutchmc.muce;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
		
		if(!configFile.exists()) {
			saveResource("config.yml", configDirectory.getAbsolutePath());
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
	
	/**
	 * Save a resource from inside the JAR to a directory
	 * @param name The name of the resource to save
	 * @param targetPath The path to save the resource to
	 */
	private void saveResource(String name, String targetPath) {
		InputStream in = null;
		
		try {
			in = this.getClass().getResourceAsStream("/" + name);
			
			if(name == null) {
				throw new FileNotFoundException("Cannot find file " + name + " in jar!");
			}
			
			if(in == null) {
				throw new FileNotFoundException("Cannot find file " + name + " in jar!");
			}
			
			Path exportPath = Paths.get(targetPath + File.separator + name);
			Files.copy(in, exportPath);
		} catch (FileNotFoundException e) {
			App.logError("A FileNotFoundException was thrown whilst trying to save " + name + ". Use --debug for more details.");
			App.logError(Utils.getStackTrace(e));
		} catch (IOException e) {
			App.logError("An IOException was thrown whilst trying to save " + name + ". Use --debug for more details.");
			App.logError(Utils.getStackTrace(e));
		}
	}
}
