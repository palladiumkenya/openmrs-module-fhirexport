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

import org.openmrs.module.fhirexport.export.ExportFhirObject;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.resource.ResourceFactory;

/**
 * invokes the generation of fhir objects
 */
public class ExportDataFragmentController {
	
	public void controller() {
		
	}
	
	public SimpleObject generateAndPostFhirObject(UiUtils ui, @SpringBean ResourceFactory resourceFactory) {
		
		String result = ExportFhirObject.generate();
		SimpleObject summary = new SimpleObject();
		summary.put("success", "true");
		summary.put("payload", result);
		return summary;
	}
	
}
