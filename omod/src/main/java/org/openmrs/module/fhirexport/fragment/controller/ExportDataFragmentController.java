/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fhirexport.fragment.controller;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.fhirexport.export.ExportFhirObject;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.resource.ResourceFactory;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * invokes the generation of fhir objects
 */
public class ExportDataFragmentController {
	
	public void controller() {
		
	}
	
	public SimpleObject generateAndPostFhirObject(@RequestParam(value = "patientIds", required = false) String patientIds,
	        @RequestParam(value = "exportToFhirServer", required = false) Boolean exportToFhirServer,
	        @RequestParam(value = "fhirServerUrl", required = false) String fhirServerUrl,
	        @RequestParam(value = "exportToFileSystem", required = false) Boolean exportToFileSystem,
	        @RequestParam(value = "fileSystemPath", required = false) String fileSystemPath, UiUtils ui,
	        @SpringBean ResourceFactory resourceFactory) {
		
		SimpleObject summary = new SimpleObject();
		ExportFhirObject exportFhirObject = null;
		
		if (StringUtils.isNotBlank(patientIds)) {
			exportFhirObject = new ExportFhirObject(Arrays.asList(patientIds.split(",")));
		} else {
			exportFhirObject = new ExportFhirObject();
		}
		
		if (exportToFileSystem != null) {
			exportFhirObject.setExportToFileSystem(exportToFileSystem);
		}
		if (StringUtils.isNotBlank(fhirServerUrl)) {
			exportFhirObject.setExportUrl(fhirServerUrl);
		}
		if (exportToFhirServer != null) {
			exportFhirObject.setSendToFhirServer(exportToFhirServer);
		}
		if (StringUtils.isNotBlank(fileSystemPath)) {
			exportFhirObject.setExportDirectory(fileSystemPath);
		}
		
		try {
			exportFhirObject.generate();
			summary.put("success", "true");
			summary.put("payload", exportFhirObject.toString());
		}
		catch (IOException e) {
			summary.put("success", "false");
			summary.put("payload", e.getMessage());
		}
		
		return summary;
	}
	
}
