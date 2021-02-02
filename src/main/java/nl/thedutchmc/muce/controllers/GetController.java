package nl.thedutchmc.muce.controllers;

import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/muce")
public class GetController {

	@CrossOrigin(origins = {"https://muce.apps.thedutchmc.nl", "http://localhost", "https://muce-code.github.io"})
	@RequestMapping(value = "health", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getHealth() {
		JSONObject response = new JSONObject();
		response.put("health", "OK");
		return response.toString();
	}
}
