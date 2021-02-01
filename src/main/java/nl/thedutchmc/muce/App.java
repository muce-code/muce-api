package nl.thedutchmc.muce;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
		SpringApplication.run(App.class, args);
    }
    
    /**
     * Initialize all the required components for the application
     */
    private void start() {
    	
    	//Read the config file
    	config = new Config();
    	config.read();
    	
    	//Create an instance of the Docker class
    	docker = new Docker((String) config.getConfigValue("mucepath"));
    	
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
