package org.openmrs.module.fhirexport.export;

import junit.framework.TestCase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.junit.After;
import org.junit.Before;
import org.openmrs.test.BaseModuleContextSensitiveTest;

public class ExportFhirObjectTest extends BaseModuleContextSensitiveTest {
	
	@Before
	public void setUp() throws Exception {
		super.initializeInMemoryDatabase();
		// Add dummy patients
		//
	}
	
	@After
	public void tearDown() throws Exception {
	}
	
	public void testGenerate() {
	
	}
	
	public void testExportFhirResources_ShouldPostTioFhirEndpointAndNotThrowException() {
		IBaseBundle bundle = ExportFhirObject.generate();
		ExportFhirObject.ExportExtractedFhirBundle(bundle);
	}
}
