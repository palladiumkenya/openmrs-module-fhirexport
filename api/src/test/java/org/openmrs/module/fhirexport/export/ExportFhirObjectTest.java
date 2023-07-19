package org.openmrs.module.fhirexport.export;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import junit.framework.TestCase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

@RunWith(SpringJUnit4ClassRunner.class)
public class ExportFhirObjectTest extends BaseModuleContextSensitiveTest {
	
	// @Before
	public void setUp() throws Exception {
		//super.initializeInMemoryDatabase();
		// Add dummy patients
		//
	}
	
	@Test
	public void testExportFhirResources_ShouldPostTioFhirEndpointAndNotThrowException() {
		// ExportFhirObject exportFhirObject = new ExportFhirObject(Collections.singletonList(39857));
		ExportFhirObject exportFhirObject = new ExportFhirObject(Collections.singletonList(0));
		IBaseResource bundle = exportFhirObject.generate();
		exportFhirObject.sendBundle((Bundle) bundle);
		// ExportFhirObject.ExportExtractedFhirBundle(bundle);
		// Assert.notNull(null);
		
		//TODO: Add test data and appropriate Assert conditions
	}
}
