package com.mulperi.controller;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.mulperi.models.submit.Requirement;
import com.mulperi.services.CaasClient;
import com.mulperi.services.FormatTransformerService;

import java.util.List;

@SpringBootApplication
@RestController
@RequestMapping("submit")
public class SubmitController {

	private FormatTransformerService transform = new FormatTransformerService();
	
	@RequestMapping(value = "/simple", method = RequestMethod.POST)
    public String simpleIn(@RequestBody List<Requirement> requirements) {
		
		String name = "ID" + requirements.hashCode();
		
        String kumbangModel = transform.SimpleToKumbang(name, requirements);
        
        CaasClient client = new CaasClient();
		
		String address = "http://localhost:8080/kumbang.configurator.server/Kumb";
		
		try {
			client.uploadConfigurationModel(name, kumbangModel, address);
			
		}	catch(Exception e) {
            return "Couldn't upload the configuration model";            
		} 
        return kumbangModel;
    }

}