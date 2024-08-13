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
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
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
import java.util.UUID;

/**
 * Holder for code that generates FHIR objects
 */
public class ExportFhirObject {
	
	public static final String OPENMRS_ID_TYPE_UUID = "dfacd928-0370-4315-99d7-6ec1c9f7ae76";
	
	private static final String FHIR_EXPORT_BATCH_SIZE = "fhirexport.fhir_export_batch_size";
	
	private static final String FHIR_EXPORT_PATH = "fhirexport.fhir_export_dir";
	
	private static final String FHIR_EXPORT_URL = "fhir_export_server_url";
	
	public String getExportUrl() {
		return exportUrl;
	}
	
	public void setExportUrl(String exportUrl) {
		this.exportUrl = exportUrl;
	}
	
	private String exportUrl;
	
	public boolean isExportToFileSystem() {
		return exportToFileSystem;
	}
	
	public void setExportToFileSystem(boolean exportToFileSystem) {
		this.exportToFileSystem = exportToFileSystem;
	}
	
	private boolean exportToFileSystem;
	
	public boolean isSendToFhirServer() {
		return sendToFhirServer;
	}
	
	public void setSendToFhirServer(boolean sendToFhirServer) {
		this.sendToFhirServer = sendToFhirServer;
	}
	
	private boolean sendToFhirServer;
	
	public String getExportDirectory() {
		return exportDirectory;
	}
	
	public void setExportDirectory(String exportDirectory) {
		this.exportDirectory = exportDirectory;
	}
	
	private String exportDirectory;
	
	public List<String> getBundleCache() {
		return bundleCache;
	}
	
	private final List<String> bundleCache = new ArrayList<String>();
	
	public ExportFhirObject(List<String> controlPatientIds) {
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
	
	public List<String> getControlPatientIds() {
		return controlPatientIds;
	}
	
	public void setControlPatientIds(List<String> controlPatientIds) {
		this.controlPatientIds = controlPatientIds;
	}
	
	private List<String> controlPatientIds;
	
	public void generate() throws IOException {
		
		// Define all forms of interest
		EncounterService encounterService = Context.getEncounterService();
		PatientService patientService = Context.getPatientService();
		
		// hts encounters and observations
		
		String htsTestingEncTypeUuid = "9c0a7a57-62ff-4f75-babe-5835b0e921b7";
		String htsFinalResultConceptUuid = "159427AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"; // final result concept
		String htsResultGivenToPatientConceptUuid = "164848AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"; // test result given concept
		EncounterType htsTestingEncType = encounterService.getEncounterTypeByUuid(htsTestingEncTypeUuid);
		
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
		String effectiveTransferOutDate = "164384AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
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
			for (String id : this.controlPatientIds) {
				controlPatients.add(Context.getPatientService().getPatient(Integer.valueOf(id)));
			}
		}
		
		int exportBatchSize = Context.getAdministrationService().getGlobalPropertyValue(FHIR_EXPORT_BATCH_SIZE, 10);
		int batchCounter = 0;
		int patientsCount = controlPatients.size();
		Bundle bundle = new Bundle();
		for (int i = 0; i < patientsCount; i++) {
			// Set a random uuid value for the bundle
			IIdType bundleId = FhirContext.forR4().getVersion().newIdType();
			bundleId.setValue(UUID.randomUUID().toString());
			bundle.setIdElement((IdType) bundleId);
			
			Patient patient = controlPatients.get(i);
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
			
			List<Encounter> htsTestEncounters = allEncounters(patient, htsTestingEncType, null);
			
			List<Encounter> followupEncounters = allEncounters(patient, followUpEncType,
			    Arrays.asList(hivVisitSummaryForm, hivVisitPoCForm));
			
			List<Encounter> hivEnrolmentEncounters = allEncounters(patient, hivEnrolmentEncType,
			    Arrays.asList(hivEnrolmentForm));
			
			List<Encounter> hivDiscontinuationEncounters = allEncounters(patient, discEncType,
			    Arrays.asList(hivProgramDiscontinuationForm));
			
			Encounter artStartEncounter = getOriginalARTStartEncounter(patient);
			//On ART -- find if client has active ART
			
			if (!htsTestEncounters.isEmpty()) {
				System.out.println("HTS testing encounters: " + htsTestEncounters.size());
				addEncounterObsToBundle(Arrays.asList(htsFinalResultConceptUuid, htsResultGivenToPatientConceptUuid),
				    obsResourceProvider, patientReference, hivEnrolmentEncounters, bundle);
			}
			
			if (!hivEnrolmentEncounters.isEmpty()) {
				System.out.println("HIV enrolment encounters: " + hivEnrolmentEncounters.size());
				addEncounterObsToBundle(Arrays.asList(hivEnrolmentEntryPointConceptUuid), obsResourceProvider,
				    patientReference, hivEnrolmentEncounters, bundle);
			}
			
			if (artStartEncounter != null) {
				System.out.println("ART start encounter");
				addEncounterObsToBundle(Arrays.asList(arvRegimenConceptUuid), obsResourceProvider, patientReference,
				    Arrays.asList(artStartEncounter), bundle);
			}
			
			if (!followupEncounters.isEmpty()) {
				System.out.println("HIV followup encounters: " + followupEncounters.size());
				addEncounterObsToBundle(Arrays.asList(hivFollowupTCAConceptUuid), obsResourceProvider, patientReference,
				    followupEncounters, bundle);
			}
			
			if (!hivDiscontinuationEncounters.isEmpty()) {
				System.out.println("HIV program discontinuation encounters: " + hivDiscontinuationEncounters.size());
				addEncounterObsToBundle(Arrays.asList(hivDiscontinuationReason, effectiveTransferOutDate),
				    obsResourceProvider, patientReference, hivDiscontinuationEncounters, bundle);
			}
			
			batchCounter++;
			
			// Once a batch is created, send it to the defined destination
			if (batchCounter >= exportBatchSize || i == patientsCount - 1) {
				batchCounter = 0;
				this.bundle = bundle;
				sendBundle(bundle);
				bundleCache.add(bundle.getIdElement().getValue());
				bundle = new Bundle();
			}
		}
	}
	
	private static void addEncounterObsToBundle(List<String> variableConceptUuids,
	        ObservationFhirResourceProvider obsResourceProvider, ReferenceAndListParam patientReference,
	        List<Encounter> encounters, Bundle bundle) {
		for (Encounter encounter : encounters) {
			
			ReferenceAndListParam encounterReference = new ReferenceAndListParam();
			ReferenceParam encounterParam = new ReferenceParam();
			encounterParam.setValue(encounter.getUuid());
			encounterReference.addValue(new ReferenceOrListParam().add(encounterParam));
			for (String variableConceptUuid : variableConceptUuids) {
				TokenAndListParam code = new TokenAndListParam();
				TokenParam codingToken = new TokenParam();
				codingToken.setValue(variableConceptUuid); // using the UUID in OpenMRS concept dictionary
				code.addAnd(codingToken);
				
				Bundle.BundleEntryRequestComponent request = new Bundle.BundleEntryRequestComponent();
				request.setMethod(Bundle.HTTPVerb.PUT).setUrl("Observation/" + encounter.getUuid());
				
				IBundleProvider results = obsResourceProvider.searchObservations(encounterReference, patientReference, null,
				    null, null, null, null, null, code, null, null, null, null, null, null, null);
				
				// System.out.println("FHIR Results: " + results.getAllResources().size());
				for (IBaseResource resource : results.getAllResources()) {
					bundle.addEntry().setResource((Resource) resource).setRequest(request);
				}
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
		request.setMethod(Bundle.HTTPVerb.PUT).setUrl("Patient/" + patientObject.getIdElement());
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
		if (this.bundle == null) {
			try {
				this.generate();
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		return FhirContext.forCached(FhirVersionEnum.R4).newJsonParser().setPrettyPrint(true)
		        .encodeResourceToString(this.bundle);
	}
	
	public ExportFhirObject(boolean exportToFileSystem, boolean sendToFhirServer, String exportDirectory) {
		this.exportToFileSystem = exportToFileSystem;
		this.sendToFhirServer = sendToFhirServer;
		this.exportDirectory = exportDirectory;
	}
	
	public ExportFhirObject(List<String> controlPatientIds, boolean exportToFileSystem, boolean sendToFhirServer,
	    String exportDirectory) {
		this.controlPatientIds = controlPatientIds;
		this.exportToFileSystem = exportToFileSystem;
		this.sendToFhirServer = sendToFhirServer;
		this.exportDirectory = exportDirectory;
	}
	
	public void sendBundle(Bundle bundle) throws IOException, RuntimeException {
		
		if (exportToFileSystem) {
			if (this.exportDirectory == null) {
				this.exportDirectory = Context.getAdministrationService()
						.getGlobalPropertyValue(FHIR_EXPORT_PATH, "src/main/resources/");
			}
			
			Date date = new Date();
			String dateString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
			String filePath = exportDirectory + bundle.getIdElement().getValue() + "-" + dateString + ".json";
			java.io.File file = new java.io.File(filePath);
			
			if (file.exists()) {
				java.io.File resourcesFile = new java.io.File(this.exportDirectory, file.getName());
				copyFile(file, resourcesFile);
			}
			else {
				BufferedWriter writer = null;
				try (FileWriter fileWriter = new FileWriter(file)) {
					String bundleJson = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser().setPrettyPrint(true)
							.encodeResourceToString(bundle);
					writer = new BufferedWriter(new FileWriter(file));
					writer.write(bundleJson);
					
					fileWriter.write(bundleJson);
					System.out.println("FHIR Bundle written to: " + file.getAbsolutePath());
				}
				catch (IOException e) {
					throw e;
				}
				finally {
					if (writer != null) {
						writer.close();
					}
				}
			}
		}
		
		if (sendToFhirServer) {
			if (StringUtils.isBlank(exportUrl)) {
				exportUrl = Context.getAdministrationService()
						.getGlobalPropertyValue(FHIR_EXPORT_URL, "http://localhost:8081/fhir");
			}

			FhirContext fhirContext = FhirContext.forR4();
			IGenericClient client = fhirContext.newRestfulGenericClient(exportUrl);

			// Set up basic authentication
			BasicAuthInterceptor authInterceptor = new BasicAuthInterceptor("username", "password");
			client.registerInterceptor(authInterceptor);
			
			try {
				client.transaction().withBundle(bundle).execute();
				System.out.println("Bundle posted successfully!");
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
	}
	
	private static void copyFile(java.io.File sourceFile, java.io.File destFile) throws IOException {
		java.io.FileInputStream inputStream = null;
		java.io.FileOutputStream outputStream = null;
		try {
			inputStream = new java.io.FileInputStream(sourceFile);
			outputStream = new java.io.FileOutputStream(destFile);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = inputStream.read(buffer)) > 0) {
				outputStream.write(buffer, 0, length);
			}
		}
		finally {
			if (inputStream != null) {
				inputStream.close();
			}
			if (outputStream != null) {
				outputStream.close();
			}
		}
	}
}
