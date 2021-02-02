package nl.thedutchmc.muce;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.json.JSONObject;

import nl.thedutchmc.muce.types.Pair;

public class Docker {
	
	/**
	 * Create a Docker container for a user<br>
	 * You should first check {@link #getContainer(String)} for if a container already exists for this user.
	 * @param fakeUserId The fakeUserId of the user to create a container for
	 * @return Returns true if no output was written to stderr
	 * @throws IOException
	 */
	public boolean createContainer(String fakeUserId) throws IOException {
		
		File userDir = new File("/opt/muce/data/" + fakeUserId + "/config/data/User");
		if(!userDir.exists()) {
			userDir.mkdirs();
		}
		
		JSONObject configFileContent = new JSONObject();
		configFileContent.put("terminal.integrated.shell.linux", "/bin/bash");
		configFileContent.put("workbench.colorTheme", "Default Dark+");
		
		File configFile = new File(userDir, "settings.json");
		try {
		    BufferedWriter writer = new BufferedWriter(new FileWriter(configFile));
		    writer.write(configFileContent.toString());
		    writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		//The docker command required to start the container
		String dockerCommand = "docker run -d "
				+ "--name=muce-code-USERID "
				+ "-e PUID=1000 "
				+ "-e PGID=1000 "
				+ "-e TZ=Europe/London "
				+ "-v /opt/muce/data/USERID/config:/config "
				+ "-l \"traefik.enable=true\" "
				+ "-l \"traefik.http.routers.muce-code-USERID.rule=PathPrefix(\\`/workspace/USERID\\`)\" " //Path prefix of the container is /workspace/USERID/
				+ "-l \"traefik.http.routers.muce-code-USERID.entrypoints=web\" " //Using web entry point
				+ "-l \"traefik.http.middlewares.muce-code-USERID-replacepathregex.replacepathregex.regex=\\/workspace\\/(\\d*)\\/?\" " //replace /workspace/USERID/ with / in the final path
				+ "-l \"traefik.http.middlewares.muce-code-USERID-replacepathregex.replacepathregex.replacement=/\" "
				+ "-l \"traefik.http.routers.muce-code-USERID.middlewares=muce-code-USERID-replacepathregex@docker\" " //Enable the above specified replacement
				+ "-l \"traefik.http.services.muce-code-USERID.loadbalancer.server.port=8443\" " //Port is 8443
				+ "-l \"traefik.docker.network=muce-traefik\" "
				+ "--network=muce-traefik "
				+ "muce-code:latest";
		
		//Replace 'variables' in the command
		// USERID: 		The fakeUserId of the user
		dockerCommand = dockerCommand.replace("USERID", fakeUserId);
				
		System.out.println(dockerCommand);
		
		//Execute the command
		Runtime rt = Runtime.getRuntime();
		String[] commands = {"/bin/bash", "-c", dockerCommand};
		Process proc = rt.exec(commands);

		//Get the stdout from the command
		BufferedReader stdInput = new BufferedReader(new 
		     InputStreamReader(proc.getInputStream()));

		//Get the stderr from the command
		BufferedReader stdError = new BufferedReader(new 
		     InputStreamReader(proc.getErrorStream()));

		//Read stdout
		String buff;
		String stdout = "";
		while ((buff = stdInput.readLine()) != null) {
		    stdout += buff;
		}
		
		App.logInfo("Created container for " + fakeUserId + ": " + stdout);
		
		//Read stderr
		String stderr = "";
		while ((buff = stdError.readLine()) != null) {
		    stderr += buff;
		}
		
		//If stderr is not null, that means an error occured
		if(stderr != "") {
			App.logError(stderr);
		}
		
		//Returns true if stderr is null
		return stderr == "" || stderr.contains("WARNING");
	}

	/**
	 * Get if a container exists, and it's ID
	 * @param fakeUserId The fakeUserId used for the container
	 * @return Returns {@link Pair}. The Boolean indicates if the container exists, the String is the ID of the container. The ID is null if the container does not exist.
	 * @throws IOException
	 */
	public Pair<Boolean, String> getContainer(String fakeUserId) throws IOException {
		
		//The docker command to run to get the container, if there is any
		String dockerCommand = "docker ps "
				+ "-qf \"label=traefik.http.routers.muce-code-USERID.rule=PathPrefix(\\`/workspace/USERID\\`)\"";
		
		//Replace 'variables' in the command
		// USERID: fakeUserId
		dockerCommand = dockerCommand.replace("USERID", fakeUserId);
		
		System.out.println(dockerCommand);
		
		//Execute the command
		Runtime rt = Runtime.getRuntime();
		String[] commands = {"/bin/bash", "-c", dockerCommand};
		Process proc = rt.exec(commands);
		
		//Get stdout of the command
		BufferedReader stdInput = new BufferedReader(new 
			     InputStreamReader(proc.getInputStream()));

		//Get stderr of the command
		BufferedReader stdError = new BufferedReader(new 
			     InputStreamReader(proc.getErrorStream()));
		
		//Read stdout
		String buff;
		String stdout = "";
		while ((buff = stdInput.readLine()) != null) {
		    stdout += buff;
		}
				
		//Read stderr
		String stderr = "";
		while ((buff = stdError.readLine()) != null) {
		    stderr += buff;
		}
		
		//If stderr is not null, an error occured 
		if(stderr != "") {
			App.logError(stderr);
		}
		
		return new Pair<Boolean, String>(stdout != "", stdout);
	}

	public boolean destroyContainer(String containerId) throws IOException {
		String dockerCommand = "docker rm -f CONTAINERID";
		dockerCommand = dockerCommand.replace("CONTAINERID", containerId);
		
		//Execute the command
		Runtime rt = Runtime.getRuntime();
		String[] commands = {"/bin/bash", "-c", dockerCommand};
		Process proc = rt.exec(commands);

		//Get the stdout from the command
		BufferedReader stdInput = new BufferedReader(new 
		     InputStreamReader(proc.getInputStream()));

		//Get the stderr from the command
		BufferedReader stdError = new BufferedReader(new 
		     InputStreamReader(proc.getErrorStream()));

		//Read stdout
		String buff;
		String stdout = "";
		while ((buff = stdInput.readLine()) != null) {
		    stdout += buff;
		}
		
		App.logInfo("Destroyed container: " + containerId);
		
		//Read stderr
		String stderr = "";
		while ((buff = stdError.readLine()) != null) {
		    stderr += buff;
		}
		
		App.logError(stderr);
		
		return stdout.contains(containerId);
	}
}
