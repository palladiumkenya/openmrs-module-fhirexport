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
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.EncounterService;
import org.openmrs.api.FormService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.providers.r4.ObservationFhirResourceProvider;
import org.openmrs.module.fhir2.providers.r4.PatientFhirResourceProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

/**
 * Holder for code that generates FHIR objects
 */
public class ExportFhirObject {
	
	public static final String OPENMRS_ID_TYPE_UUID = "dfacd928-0370-4315-99d7-6ec1c9f7ae76";
	
	public ExportFhirObject(List<Integer> controlPatientIds) {
		this.controlPatientIds = controlPatientIds;
	}
	
	public ExportFhirObject() {
	}
	
	public IBaseResource getBundle() {
		return bundle;
	}
	
	public void setBundle(IBaseResource bundle) {
		this.bundle = bundle;
	}
	
	private IBaseResource bundle;
	
	public List<Integer> getControlPatientIds() {
		return controlPatientIds;
	}
	
	public void setControlPatientIds(List<Integer> controlPatientIds) {
		this.controlPatientIds = controlPatientIds;
	}
	
	private List<Integer> controlPatientIds;
	
	public IBaseResource generate() {
		
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
		
		String arvRegimenConceptUuid = "1193AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		
		Map<String, IBaseBundle> obsCache = new HashMap<String, IBaseBundle>();
		PatientFhirResourceProvider patientResourceProvider = Context.getRegisteredComponent(
		    "patientFhirR4ResourceProvider", PatientFhirResourceProvider.class);
		ObservationFhirResourceProvider obsResourceProvider = Context.getRegisteredComponent(
		    "observationFhirR4ResourceProvider", ObservationFhirResourceProvider.class);
		
		// List<Patient> controlPatients = Collections.singletonList(Context.getPatientService().getPatient(39857));
		List<Patient> controlPatients = new ArrayList<Patient>();
		if (this.controlPatientIds == null || this.controlPatientIds.isEmpty()) {
			controlPatients = Context.getPatientService().getAllPatients(false);
		} else {
			for (Integer i : this.controlPatientIds) {
				controlPatients.add(Context.getPatientService().getPatient(this.controlPatientIds.get(i)));
			}
		}
		
		Bundle bundle = new Bundle();
		for (Patient patient : controlPatients) {
			
			// generate patient objects
			PatientIdentifierType openmrsIdType = patientService.getPatientIdentifierTypeByUuid(OPENMRS_ID_TYPE_UUID);
			PatientIdentifier openmrsId = patient.getPatientIdentifier(openmrsIdType); // all patients have openmrs id thus the preference
			
			ReferenceAndListParam patientReference = new ReferenceAndListParam();
			ReferenceParam patientParam = new ReferenceParam();
			patientParam.setValue(patient.getUuid());
			patientReference.addValue(new ReferenceOrListParam().add(patientParam));
			
			addPatientObjectToBundle(openmrsId.getIdentifier(), patientResourceProvider, bundle);
			
			// generate observation objects
			//1. get all hiv enrolment encounters
			//2. get ART start encounter
			//3. get all hiv follow up encounters
			//4. get all hiv program discontinuation encounters
			List<Encounter> followupEncounters = allEncounters(patient, followUpEncType,
			    Arrays.asList(hivVisitSummaryForm, hivVisitPoCForm));
			
			List<Encounter> hivEnrolmentEncounters = allEncounters(patient, hivEnrolmentEncType,
			    Arrays.asList(hivEnrolmentForm));
			
			List<Encounter> hivDiscontinuationEncounters = allEncounters(patient, discEncType,
			    Arrays.asList(hivProgramDiscontinuationForm));
			
			Encounter artStartEncounter = getOriginalARTStartEncounter(patient);
			//On ART -- find if client has active ART
			
			if (!hivEnrolmentEncounters.isEmpty()) {
				System.out.println("HIV enrolment encounters: " + hivEnrolmentEncounters.size());
				addEncounterObsToBundle(hivEnrolmentEntryPointConceptUuid, obsResourceProvider, patientReference,
				    hivEnrolmentEncounters, bundle);
			}
			
			if (artStartEncounter != null) {
				System.out.println("ART start encounter");
				addEncounterObsToBundle(arvRegimenConceptUuid, obsResourceProvider, patientReference,
				    Arrays.asList(artStartEncounter), bundle);
			}
			
			if (!followupEncounters.isEmpty()) {
				System.out.println("HIV followup encounters: " + followupEncounters.size());
				addEncounterObsToBundle(hivFollowupTCAConceptUuid, obsResourceProvider, patientReference,
				    followupEncounters, bundle);
			}
			
			if (!hivDiscontinuationEncounters.isEmpty()) {
				System.out.println("HIV program discontinuation encounters: " + hivDiscontinuationEncounters.size());
				addEncounterObsToBundle(hivDiscontinuationReason, obsResourceProvider, patientReference,
				    hivDiscontinuationEncounters, bundle);
			}
			
		}
		return bundle;
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
	
	public static Encounter getOriginalARTStartEncounter(Patient patient) {
		String DRUG_REGIMEN_EDITOR_FORM_UUID = "da687480-e197-11e8-9f32-f2801f1b9fd1";
		String DRUG_REGIMEN_EDITOR_ENC_TYPE_UUID = "7dffc392-13e7-11e9-ab14-d663bd873d93";
		
		FormService formService = Context.getFormService();
		EncounterService encounterService = Context.getEncounterService();
		String ARV_TREATMENT_PLAN_EVENT_CONCEPT = "1255AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		
		EncounterType et = encounterService.getEncounterTypeByUuid(DRUG_REGIMEN_EDITOR_ENC_TYPE_UUID);
		Form form = formService.getFormByUuid(DRUG_REGIMEN_EDITOR_FORM_UUID);
		
		List<Encounter> encs = allEncounters(patient, et, Arrays.asList(form));
		NavigableMap<Date, Encounter> programEncs = new TreeMap<Date, Encounter>();
		for (Encounter e : encs) {
			if (e != null) {
				Set<Obs> obs = e.getObs();
				if (isARTStartEncounter(obs, ARV_TREATMENT_PLAN_EVENT_CONCEPT)) {
					programEncs.put(e.getEncounterDatetime(), e);
				}
			}
		}
		if (!programEncs.isEmpty()) {
			return programEncs.firstEntry().getValue();
		}
		return null;
	}
	
	public static boolean isARTStartEncounter(Set<Obs> obs, String conceptUuidToMatch) {
		for (Obs o : obs) {
			if (o.getConcept().getUuid().equals(conceptUuidToMatch)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String toString() {
		bundle = this.generate();
		return FhirContext.forCached(FhirVersionEnum.R4).newJsonParser().setPrettyPrint(true)
		        .encodeResourceToString(this.bundle);
	}
	
	public void sendBundle(Bundle bundle) {
		IGenericClient client = FhirContext.forCached(FhirVersionEnum.R4).newRestfulGenericClient(
		    "http://localhost:8080/fhir");
		BasicAuthInterceptor authInterceptor = new BasicAuthInterceptor("", "");
		client.registerInterceptor(authInterceptor);
		try {
			client.transaction().withBundle(bundle).execute();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
}
