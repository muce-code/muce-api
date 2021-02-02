package nl.thedutchmc.muce;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.PropertySource;

import nl.thedutchmc.muce.database.Database;

@SpringBootApplication
@PropertySource("classpath:application.properties")
public class App {

	private Config config;
	
	private static Docker docker;
	private static Database database;
	
    public static void main(String[] args) {
     
    	//Intialize the application
    	new App().start();
    	
    	//Start the Spring boot webserver
    	new SpringApplicationBuilder(App.class)
    		.bannerMode(Banner.Mode.OFF)
    		.logStartupInfo(false)
    		.run(args);
    	
    	App.logInfo("Startup complete.");
    }
    
    /**
     * Initialize all the required components for the application
     */
    private void start() {
    	
    	//Read the config file
    	config = new Config();
    	config.read();
    	
    	//Create an instance of the Docker class
    	docker = new Docker();
    	
    	//Initialize the connection to the database
    	database = new Database(
    			(String) config.getConfigValue("dbHost"), 
    			(String) config.getConfigValue("dbName"), 
    			(String) config.getConfigValue("dbUsername"), 
    			(String) config.getConfigValue("dbPassword"));
    }
    
    /**
     * Get the instance of the Database class
     */
    public static Database getDatabase() {
    	return App.database;
    }
    
    /**
     * Get the instance of the Docker class
     * @return
     */
    public static Docker getDocker() {
    	return App.docker;
    }
    
	/**
	 * Save a resource from inside the JAR to a directory
	 * @param name The name of the resource to save
	 * @param targetPath The path to save the resource to
	 */
	public static void saveResource(String name, String targetPath) {
		InputStream in = null;
		
		try {
			in = App.class.getResourceAsStream("/" + name);
			
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
    
	/**
	 * Log an Object with log level ERROR
	 * @param log The object to log
	 */
	public static void logError(Object log) {
		final DateTimeFormatter f = DateTimeFormatter.ofPattern("HH:mm:ss");
		System.err.println("[" + LocalTime.now(ZoneId.systemDefault()).format(f) + "][ERROR] " + log);
	}
	
	/**
	 * Log an Object with log level INFO
	 * @param log The object to log
	 */
	public static void logInfo(Object log) {
		final DateTimeFormatter f = DateTimeFormatter.ofPattern("HH:mm:ss");
		System.out.println("[" + LocalTime.now(ZoneId.systemDefault()).format(f) + "][INFO] " + log);
	}
}
