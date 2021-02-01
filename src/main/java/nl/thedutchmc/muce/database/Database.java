package nl.thedutchmc.muce.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.json.JSONObject;

import nl.thedutchmc.muce.App;

public class Database {

	private final SqlManager sqlManager;
	
	public Database(String dbHost, String dbName, String dbUsername, String dbPassword) {
		sqlManager = new SqlManager(dbHost, dbName, dbUsername, dbPassword);
		
		if(!checkDb(dbName)) {
			App.logInfo("Database not set up. Initializing...");
			initDb(dbName);
		}
	}
	
	public boolean removeSession(String sessionId) {
		String sql = "DELETE FROM sessions WHERE sessionId=?";
		
		try {
			PreparedStatement pr = sqlManager.createPreparedStatement(sql);
			pr.setString(1, sessionId);
			
			return true;
		} catch(SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private boolean checkDb(String dbName) {
		String sql = "SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ?";
		
		List<String> requiredTables = new ArrayList<>(Arrays.asList(
				"accounts",
				"sessions"
		));
		
		try {
			PreparedStatement pr = sqlManager.createPreparedStatement(sql);
			pr.setString(1, dbName);
			ResultSet rs = sqlManager.executeFetchQuery(pr);
			
			List<String> allTableNames = new ArrayList<>();
			while(rs.next()) {
				String tableName = rs.getString("TABLE_NAME");
				allTableNames.add(tableName);
			}
			
			if(!allTableNames.isEmpty() && !allTableNames.containsAll(requiredTables)) {
				App.logInfo("Your database is not complete. Please drop it!");
				System.exit(1);
			}
			
			return !allTableNames.isEmpty();
			
		} catch(SQLException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	private void initDb(String dbName) {
		String[] createTablesSql = new String[] {
				"CREATE TABLE `" + dbName + "`.`accounts` ( `userid` BIGINT NOT NULL , `email` VARCHAR(400) NOT NULL , `password` VARCHAR(400) NOT NULL , `username` VARCHAR(400) NOT NULL, `salt` VARCHAR(1000) NOT NULL, PRIMARY KEY (`userid`)) ENGINE = InnoDB;",
				"CREATE TABLE `" + dbName + "`.`sessions` ( `userid` BIGINT NOT NULL , `sessionid` VARCHAR(400) NOT NULL , `fakeuserid` BIGINT NOT NULL, PRIMARY KEY (`userid`)) ENGINE = InnoDB;"
		};
				
		try {
			for(String s : createTablesSql) {
				PreparedStatement pr = sqlManager.createPreparedStatement(s);
				sqlManager.executePutQuery(pr);
			}
		} catch(SQLException e) {
			e.printStackTrace();
		}
	}

	public String getEmail(String fakeUserId) {
		String sql = "SELECT accounts.email FROM accounts LEFT JOIN sessions ON accounts.userid=sessions.userid wHERE sessions.fakeuserid=?";
		
		try {
			PreparedStatement pr = sqlManager.createPreparedStatement(sql);
			pr.setLong(1, Long.valueOf(fakeUserId));
			
			ResultSet rs = sqlManager.executeFetchQuery(pr);
			while(rs.next()) {
				JSONObject result = new JSONObject();
				result.put("success", true);
				result.put("email", rs.getString("email"));
			
				return result.toString();
			}
		} catch(SQLException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public String login(String email, String password) {
		
		String sql = "SELECT accounts.username, accounts.userid, sessions.sessionid, sessions.fakeuserid FROM accounts LEFT JOIN sessions ON accounts.userid=sessions.userid WHERE accounts.email=? AND accounts.password=?";
				
		try {
			PreparedStatement pr = sqlManager.createPreparedStatement(sql);
			pr.setString(1, email);
			pr.setString(2, password);
			
			ResultSet rs = sqlManager.executeFetchQuery(pr);			
						
			JSONObject result = new JSONObject();

			if(!rs.next()) {
				result.put("valid", false);
				return result.toString();
			} else {
				do {
					result.put("valid", true);
					result.put("userid", rs.getLong("userid"));
					result.put("username", rs.getString("username"));
					result.put("sessionid", rs.getString("sessionid"));
					result.put("fakeuserid", rs.getLong("fakeuserid"));
				} while(rs.next());
			}
			
			return result.toString();
		} catch(SQLException e) {
			e.printStackTrace();
		}
		
		return "";
	}

	public String getSalt(String email) {
		String sql = "SELECT salt FROM accounts WHERE email=?";
		
		try {
			PreparedStatement pr = sqlManager.createPreparedStatement(sql);
			pr.setString(1, email);
			
			ResultSet rs = sqlManager.executeFetchQuery(pr);
			
			while(rs.next()) {
				return rs.getString("salt");
			}
			
		} catch(SQLException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public String register(String email, String password, String username, String salt) {
		String sql = "INSERT INTO accounts (userid, email, password, username, salt) VALUES (?, ?, ?, ?, ?)";
		
		long userId = Math.abs(new Random().nextLong());
		try {
			PreparedStatement pr = sqlManager.createPreparedStatement(sql);
			pr.setLong(1, userId);
			pr.setString(2, email);
			pr.setString(3, password);
			pr.setString(4, username);
			pr.setString(5, salt);
			
			sqlManager.executePutQuery(pr);
			
			JSONObject response = new JSONObject();
			response.put("success", true);
			response.put("userid", userId);
			
			return response.toString();
		} catch(SQLException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public boolean accountExists(String email) {
		String sql = "SELECT userId FROM accounts WHERE email=?";
		
		try {
			PreparedStatement pr = sqlManager.createPreparedStatement(sql);
			pr.setString(1, email);
			
			ResultSet rs = sqlManager.executeFetchQuery(pr);
			
			return rs.next();
		} catch(SQLException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public boolean sessionExists(String sessionId, long fakeUserId) {
		String sql = "SELECT * FROM sessions WHERE sessionid=? AND fakeuserid=?";
		try {
			PreparedStatement pr = sqlManager.createPreparedStatement(sql);
			pr.setString(1, sessionId);
			pr.setLong(2, fakeUserId);
			
			ResultSet rs = sqlManager.executeFetchQuery(pr);
			
			return rs.next();
		} catch(SQLException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public String getRealUser(long fakeUserId) {
		String sql = "SELECT userid FROM sessions WHERE fakeuserid=?";
		try {
			PreparedStatement pr = sqlManager.createPreparedStatement(sql);
			pr.setLong(1, fakeUserId);
			
			ResultSet rs = sqlManager.executeFetchQuery(pr);
			
			JSONObject result = new JSONObject();
			while(rs.next()) {
				result.put("success", true);
				result.put("userid", rs.getLong("userid"));
				
				return result.toString();
			}			
		} catch(SQLException e) {
			e.printStackTrace();
		}
		
		return "";
	}
	
	public String createSession(long userId) {
		String sql = "INSERT INTO `sessions` (`userId`, `sessionid`, `fakeuserid`) VALUES (?, ?, ?)";
		
		String sessionId = UUID.randomUUID().toString();
		long fakeUserId = Math.abs(new Random().nextLong());
		
		try {
			PreparedStatement pr = sqlManager.createPreparedStatement(sql);
			pr.setLong(1, userId);
			pr.setString(2, sessionId);
			pr.setLong(3, fakeUserId);
			
			sqlManager.executePutQuery(pr);
			
			JSONObject result = new JSONObject();
			result.put("sessionid", sessionId);
			result.put("fakeuserid", fakeUserId);
			
			return result.toString();
			
		} catch(SQLException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
