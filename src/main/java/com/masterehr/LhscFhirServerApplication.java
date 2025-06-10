package com.masterehr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan // This annotation tells Spring to scan for and register Servlets like our FHIR server
public class LhscFhirServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(LhscFhirServerApplication.class, args);
	}

}
