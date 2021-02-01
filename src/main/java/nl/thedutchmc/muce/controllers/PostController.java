package nl.thedutchmc.muce.controllers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.tomcat.util.codec.binary.Base64;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import nl.thedutchmc.muce.App;
import nl.thedutchmc.muce.Utils;
import nl.thedutchmc.muce.database.Database;
import nl.thedutchmc.muce.database.Password;
import nl.thedutchmc.muce.types.Pair;

@RestController
@RequestMapping("/muce")
public class PostController {

	@CrossOrigin(origins = {"https://muce.apps.thedutchmc.nl", "http://localhost", "https://muce-code.github.io"})
	@RequestMapping(value = "logout", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String logoutUser(@RequestParam String userId, @RequestParam String sessionId) {
		Database db = App.getDatabase();
		
		JSONObject response = new JSONObject();
			
		if(!Utils.isPositiveLong(userId)) {
			response.put("success", false);
			return response.toString();
		}
		
		//Check if the supplied userId (fakeUserId) and sessionId belong to an existing session
		//If not, return with the appropriate response
		boolean sessionExists = db.sessionExists(sessionId, Long.valueOf(userId));
		if(!sessionExists) {
			response.put("sessionvalid", false);
			response.put("success", false);
			return response.toString();
		}
		
		//We now know that the session is valud
		response.put("sessionvalid", true);
		
		boolean removeSessionSuccess = db.removeSession(sessionId);
		try {
			Pair<Boolean, String> container = App.getDocker().getContainer(userId);
			
			if(container.getFirst()) {
				boolean dockerRemoveSuccess = App.getDocker().destroyContainer(container.getSecond());
				response.put("success", dockerRemoveSuccess && removeSessionSuccess);
			} else {
				response.put("success", false);
			}
		} catch(IOException e) {
			e.printStackTrace();
			response.put("success", false);
		}

		return response.toString();
	}
	
	/**
	 * Get the email address of a user
	 * @param userId The userId provided by the {@link #loginUser(String, String)} or {@link #registerUser(String, String)} endpoints.
	 * @param sessionId The session ID provided by the {@link #loginUser(String, String)} or {@link #registerUser(String, String)} endpoints.
	 * @return Returns a JSON formatted String
	 * 	<table border="1">
	 * 		<tr>
	 * 			<th> Key </th>
	 * 			<th> Type </th>
	 * 			<th> Description </th>
	 * 		</tr>
	 * 		<tr>
	 * 			<td> sessionvalid </td>
	 * 			<td> boolean </td>
	 * 			<td> True if the provided sessionId and userId belong to an existing session </td>
	 *		</tr>
	 *		<tr>
	 *			<td> success </td>
	 *			<td> boolean </td>
	 *			<td> True if the creating of the container succeeded </td>
	 *		</tr>
	 *		<tr>
	 *	</table>
	 */
	@CrossOrigin(origins = {"https://muce.apps.thedutchmc.nl", "http://localhost", "https://muce-code.github.io"})
	@RequestMapping(value = "user", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getUserEmail(@RequestParam String userId, @RequestParam String sessionId) {
		Database db = App.getDatabase();
		
		JSONObject response = new JSONObject();
		
		if(!Utils.isPositiveLong(userId)) {
			response.put("success", false);
			return response.toString();
		}
		
		//Check if the supplied userId (fakeUserId) and sessionId belong to an existing session
		//If not, return with the appropriate response
		boolean sessionExists = db.sessionExists(sessionId, Long.valueOf(userId));
		if(!sessionExists) {
			response.put("sessionvalid", false);
			response.put("success", false);
			return response.toString();
		}
		
		//We now know that the session is valud
		response.put("sessionvalid", true);
		
		//Get the email from the database
		//and return with the appropriate parameters
		JSONObject getEmailResponse = new JSONObject(db.getEmail(userId));
		if(getEmailResponse.has("success") && getEmailResponse.getBoolean("success")) {
			return getEmailResponse.toString();
		} else {
			response.put("success", false);
			return response.toString();
		}
	}
	
	/**
	 * Create a worskpace container for the provided user
	 * @param userId The userId provided by the {@link #loginUser(String, String)} or {@link #registerUser(String, String)} endpoints
	 * @param sessionId The sessionId provided by the {@link #loginUser(String, String)} or {@link #registerUser(String, String)} endpoints
	 * @return Returns a JSON formatted String
	 * 	<table border="1">
	 * 		<tr>
	 * 			<th> Key </th>
	 * 			<th> Type </th>
	 * 			<th> Description </th>
	 * 		</tr>
	 * 		<tr>
	 * 			<td> sessionvalid </td>
	 * 			<td> boolean </td>
	 * 			<td> True if the provided userId and sessionid belong to an existing session </td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td> success </td>
	 * 			<td> boolean </td>
	 * 			<td> True if the creation of the container succeeded, or if a container already exists for the user </td>
	 * 		</tr>
	 * 	</table>
	 */
	@CrossOrigin(origins = {"https://muce.apps.thedutchmc.nl", "http://localhost", "https://muce-code.github.io"})
	@RequestMapping(value = "container", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String createContainer(@RequestParam String userId, @RequestParam String sessionId) {
		Database db = App.getDatabase();
		
		JSONObject response = new JSONObject();

		if(!Utils.isPositiveLong(userId)) {
			response.put("success", false);
			return response.toString();
		}		
		//Check if the provided userId (fakeUserId) and sessionId belong to an existing session
		//If not, return with the appropriate response
		boolean sessionExists = db.sessionExists(sessionId, Long.valueOf(userId));
		if(!sessionExists) {
			response.put("sessionvalid", false);
			response.put("success", false);
			return response.toString();
		}
		
		//We now know that the session is valud
		response.put("sessionvalid", true);
		
		//Get the real userId from the database
		//If this does not succeed, return with the appropriate response
		JSONObject realUserResponse = new JSONObject(db.getRealUser(Long.valueOf(userId)));
		if(realUserResponse.has("success") && realUserResponse.getBoolean("success")) {
			
			//Check if a docker container already exists, if not create it
			//set the appropriate response
			try {
				Pair<Boolean, String> container = App.getDocker().getContainer(userId);
				
				response.put("a", container.getFirst());
				response.put("a", container.getSecond());
				
				if(!container.getFirst()) {
					boolean success = App.getDocker().createContainer(userId);
					response.put("success", success);
				} else {
					response.put("success", true);
				}
				
			} catch (IOException e) {
				App.logError(Utils.getStackTrace(e));
				response.put("success", false);
			}
		} else {
			response.put("success", false);
		}
		
		return response.toString();
	}
	
	/**
	 * Register a new user 
	 * @param email The email address of the new user, encoded in Base64
	 * @param password The password of the new user, encoded in Base64
	 * @return Returns a JSON formatted String
	 * 	<table border="1">
	 * 		<tr>
	 * 			<th> Key </th>
	 * 			<th> Type </th>
	 * 			<th> Description </th>
	 * 		</tr>
	 * 		<tr>
	 * 			<td> accountexists </td>
	 * 			<td> boolean </td>
	 * 			<td> True if the email address is already registered </td>
	 * 		</tr>
	 * 			<td> success </td>
	 * 			<td> boolean </td>
	 * 			<td> True if the account was successfully created </td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td> userid </td>
	 * 			<td> String </td>
	 * 			<td> The userId of the user that was registered. This parameter will not be given if success = false</td>
	 * 		</tr>
	 * 			<td> sessionid </td>
	 * 			<td> String </td>
	 * 			<td> The sessionId of the user that was registered. This parameter will not be given if success = false </td>
	 * 		</tr>
	 * 	</table>
	 */
	@CrossOrigin(origins = {"https://muce.apps.thedutchmc.nl", "http://localhost", "https://muce-code.github.io"})
	@RequestMapping(value = "register", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String registerUser(@RequestParam String email, @RequestParam String password) {
		Database db = App.getDatabase();
		
		//Decode the password and email address
		password = new String(Base64.decodeBase64(password), StandardCharsets.UTF_8);
		email = new String(Base64.decodeBase64(email), StandardCharsets.UTF_8);
		
		//Check with the database if an account is already registered with this email
		boolean accountExists = db.accountExists(email);
		
		JSONObject response = new JSONObject();

		//If an account is registered, return the appropriate response
		if(accountExists) {
			response.put("accountexists", true);
			response.put("success", false);
			return response.toString();
		}
		
		Password pswd = new Password();
		
		//Create salt and encode it in base64
		byte[] salt = pswd.salt();
		String salt64 = Base64.encodeBase64String(salt);
		
		//Hash the password with the salt and encode it in base64
		byte[] hash = pswd.hash(salt, password);
		String passwordHashed64 = Base64.encodeBase64String(hash);
		
		//Create a username from the email address
		// '@' -> '-'
		// '.' -> '-'
		String username = email.replace("@", "-").replace(".", "-");
		
		//Add the user to the database
		JSONObject registeredResponse = new JSONObject(db.register(email, passwordHashed64, username, salt64));
		
		//If adding the user succeeded, create a session and set the appropriate response
		if(registeredResponse.getBoolean("success")) {
			JSONObject sessionResponse = new JSONObject(db.createSession(registeredResponse.getLong("userid")));
			
			response.put("success", true);
			response.put("accountexists", false);
			response.put("userid", String.valueOf(sessionResponse.getLong("fakeuserid")));
			response.put("sessionid", sessionResponse.getString("sessionid"));
		} else {
			response.put("success", false);
		}
		
		return response.toString();
	}
	
	/**
	 * Login a user
	 * @param email The email address of the user encoded in Base64
	 * @param password The password of the user encoded in Base64
	 * @return Returns a JSON formatted String
	 * 	<table border="1">
	 * 		<tr>
	 * 			<th> Key </th>
	 * 			<th> Type </th>
	 * 			<th> Description </th>
	 * 		</tr>
	 * 		<tr>
	 * 			<td> login </td>
	 * 			<td> boolean </td>
	 * 			<td> True if the user was logged in successfully. This is false when the account does not exist, or the password is incorrect </td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td> accountexists </td>
	 * 			<td> boolean </td>
	 * 			<td> True if the account exists </td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td> sessionid </td>
	 * 			<td> String </td>
	 * 			<td> The sessionId for the user that logged in </td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td> userid </td>
	 * 			<td> String </td>
	 * 			<td> The userid for the user that logged in </td>
	 * 		</tr>
	 *	</table>
	 */
	@CrossOrigin(origins = {"https://muce.apps.thedutchmc.nl", "http://localhost", "https://muce-code.github.io"})
	@RequestMapping(value = "login", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String loginUser(@RequestParam String email, @RequestParam String password) {
		Database db = App.getDatabase();
		
		//Decode the password and email address from Base64
		password = new String(Base64.decodeBase64(password), StandardCharsets.UTF_8);
		email = new String(Base64.decodeBase64(email), StandardCharsets.UTF_8);
		
		//Check if the account exists
		boolean accountExists = db.accountExists(email);
		
		JSONObject response = new JSONObject();

		//If the account does not exist, return the appropriate response
		if(!accountExists) {
			response.put("login", false);
			response.put("accountexists", false);
			return response.toString();
		}
		
		//Get the salt from the database and decode it from Base64
		byte[] salt = Base64.decodeBase64(db.getSalt(email));
		
		//Hash the provided password with the salt and encode it in Base64
		byte[] hash = new Password().hash(salt, password);
		String passwordHashed64 = Base64.encodeBase64String(hash);
		
		//Consolidate the database for the sessionid, userid and if the password is correct
		JSONObject loginResponse = new JSONObject(db.login(email, passwordHashed64));
		
		//If the field 'valid' in the response is false, that means the provided password is incorrect
		//Return the appropriate response
		if(!loginResponse.getBoolean("valid")) {
			response.put("login", false);
			response.put("accountexists", true);
			return response.toString();
		}
		
		
		//If the response does not have the field 'sessionid', that means the user has no session, create one
		//if it does have the 'sessionid' field, then there is a session
		//Return the appropriate response
		if(!loginResponse.has("sessionid")) {
			//Create a session
			JSONObject sessionResponse = new JSONObject(db.createSession(loginResponse.getLong("userid")));
			
			response.put("sessionid", sessionResponse.getString("sessionid"));
			response.put("userid", String.valueOf(sessionResponse.getLong("fakeuserid")));
		} else {
			response.put("sessionid", loginResponse.getString("sessionid"));
			response.put("userid", String.valueOf(loginResponse.getLong("fakeuserid")));
		}
				
		//The login was successful and the account exists
		response.put("login", true);
		response.put("accountexists", true);
				
		return response.toString();
	}
}