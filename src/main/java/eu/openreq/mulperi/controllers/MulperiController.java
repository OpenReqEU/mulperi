package eu.openreq.mulperi.controllers;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import eu.openreq.mulperi.services.KeljuService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@SpringBootApplication
@RestController
@RequestMapping("models")
public class MulperiController {

	@Autowired
	RestTemplate rt;
	
	@Autowired
	KeljuService keljuService;
	
	/**
	 * Import a model in JSON format
	 * @param requirements
	 * @return
	 * @throws JSONException 
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
	@PostMapping(value = "murmeliModelToKeljuCaas")
	public ResponseEntity<String> murmeliModelToKeljuCaaS(@RequestBody String requirements)
			throws JSONException, IOException, ParserConfigurationException {
		return keljuService.murmeliModelToKeljuCaaS(requirements);
	}
	
	/**
	 * Update a model in JSON format
	 * @param requirements
	 * @return
	 * @throws JSONException 
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
	public ResponseEntity<String> updateMurmeliModelInKeljuCaas(@RequestBody String requirements) throws JSONException, IOException, ParserConfigurationException {
		return keljuService.updateMurmeliModelInKeljuCaas(requirements);
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
	public ResponseEntity<String> consistencyCheckAndDiagnosis(@RequestBody String jsonString, 
			@ApiParam(name = "analysisOnly", value = "If true, only analysis of consistency is performed and diagnoses are omitted. If false, Diagnosis is performed in case of inconsistency.")
			@RequestParam(required = false) boolean analysisOnly,
			@ApiParam(name = "timeOut", value = "Time in milliseconds allowed for each diagnosis. If the timeOut is exceeded, diagnosis fails and output will include 'Timeout' and 'Timeout_msg' fields. If 0 (default), there is no timeout for diagnoses.")
			@RequestParam(required = false, defaultValue = "0") int timeOut,
			@ApiParam(name = "omitCrossProject", value = "If 'true' and 'description' field of a relationship includes 'crossProjectTrue', the relationship is not taken into account in analysis. Adds 'RelationshipsIgnored' and 'RelationshipsIgnored_msg' fields to output.")
			@RequestParam(required = false) boolean omitCrossProject,
			@ApiParam(name = "omitReqRelDiag", value = "If true, the third diagnosis (both requirements and relationships) is omitted.")
			@RequestParam(required = false) boolean omitReqRelDiag) throws JSONException, IOException, ParserConfigurationException {
		return keljuService.consistencyCheckAndDiagnosis(jsonString, analysisOnly, timeOut, omitCrossProject, omitReqRelDiag);
	}	
	
	@ApiOperation(value = "Get the transitive closure of a requirement",
			notes = "Returns the transitive closure of a given requirement up to the depth of 5",
			response = String.class)
	@ApiResponses(value = { 
			@ApiResponse(code = 200, message = "Success, returns JSON model"),
			@ApiResponse(code = 400, message = "Failure, ex. model not found"), 
			@ApiResponse(code = 409, message = "Conflict")}) 
	@PostMapping(value = "/findTransitiveClosureOfRequirement")
	public ResponseEntity<String> findTransitiveClosureOfRequirement(@RequestBody List<String> requirementId, 
			@RequestParam(required = false) Integer layerCount) throws
			JSONException, IOException, ParserConfigurationException {

		return keljuService.findTransitiveClosureOfRequirement(requirementId, layerCount);

	}

	@ApiOperation(value = "Get the transitive closure of a requirement, then check for consistency",
			notes = "Solves whether the transitive closure of the requirement is consistent",
			response = String.class)
	@ApiResponses(value = { 
			@ApiResponse(code = 200, message = "Success, returns JSON model"),
			@ApiResponse(code = 400, message = "Failure, ex. model not found"), 
			@ApiResponse(code = 409, message = "Conflict")}) 
	@PostMapping(value = "/consistencyCheckForTransitiveClosure")
	public ResponseEntity<String> consistencyCheckForTransitiveClosure(@RequestBody List<String> requirementId, 
			@RequestParam(required = false) Integer layerCount, 
			@ApiParam(name = "analysisOnly", value = "If true, only analysis of consistency is performed and diagnoses are omitted. If false, Diagnosis is performed in case of inconsistency.")
			@RequestParam(required = false) boolean analysisOnly,
			@ApiParam(name = "timeOut", value = "Time in milliseconds allowed for each diagnosis. If the timeOut is exceeded, diagnosis fails and output will include 'Timeout' and 'Timeout_msg' fields. If 0 (default), there is no timeout for diagnoses.")
			@RequestParam(required = false, defaultValue = "0") int timeOut,
			@ApiParam(name = "omitCrossProject", value = "If 'true' and 'description' field of a relationship includes 'crossProjectTrue', the relationship is not taken into account in analysis. Adds 'RelationshipsIgnored' and 'RelationshipsIgnored_msg' fields to output.")
			@RequestParam(required = false) boolean omitCrossProject,
			@ApiParam(name = "omitReqRelDiag", value = "If true, the third diagnosis (both requirements and relationships) is omitted.")
			@RequestParam(required = false) boolean omitReqRelDiag) throws JSONException, IOException, ParserConfigurationException {
		return keljuService.consistencyCheckForTransitiveClosure(requirementId, layerCount, analysisOnly, timeOut, omitCrossProject, omitReqRelDiag);
	}

}
