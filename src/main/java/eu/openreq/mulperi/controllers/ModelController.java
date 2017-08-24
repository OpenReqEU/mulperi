package eu.openreq.mulperi.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import eu.openreq.mulperi.models.kumbang.ParsedModel;
import eu.openreq.mulperi.models.mulson.Requirement;
import eu.openreq.mulperi.models.reqif.SpecObject;
import eu.openreq.mulperi.models.selections.FeatureSelection;
import eu.openreq.mulperi.repositories.ParsedModelRepository;
import eu.openreq.mulperi.services.CaasClient;
import eu.openreq.mulperi.services.FormatTransformerService;
import eu.openreq.mulperi.services.KumbangModelGenerator;
import eu.openreq.mulperi.services.ReqifParser;

import java.util.Collection;
import java.util.List;
import java.util.zip.DataFormatException;

import javax.management.IntrospectionException;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ResponseHeader;

@SpringBootApplication
@RestController
@RequestMapping("models")
public class ModelController {

	private FormatTransformerService transform = new FormatTransformerService();
	private KumbangModelGenerator kumbangModelGenerator = new KumbangModelGenerator();

	@Value("${mulperi.caasAddress}")
    private String caasAddress;
	
	@Autowired
	private ParsedModelRepository parsedModelRepository;
	
	@RequestMapping(value = "", method = RequestMethod.GET)
    public List<ParsedModel> modelList() {
        return parsedModelRepository.findAll();
    }
	
	/**
	 * Get single model as FeatureSelection for selecting features
	 * @param modelName
	 * @return
	 */
	@ApiOperation(value = "Get the structure of a model",
		    notes = "Get single model as FeatureSelection for selecting features",
		    response = FeatureSelection.class)
	@ApiResponses(value = { 
			@ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 500, message = "Failure")}) 
	@CrossOrigin
	@RequestMapping(value = "/{model}", method = RequestMethod.GET)
    public ResponseEntity<?> getModel(@PathVariable("model") String modelName) {
		
		ParsedModel model = this.parsedModelRepository.findFirstByModelName(modelName);
		
		if(model == null) {
			return new ResponseEntity<>("Model not found", HttpStatus.BAD_REQUEST);
		}
		
		return new ResponseEntity<>(transform.parsedModelToFeatureSelection(model), HttpStatus.OK);
    }
	
	@RequestMapping(value = "/mulson", method = RequestMethod.POST)
    public ResponseEntity<?> chocoMulson(@RequestBody List<Requirement> requirements) {
		
		String name = generateName(requirements);
		
        ParsedModel pm = transform.parseMulson(name, requirements);
		
		return sendModelToCaasAndSave(pm, caasAddress);
    }
	
	@RequestMapping(value = "/reqif", method = RequestMethod.POST, consumes="application/xml")
    public ResponseEntity<?> reqif(@RequestBody String reqifXML) {
		ReqifParser parser = new ReqifParser();
		String name = generateName(reqifXML);
		
		try {
			Collection<SpecObject> specObjects = parser.parse(reqifXML).values();
			ParsedModel pm = transform.parseReqif(name, specObjects);
	        return sendModelToCaasAndSave(pm, caasAddress);
		} catch (Exception e) { //ReqifParser error
			return new ResponseEntity<>("Syntax error in ReqIF\n\n" + e.getMessage(), HttpStatus.BAD_REQUEST);
		}
    }
	
	private String generateName(Object object) {
		int hashCode = object.hashCode();
		return "ID" + (hashCode > 0 ? hashCode : "_" + Math.abs(hashCode)); 
		//replace - with _, since Kumbang doesn't like hyphens
	}
	
	private ResponseEntity<String> sendModelToCaasAndSave(ParsedModel pm, String caasAddress) {
		
		pm.rolesForConstraints();
		
		String kumbangModel = kumbangModelGenerator.generateKumbangModelString(pm);
		CaasClient client = new CaasClient();
		
		String result = new String();
		
		try {
			result = client.uploadConfigurationModel(pm.getModelName(), kumbangModel, caasAddress);
		} catch(IntrospectionException e) {
            return new ResponseEntity<>("Impossible model\n\n" + e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch(DataFormatException e) {
            return new ResponseEntity<>("Syntax error\n\n" + e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch(Exception e) {
            return new ResponseEntity<>("Configuration model upload failed.\n\n" + e.toString(), HttpStatus.BAD_REQUEST);
		} 
		
		//Save model to database if send successful
		parsedModelRepository.save(pm);
		
		//return new ResponseEntity<>("Configuration model upload successful.\n\n - - - \n\n" + result, HttpStatus.CREATED);
		return new ResponseEntity<>(result, HttpStatus.CREATED);
	}

}