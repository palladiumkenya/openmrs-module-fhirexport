package org.openmrs.module.fhirexport.export;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.providers.r4.ObservationFhirResourceProvider;
import org.openmrs.module.fhir2.providers.r4.PatientFhirResourceProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holder for code that generates FHIR objects
 */
public class ExportFhirObject {
	
	public static final String OPENMRS_ID_TYPE_UUID = "dfacd928-0370-4315-99d7-6ec1c9f7ae76";
	
	public static void generate() {
		
		// Define all forms of interest
		EncounterService encounterService = Context.getEncounterService();
		PatientService patientService = Context.getPatientService();
		// hiv follow-up visit - interest is only on the visit date and appointment date given
		String hivFollowupTCAConceptUuid = "5096AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"; // next appointment date concept
		String hivFollowupEncounterTypeUuid = "a0034eee-1940-4e35-847f-97537a35d05e";
		String hivVisitGreenCardFormUuid = "22c68f86-bbf0-49ba-b2d1-23fa7ccf0259";
		String hivVisitSummaryFormUuid = "23b4ebbd-29ad-455e-be0e-04aa6bc30798";
		EncounterType followUpEncType = encounterService.getEncounterTypeByUuid(hivFollowupEncounterTypeUuid);
		Form hivVisitSummaryForm = Context.getFormService().getFormByUuid(hivVisitSummaryFormUuid);
		Form hivVisitPoCForm = Context.getFormService().getFormByUuid(hivVisitGreenCardFormUuid);
		
		// hiv enrolment visit - interest is on the entry point, and enrolment date
		String hivEnrolmentFormUuid = "e4b506c1-7379-42b6-a374-284469cba8da";
		String hivEnrolmentEncTypeUuid = "de78a6be-bfc5-4634-adc3-5f1a280455cc";
		String hivEnrolmentEntryPointConceptUuid = "160540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"; // entry point concept
		EncounterType hivEnrolmentEncType = encounterService.getEncounterTypeByUuid(hivEnrolmentEncTypeUuid);
		Form hivEnrolmentForm = Context.getFormService().getFormByUuid(hivEnrolmentFormUuid);
		
		// hiv program discontinuation - gets effective date, discontinuation date, and discontinuation reason
		String hivProgramDiscontinuationFormUuid = "e3237ede-fa70-451f-9e6c-0908bc39f8b9";
		String hivDiscontinuationEncTypeUuid = "2bdada65-4c72-4a48-8730-859890e25cee";
		String hivDiscontinuationReason = "161555AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		EncounterType discEncType = encounterService.getEncounterTypeByUuid(hivDiscontinuationEncTypeUuid);
		Form hivProgramDiscontinuationForm = Context.getFormService().getFormByUuid(hivProgramDiscontinuationFormUuid);
		
		Map<String, IBaseBundle> obsCache = new HashMap<String, IBaseBundle>();
		PatientFhirResourceProvider patientResourceProvider = Context.getRegisteredComponent(
		    "patientFhirR4ResourceProvider", PatientFhirResourceProvider.class);
		ObservationFhirResourceProvider obsResourceProvider = Context.getRegisteredComponent(
		    "observationFhirR4ResourceProvider", ObservationFhirResourceProvider.class);
		
		List<Integer> controlPatientIds = Arrays.asList(39857);
		//for (Patient patient : Context.getPatientService().getAllPatients(false)) {
		for (Integer patientId : controlPatientIds) {
			
			// generate patient objects
			IBaseBundle bundle = new Bundle();
			Patient patient = Context.getPatientService().getPatient(patientId);
			PatientIdentifierType openmrsIdType = patientService.getPatientIdentifierTypeByUuid(OPENMRS_ID_TYPE_UUID);
			PatientIdentifier openmrsId = patient.getPatientIdentifier(openmrsIdType); // all patients have openmrs id thus the preference
			
			ReferenceAndListParam patientReference = new ReferenceAndListParam();
			ReferenceParam patientParam = new ReferenceParam();
			patientParam.setValue(patient.getUuid());
			patientReference.addValue(new ReferenceOrListParam().add(patientParam));
			
			addPatientObjectToBundle(openmrsId.getIdentifier(), patientResourceProvider, (Bundle) bundle);
			
			// generate observation objects
			//1. get all hiv enrolment encounters
			//2. get all hiv follow up encounters
			//3. get all hiv program discontinuation encounters
			List<Encounter> followupEncounters = allEncounters(patient, followUpEncType,
			    Arrays.asList(hivVisitSummaryForm, hivVisitPoCForm));
			
			List<Encounter> hivEnrolmentEncounters = allEncounters(patient, hivEnrolmentEncType,
			    Arrays.asList(hivEnrolmentForm));
			
			List<Encounter> hivDiscontinuationEncounters = allEncounters(patient, discEncType,
			    Arrays.asList(hivProgramDiscontinuationForm));
			
			if (!hivEnrolmentEncounters.isEmpty()) {
				System.out.println("HIV enrolment encounters: " + hivEnrolmentEncounters.size());
				addEncounterObsToBundle(hivEnrolmentEntryPointConceptUuid, obsResourceProvider, patientReference,
				    hivEnrolmentEncounters, (Bundle) bundle);
			}
			
			if (!followupEncounters.isEmpty()) {
				System.out.println("HIV followup encounters: " + followupEncounters.size());
				addEncounterObsToBundle(hivFollowupTCAConceptUuid, obsResourceProvider, patientReference,
				    followupEncounters, (Bundle) bundle);
			}
			
			if (!hivDiscontinuationEncounters.isEmpty()) {
				System.out.println("HIV program discontinuation encounters: " + hivDiscontinuationEncounters.size());
				addEncounterObsToBundle(hivDiscontinuationReason, obsResourceProvider, patientReference,
				    hivDiscontinuationEncounters, (Bundle) bundle);
			}
			
			IGenericClient client = FhirContext.forCached(FhirVersionEnum.R4).newRestfulGenericClient(
			    "http://localhost:8080/fhir");
			BasicAuthInterceptor authInterceptor = new BasicAuthInterceptor("", "");
			client.registerInterceptor(authInterceptor);
			try {
				System.out.println(FhirContext.forCached(FhirVersionEnum.R4).newJsonParser().setPrettyPrint(true)
				        .encodeResourceToString(bundle));
				
				// client.transaction().withBundle(bundle).execute();
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
		/**
		 * Flow: Get all patients, Loop through all patients: generate patient resource generate
		 * observation resource(s) for required data add to bundle send to fhir server Questions: 1.
		 * Do we need all encounters, or just some? i.e. last, first, etc 2. Should we Adopted:
		 * Generate all patient resources and send Generate all observations and send
		 */
		
	}
	
	private static void addEncounterObsToBundle(String variableConceptUuid,
	        ObservationFhirResourceProvider obsResourceProvider, ReferenceAndListParam patientReference,
	        List<Encounter> encounters, Bundle bundle) {
		for (Encounter encounter : encounters) {
			
			ReferenceAndListParam encounterReference = new ReferenceAndListParam();
			ReferenceParam encounterParam = new ReferenceParam();
			encounterParam.setValue(encounter.getUuid());
			encounterReference.addValue(new ReferenceOrListParam().add(encounterParam));
			
			TokenAndListParam code = new TokenAndListParam();
			TokenParam codingToken = new TokenParam();
			codingToken.setValue(variableConceptUuid); // using the UUID in OpenMRS concept dictionary
			code.addAnd(codingToken);
			
			Bundle.BundleEntryRequestComponent request = new Bundle.BundleEntryRequestComponent();
			request.setMethod(Bundle.HTTPVerb.POST).setUrl("Observation");
			
			IBundleProvider results = obsResourceProvider.searchObservations(encounterReference, patientReference, null,
			    null, null, null, null, null, code, null, null, null, null, null, null, null);
			
			System.out.println("FHIR Results: " + results.getAllResources().size());
			for (IBaseResource resource : results.getAllResources()) {
				bundle.addEntry().setResource((Resource) resource).setRequest(request);
			}
		}
	}
	
	private static void addPatientObjectToBundle(String patientIdentifier,
	        PatientFhirResourceProvider patientResourceProvider, Bundle bundle) {
		
		TokenAndListParam identifierParam = new TokenAndListParam().addAnd(new TokenOrListParam().add(patientIdentifier));
		IBundleProvider results = patientResourceProvider.searchPatients(null, null, null, identifierParam, null, null,
		    null, null, null, null, null, null, null, null, null, null);
		List<IBaseResource> resources = results.getResources(0, 10);
		IBaseResource patientObject = resources.get(0);
		
		Bundle.BundleEntryRequestComponent request = new Bundle.BundleEntryRequestComponent();
		request.setMethod(Bundle.HTTPVerb.PUT).setUrl("Patient");
		bundle.addEntry().setResource((Resource) patientObject).setRequest(request);
		
	}
	
	/**
	 * Utility method for getting a patient's encounters
	 * 
	 * @param patient
	 * @param type - the encounter type
	 * @param form
	 * @return
	 */
	public static List<Encounter> allEncounters(Patient patient, EncounterType type, List<Form> form) {
		List<Encounter> encounters = Context.getEncounterService().getEncounters(patient, null, null, null, form,
		    Collections.singleton(type), null, null, null, false);
		return encounters;
	}
	
	/**
	 * Gets the last encounter of a specific type and/or form for a patient
	 * 
	 * @param patient
	 * @param type
	 * @param forms
	 * @return
	 */
	public static Encounter lastEncounter(Patient patient, EncounterType type, List<Form> forms) {
		List<Encounter> encounters = Context.getEncounterService().getEncounters(patient, null, null, null, forms,
		    Collections.singleton(type), null, null, null, false);
		return encounters.size() > 0 ? encounters.get(encounters.size() - 1) : null;
	}
}
