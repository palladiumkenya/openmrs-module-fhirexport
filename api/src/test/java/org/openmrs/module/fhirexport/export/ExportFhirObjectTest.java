package org.openmrs.module.fhirexport.export;

import java.io.IOException;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class ExportFhirObjectTest extends BaseModuleContextSensitiveTest {
	
	@Before
	public void setUp() throws Exception {
		this.setAutoIncrementOnTablesWithNativeIfNotAssignedIdentityGenerator();
		this.initializeInMemoryDatabase();
		this.executeDataSet("pid_1458.xml");
		// Add dummy patients
		//
	}
	
	@Test
	public void testExportFhirResources_ShouldPostTioFhirEndpointAndNotThrowException() {
		ExportFhirObject exportFhirObject = new ExportFhirObject(Collections.singletonList("1458"));
		try {
			exportFhirObject.setSendToFhirServer(true);
			exportFhirObject.setExportToFileSystem(true);
			exportFhirObject.setExportDirectory(null);
			exportFhirObject.generate();
		}
		catch (IOException e) {
			Assert.fail(e.getMessage());
			throw new RuntimeException(e);
		}
		// exportFhirObject.sendBundle((Bundle) bundle);
		// ExportFhirObject.ExportExtractedFhirBundle(bundle);
		// Assert.notNull(null);
		
		//TODO: Add test data and appropriate Assert conditions
	}
}
