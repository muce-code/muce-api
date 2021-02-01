package nl.thedutchmc.muce.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import nl.thedutchmc.muce.App;
import nl.thedutchmc.muce.Utils;

public class SqlManager {

	private Connection connection;
	
	private String dbHost, dbName, dbUsername, dbPassword;
	
	public SqlManager(String dbHost, String dbName, String dbUsername, String dbPassword) {
		this.dbHost = dbHost;
		this.dbName = dbName;
		this.dbUsername = dbUsername;
		this.dbPassword = dbPassword;
		
		App.logInfo("Initializing database connection...");
		
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch(ClassNotFoundException e) {
			App.logError("Unable to initialize the database connection! MySQL driver not found");
			App.logError(Utils.getStackTrace(e));
			System.exit(1);
		}
		
		App.logInfo("Connecting to database...");
		this.connection = connect();
		
		if(this.connection == null) {
			App.logError("Unable to establish connection with database!");
			System.exit(1);
		}
	}
	
	/**
	 * Connect to the database
	 * @return Returns the Connection, null if an error occured
	 */
	private Connection connect() {
		try {
			return DriverManager.getConnection("jdbc:mysql://" + dbHost + "/" + dbName + "?user=" + dbUsername + "&password=" + dbPassword);
		} catch(SQLException e) {
			App.logError("Unable to establish connection to database!");
			App.logError(Utils.getStackTrace(e));
			return null;
		}
	}
	
	/**
	 * Execute a fetch query to the database
	 * @param preparedStatement PreparedStatement to execute
	 * @return Returns a ResultSet with the results from the database
	 * @throws SQLException
	 */
	public ResultSet executeFetchQuery(PreparedStatement preparedStatement) throws SQLException {
		if(connection.isClosed()) {
			App.logInfo("Connection to database lost. Reconnecting!");
			connection = connect();
		}
		
		return preparedStatement.executeQuery();
	}
	
	/**
	 * Execute a put statement (Insert/Update etc)
	 * @param preparedStatement PreparedStatement to execute
	 * @return Returns the status code returned by the database.
	 * @throws SQLException
	 */
	public int executePutQuery(PreparedStatement preparedStatement) throws SQLException {
		if(connection.isClosed()) {
			App.logInfo("Connection to database lost. Reconnecting!");
			connection = connect();
		}
		return preparedStatement.executeUpdate();
	}
	
	/**
	 * Create a PreparedStatement
	 * @param sql SQL statement to use for the PreparedStatement
	 * @return Returns the created PreparedStatement
	 * @throws SQLException
	 */
	public PreparedStatement createPreparedStatement(String sql) throws SQLException {
		if(connection.isClosed()) {
			App.logInfo("Connection to database lost. Reconnecting!");
			connection = connect();
		}
		return connection.prepareStatement(sql);
	}
}
