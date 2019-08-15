package eu.openreq.mulperi.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import eu.openreq.mulperi.models.json.Dependency;
import eu.openreq.mulperi.models.json.Project;
import eu.openreq.mulperi.models.json.Release;
import eu.openreq.mulperi.models.json.Requirement;
import eu.openreq.mulperi.models.json.Requirement_type;
import eu.openreq.mulperi.services.InputChecker;
import eu.openreq.mulperi.services.OpenReqJSONParser;
import eu.openreq.mulperi.services.MurmeliModelGenerator;
import eu.openreq.mulperi.services.OpenReqConverter;
import fi.helsinki.ese.murmeli.ElementModel;
import fi.helsinki.ese.murmeli.TransitiveClosure;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import springfox.documentation.annotations.ApiIgnore;

@SpringBootApplication
@RestController
@RequestMapping("models")
public class MulperiController {
	
	private Gson gson = new Gson();

	@Value("${mulperi.caasAddress}")
	private String caasAddress; 
	
	@Autowired
	RestTemplate rt;
	
	/**
	 * Import a model in JSON format
	 * @param requirements
	 * @return
	 * @throws JSONException 
	 * @throws ReleasePlanException 
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 */
	@ApiOperation(value = "Import OpenReq JSON model to Caas",
			notes = "Import a model to Caas in JSON format",
			response = String.class)
	@ApiResponses(value = { 
			@ApiResponse(code = 201, message = "Success, returns received requirements and dependencies "
					+ "in OpenReq JSON format"),
			@ApiResponse(code = 400, message = "Failure, ex. malformed input"),
			@ApiResponse(code = 409, message = "Failure")}) 
	@PostMapping(value = "murmeliModelToKeljuCaaS")
	public ResponseEntity<String> murmeliModelToKeljuCaaS(@RequestBody String requirements) throws JSONException, 
				IOException, ParserConfigurationException {
		OpenReqJSONParser parser = new OpenReqJSONParser(requirements);
		
		MurmeliModelGenerator generator = new MurmeliModelGenerator();
		ElementModel model;
		String projectId = null;
		if(parser.getProjects()!=null) {
			projectId = parser.getProjects().get(0).getId();
			model = generator.initializeElementModel(parser.getRequirements(), parser.getDependencies(), projectId);
		}
		else{
			model = generator.initializeElementModel(parser.getRequirements(), parser.getDependencies(), 
					parser.getRequirements().get(0).getId());
		}
		
		try {
			Date date = new Date();
			System.out.println("Sending " + projectId + " to KeljuCaas at " + date.toString());
			
			return this.sendModelToKeljuCaas(OpenReqJSONParser.parseToJson(model));
		}
		catch (Exception e) {
			return new ResponseEntity<String>("Cannot send the model to KeljuCaas", HttpStatus.EXPECTATION_FAILED); //change to something else?
		}
	}
	
	/**
	 * Update a model in JSON format
	 * @param requirements
	 * @return
	 * @throws JSONException 
	 * @throws ReleasePlanException 
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 */
	@ApiOperation(value = "Update OpenReq JSON model in Caas",
			notes = "Import the updated requirements to Caas as a project in JSON format",
			response = String.class)
	@ApiResponses(value = { 
			@ApiResponse(code = 201, message = "Success, returns received requirements and dependencies in OpenReq JSON format"),
			@ApiResponse(code = 400, message = "Failure, ex. malformed input"),
			@ApiResponse(code = 409, message = "Failure")}) 
	@PostMapping(value = "updateMurmeliModelInKeljuCaas")
	public ResponseEntity<?> updateMurmeliModelInKeljuCaas(@RequestBody String requirements) throws JSONException, IOException, ParserConfigurationException {
		
		OpenReqJSONParser parser = new OpenReqJSONParser(requirements);
		
		MurmeliModelGenerator generator = new MurmeliModelGenerator();
		ElementModel model;
		String projectId = null;
		if (parser.getProjects() != null) {
			projectId = parser.getProjects().get(0).getId();
			model = generator.initializeElementModel(parser.getRequirements(), parser.getDependencies(), projectId);
		} else {
			model = generator.initializeElementModel(parser.getRequirements(), parser.getDependencies(), parser.getRequirements().get(0).getId());
		}
		
		try {
			Date date = new Date();
			System.out.println("Updating " + projectId + " in KeljuCaas at " + date.toString());
			
			return this.updateModelInKeljuCaas(OpenReqJSONParser.parseToJson(model));
		} catch (Exception e) {
			return new ResponseEntity<>("Cannot send the model to KeljuCaas", HttpStatus.EXPECTATION_FAILED); //change to something else?
		}
	}
	
	private ResponseEntity<?> updateModelInKeljuCaas(String jsonString) {
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_XML);

		HttpEntity<String> entity = new HttpEntity<String>(jsonString, headers);

		try {
			return rt.postForEntity(caasAddress + "/updateModel", entity, String.class);
		} catch (HttpClientErrorException e) {
			return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}
	
	/**
	 * Checks whether a release plan is consistent and provides diagnosis if not
	 * 
	 * @param jsonString
	 * @param analysisOnly
	 * @param timeOut
	 * @return JSON response
	 * 		{ 
	 * 			"response": {
	 * 				"consistent": false, 
	 * 				"diagnosis": [
	 * 					[
	 * 						{
	 * 							"requirement": (requirementID)
	 * 						}
	 * 					]
	 * 				]
	 * 			}
	 * 		}
	 * @throws JSONException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	@ApiOperation(value = "Is release plan consistent and do diagnosis v2",
			notes = "Check whether a release plan is consistent. Provide diagnosis if it is not consistent.",
			response = String.class)
	@ApiResponses(value = { 
			@ApiResponse(code = 200, message = "Success, returns JSON {\"response\": {\"consistent\": true}}"),
			@ApiResponse(code = 400, message = "Failure, ex. model not found"), 
			@ApiResponse(code = 409, message = "Diagnosis of inconsistency returns JSON {\"response\": {\"consistent\": false, \"diagnosis\": [[{\"requirement\": (requirementID)}]]}}")}) 
	@PostMapping(value = "/projects/consistencyCheckAndDiagnosis")
	public ResponseEntity<?> consistencyCheckAndDiagnosis(@RequestBody String jsonString,
			@RequestParam(required = false) boolean analysisOnly,
			@RequestParam(required = false, defaultValue = "0") int timeOut) 
					throws JSONException, IOException, ParserConfigurationException {
		
		String completeAddress = caasAddress + "/consistencyCheckAndDiagnosis?analysisOnly=" + analysisOnly;

		return convertToMurmeliAndPostToCaas(jsonString, completeAddress, false, timeOut);	
	}
	
	/**
	 * Converts the given OpenReq JSON to Murmeli along with various checks, then sends it to Keljucaas
	 * 
	 * @param jsonString
	 * @param completeAddress
	 * @param duplicatesInResponse
	 * @param timeOut
	 * @return
	 * @throws JSONException
	 */
	public ResponseEntity<String> convertToMurmeliAndPostToCaas(String jsonString, String completeAddress, 
			boolean duplicatesInResponse, int timeOut) throws JSONException {

		OpenReqJSONParser parser = new OpenReqJSONParser(jsonString);
		
		List<Requirement> requirements = parser.getRequirements();
		List<Dependency> dependencies = parser.getDependencies();
		
		for (Requirement req : requirements) {
			if (req.getRequirement_type() == null) {
				req.setRequirement_type(Requirement_type.REQUIREMENT);
			}
		} 
		
		Project project = null;
		String id = null;
		 
		if (requirements.size()>0) {
			id = requirements.get(0).getName();
		}
		if (parser.getProject() != null) {
			project = parser.getProject();
			id = project.getId();
		}
		
		List<Release> releases = new ArrayList<Release>();
		if (parser.getReleases() != null) {
			releases = parser.getReleases();
		}
		
		
		//Input checker
		//---------------------------------------------------------------
				
		InputChecker checker = new InputChecker();
		String result = checker.checkInput(project, requirements,  dependencies, releases);
		
		if (!result.equals("OK")) {
			return new ResponseEntity<>("Mulperi error: " + result, HttpStatus.BAD_REQUEST); 
		}
			
		//---------------------------------------------------------------
	
		//Combine requirements with dependency "duplicates"
		//---------------------------------------------------------------
		
		JsonArray changes = null;
		
		try {
			changes = parser.combineDuplicates();
		} catch (JSONException e) {
			return new ResponseEntity<>("JSON error", HttpStatus.BAD_REQUEST);
		}
		requirements = parser.getFilteredRequirements();
		dependencies = parser.getFilteredDependencies();
		releases = parser.getFilteredReleases();
		
		//---------------------------------------------------------------
		
		
		MurmeliModelGenerator generator = new MurmeliModelGenerator();
		ElementModel model = generator.initializeElementModel(requirements, new ArrayList<String>(), dependencies, releases, id);
		
		Gson gson = new Gson();
		String murmeli = gson.toJson(model);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		
		HttpEntity<String> entity = new HttpEntity<String>(murmeli, headers);
		ResponseEntity<?> response = null;
		
		RestTemplate tempRt = null;
		
//		if (timeOut!=0) {
//			tempRt = rt;
//			tempRt.setRequestFactory(new SimpleClientHttpRequestFactory());
//			SimpleClientHttpRequestFactory rf = (SimpleClientHttpRequestFactory) tempRt
//	                .getRequestFactory();
//	        rf.setReadTimeout(timeOut);
//		} else {
			tempRt = rt;
		//}
		try {
			response = tempRt.postForEntity(completeAddress, entity, String.class);
		} catch (ResourceAccessException e) {
			return new ResponseEntity<>("Request timed out", HttpStatus.REQUEST_TIMEOUT);
		} catch (HttpClientErrorException e) {
			return new ResponseEntity<>("Error:\n\n" + e.getResponseBodyAsString(), e.getStatusCode());
		}
		if (duplicatesInResponse) {
			JsonObject responseObject = new Gson().fromJson(response.getBody().toString(), JsonObject.class);
			responseObject.add("duplicates", changes);
			return new ResponseEntity<String>(responseObject.toString(), response.getStatusCode());
		}
		return new ResponseEntity<String>(response.getBody() + "", response.getStatusCode());
	}
	
	@ApiIgnore
	@PostMapping(value = "/sendModelToKeljuCaas")
	public ResponseEntity<String> sendModelToKeljuCaas(String jsonString) {

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_XML);

		HttpEntity<String> entity = new HttpEntity<String>(jsonString, headers);

		try {
			return rt.postForEntity(caasAddress + "/importModelAndUpdateGraph", entity, String.class);
		} catch (HttpClientErrorException e) {
			return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}
	
	
	@ApiOperation(value = "Get the transitive closure of a requirement",
			notes = "Returns the transitive closure of a given requirement up to the depth of 5",
			response = String.class)
	@ApiResponses(value = { 
			@ApiResponse(code = 200, message = "Success, returns JSON model"),
			@ApiResponse(code = 400, message = "Failure, ex. model not found"), 
			@ApiResponse(code = 409, message = "Conflict")}) 
	@PostMapping(value = "/findTransitiveClosureOfRequirement")
	public ResponseEntity<?> findTransitiveClosureOfRequirement(@RequestBody List<String> requirementId, 
			@RequestParam(required = false) Integer layerCount) throws JSONException, IOException, ParserConfigurationException {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		String completeAddress = caasAddress + "/findTransitiveClosureOfElement";
		
		if (layerCount!=null) {
			completeAddress += "?layerCount=" + layerCount;
		}
		
		String response = null;
		
		try {
			Set<Requirement> requirements = new HashSet<Requirement>();
			Set<Dependency> dependencies = new HashSet<Dependency>();
			Map<Integer, List<String>> layers = new HashMap<Integer, List<String>>();
			for (String reqId : requirementId) {
				response = rt.postForObject(completeAddress, reqId, String.class);
				TransitiveClosure closure = gson.fromJson(response, TransitiveClosure.class);
				OpenReqConverter converter = new OpenReqConverter(closure.getModel());
				List<Requirement> convertedRequirements = converter.getRequirements();
				
				//Add Unknown requirement part if empty (hack?)
				convertedRequirements = converter.addUnknownIfEmpty(convertedRequirements);
				
				requirements.addAll(convertedRequirements);
				
				dependencies.addAll(converter.getDependencies());
				
				Map<Integer, List<String>> closureLayers = closure.getLayers();
				for (Integer i : closureLayers.keySet()) {
					if (layers.containsKey(i)) {
						List<String> combinedLayers = layers.get(i);
						combinedLayers.addAll(closureLayers.get(i));
						layers.put(i, combinedLayers);
					} else {
						layers.put(i, closureLayers.get(i));
					}
				}
			}

			String requirementsAsString = gson.toJson(requirements);
			String dependenciesAsString = gson.toJson(dependencies);
			String layersAsString = gson.toJson(layers);
			
			String json = "{\"requirements\":" + requirementsAsString + ",\"dependencies\":" + dependenciesAsString + ",\"layers\":" + layersAsString + "}";
			return new ResponseEntity<>(json, HttpStatus.OK);
		} catch (HttpClientErrorException e) {
			return new ResponseEntity<>("Error:\n\n" + e.getResponseBodyAsString(), e.getStatusCode());
		}
		catch (Exception e) {
			return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
		}

	}

	@ApiOperation(value = "Get the transitive closure of a requirement, then check for consistency",
			notes = "Solves whether the transitive closure of the requirement is consistent",
			response = String.class)
	@ApiResponses(value = { 
			@ApiResponse(code = 200, message = "Success, returns JSON model"),
			@ApiResponse(code = 400, message = "Failure, ex. model not found"), 
			@ApiResponse(code = 409, message = "Conflict")}) 
	@PostMapping(value = "/consistencyCheckForTransitiveClosure")
	public ResponseEntity<?> consistencyCheckForTransitiveClosure(@RequestBody List<String> requirementId, 
			@RequestParam(required = false) Integer layerCount, 
			@RequestParam(required = false) boolean analysisOnly,  
			@RequestParam(required = false, defaultValue = "0") int timeOut) 
					throws JSONException, IOException, ParserConfigurationException {
		ResponseEntity<?> transitiveClosure = findTransitiveClosureOfRequirement(requirementId, layerCount);
		String completeAddress = caasAddress + "/consistencyCheckAndDiagnosis?analysisOnly=" + analysisOnly;
		return convertToMurmeliAndPostToCaas(transitiveClosure.getBody().toString(), completeAddress, true, timeOut);	
	}

	/**
	 * Check whether a release plan is consistent
	 * 
	 * @param jsonString
	 * @return JSON response
	 * 		{ 
	 * 			"response": {
	 * 				"consistent": false
	 * 			}
	 * 		}
	 * @throws JSONException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	/*
	 * HIDDEN FROM SWAGGER!
	 * @ApiOperation(value = "Is release plan consistent",
			notes = "Send model to Caas to check whether a release plan is consistent.",
			response = String.class)
	@ApiResponses(value = { 
			@ApiResponse(code = 200, message = "Success, returns JSON {\"response\": {\"consistent\": true}}"),
			@ApiResponse(code = 400, message = "Failure, ex. model not found"), 
			@ApiResponse(code = 409, message = "Check of inconsistency returns JSON {\"response\": {\"consistent\": false}}")}) 
	@PostMapping(value = "/projects/uploadDataAndCheckForConsistency")*/
	public ResponseEntity<String> uploadDataAndCheckForConsistency(@RequestBody String jsonString) throws JSONException, IOException, ParserConfigurationException {
		String completeAddress = caasAddress + "/uploadDataAndCheckForConsistency";	
		return convertToMurmeliAndPostToCaas(jsonString, completeAddress, false, 30000);		
	}
	
	/**
	 * Checks whether a release plan is consistent and provides diagnosis if not
	 * 
	 * @param jsonString
	 * @return JSON response
	 * 		{ 
	 * 			"response": {
	 * 				"consistent": false, 
	 * 				"diagnosis": [
	 * 					[
	 * 						{
	 * 							"requirement": (requirementID)
	 * 						}
	 * 					]
	 * 				]
	 * 			}
	 * 		}
	 * @throws JSONException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	/*HIDDEN FROM SWAGGER. PERHAPS OBSOLETE.
	 * 
	 * @ApiOperation(value = "Is release plan consistent and do diagnosis",
			notes = "Check whether a release plan is consistent. Provide diagnosis if it is not consistent.",
			response = String.class)
	@ApiResponses(value = { 
			@ApiResponse(code = 200, message = "Success, returns JSON {\"response\": {\"consistent\": true}}"),
			@ApiResponse(code = 400, message = "Failure, ex. model not found"), 
			@ApiResponse(code = 409, message = "Diagnosis of inconsistency returns JSON {\"response\": {\"consistent\": false, \"diagnosis\": [[{\"requirement\": (requirementID)}]]}}")}) 
	@PostMapping(value = "/projects/uploadDataCheckForConsistencyAndDoDiagnosis")*/
	public ResponseEntity<?> uploadDataCheckForConsistencyAndDoDiagnosis(@RequestBody String jsonString) throws JSONException, IOException, ParserConfigurationException {
		String completeAddress = caasAddress + "/uploadDataCheckForConsistencyAndDoDiagnosis";	
		return convertToMurmeliAndPostToCaas(jsonString, completeAddress, false, 30000);
	}
}
