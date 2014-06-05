/**
 *  Copyright (c) 2013 SLL <http://sll.se/>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package se.sll.reimbursementadapter.gvr.transform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import riv.followup.processdevelopment.reimbursement.v1.*;
import se.sll.ersmo.xml.indata.Diagnoser.Diagnos;
import se.sll.ersmo.xml.indata.ERSMOIndata;
import se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse;
import se.sll.ersmo.xml.indata.Kon;
import se.sll.ersmo.xml.indata.Tillståndslista.Tillstånd;
import se.sll.ersmo.xml.indata.Yrkeskategorier.Yrkeskategori;
import se.sll.ersmo.xml.indata.Åtgärder.Åtgärd;
import se.sll.reimbursementadapter.admincareevent.model.CommissionState;
import se.sll.reimbursementadapter.admincareevent.model.FacilityState;
import se.sll.reimbursementadapter.admincareevent.service.CodeServerMEKCacheManagerService;
import se.sll.reimbursementadapter.parser.TermItem;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import java.nio.file.Path;
import java.util.*;

/**
 * Transforms a single ERSMOIndata XML object to a number of CareEventType XML objects.
 * Transformation rules implement the "inrapportering_gvr_meddelandeinnehall_0.3"-specification.
 */
public class ERSMOIndataToCareEventTransformer {
	
	private static final Logger LOG = LoggerFactory.getLogger(ERSMOIndataToCareEventTransformer.class);

    private static final String OID_TEMPORARY_PATIENT_ID = "1.2.752.97.3.1.3";
    private static final String OID_COORDINATION_ID = "1.2.752.129.2.1.3.3";
    private static final String OID_PATIENT_IDENTIFIER = "1.2.752.129.2.1.3.1";
    private static final String OID_KV_KÖN = "1.2.752.129.2.2.1.1";
    private static final String OID_HYBRID_GUID_IDENTIFIER = "1.2.752.129.2.1.2.1";
    private static final String OID_KV_KONTAKTTYP = "1.2.752.129.2.2.2.25";
    private static final String OID_ICD10_SE = "1.2.752.116.1.1.1.1.3";
    private static final String OID_KVÅ = "1.2.752.116.1.3.2.1.4";
    private static final String OID_ATC = "1.2.752.129.2.2.3.1.1";
    private static final String OID_KV_LÄN = "1.2.752.129.2.2.1.18";
    private static final String OID_KV_LÄN_TEXT = "KV_LÄN - Länskod enligt SCB";
    private static final String OID_KV_KOMMUN = "1.2.752.129.2.2.1.17";
    private static final String OID_KV_KOMMUN_TEXT = "KV_KOMMUN - Kommunkod enligt SCB";
    private static final String OID_KV_FÖRSAMLING = "1.2.752.129.2.2.1.16";
    private static final String OID_KV_FÖRSAMLING_TEXT = "KV_FÖRSAMLING - Församlingskod enligt SCB";

    private static final String SLL_CAREGIVER_HSA_ID = "SE2321000016-39KJ";

    private static final String HYBRID_GUI_SEPARATOR = "+";

    /**
     * Transforms a single {@link se.sll.ersmo.xml.indata.ERSMOIndata} object to a list of
     * {@link riv.followup.processdevelopment.reimbursement.v1.CareEventType} objects.
     *
     * @param ersmoIndata The {@link se.sll.ersmo.xml.indata.ERSMOIndata} object to transform from.
     * @param fileUpdatedTime The update time for the file the ERSMOIndata was read from. This is used
     *                        for setting the "lastUpdatedTime" parameter in the transformation, which
     *                        is not available in the source data.
     * @param currentFile 
     * @return The transformed list of {@link riv.followup.processdevelopment.reimbursement.v1.CareEventType} objects
     */
	public static List<CareEventType> doTransform(ERSMOIndata ersmoIndata, Date fileUpdatedTime, Path currentFile) {
		LOG.debug("Entering ERSMOIndataToCareEventTransformer.doTransform()");
		// Instantiate the Cache Manager.
		CodeServerMEKCacheManagerService cacheManager = CodeServerMEKCacheManagerService.getInstance();
		// Create the response object.
		List<CareEventType> responseList = new ArrayList<>();
		
		// Iterate over all the ersmoIndata.getErsättningshändelse() and convert them to CareEventType.
		for (Ersättningshändelse currentErsh : ersmoIndata.getErsättningshändelse()) {
            if (currentErsh.getHändelseklass().getVårdkontakt() != null) {
                CareEventType currentEvent = createCareEventFromErsättningshändelse(currentErsh, ersmoIndata, cacheManager, fileUpdatedTime, currentFile);
                responseList.add(currentEvent);
            }
		}

		return responseList;
	}

    /**
     * <p>Transforms a single source {@link se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse} along
     * with the parent {@link se.sll.ersmo.xml.indata.ERSMOIndata} object for metadata into a single, complete
     * {@link riv.followup.processdevelopment.reimbursement.v1.CareEventType} object.</p>
     *
     * <p>To handle the lookup of local care contract data as well as transformation from the local care unit
     * id format (KOMBIKA) to the national HSA format, the stored cache structure in
     * {@link se.sll.reimbursementadapter.admincareevent.service.CodeServerMEKCacheManagerService} is used.</p>
     * 
     * @param currentErsh The {@link se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse} to transform from.
     * @param ersmoIndata The {@link se.sll.ersmo.xml.indata.ERSMOIndata} to transform from.
     * @param cacheManager The {@link se.sll.reimbursementadapter.admincareevent.service.CodeServerMEKCacheManagerService}
     *                     to use for looking up additional information not availible in the source data.
     * @param updatedTime The update time for the file the ERSMOIndata was read from. This is used
     *                    for setting the "lastUpdatedTime" parameter in the transformation, which
     *                    is not available in the source data.
     * @param currentFile 
     * @return The transformed {@link riv.followup.processdevelopment.reimbursement.v1.CareEventType}.
     */
    private static CareEventType createCareEventFromErsättningshändelse(Ersättningshändelse currentErsh, 
                                                                        ERSMOIndata ersmoIndata, 
                                                                        CodeServerMEKCacheManagerService cacheManager, 
                                                                        Date updatedTime, Path currentFile) {
        ObjectFactory of = new ObjectFactory();
        CareEventType currentEvent = of.createCareEventType();

        currentEvent.setId(currentErsh.getID());

        String currentErsId = currentErsh.getID();
        
        // Source System
        currentEvent.setSourceSystem(of.createSourceSystemType());
        currentEvent.getSourceSystem().setOrg(SLL_CAREGIVER_HSA_ID);
        currentEvent.getSourceSystem().setId(ersmoIndata.getKälla());

        // Patient
        if (currentErsh.getPatient() != null) {
            currentEvent.setPatient(of.createPatientType());
            currentEvent.getPatient().setId(of.createPersonIdType());
            currentEvent.getPatient().getId().setId(currentErsh.getPatient().getID());
            if (currentErsh.getPatient().getID().startsWith("99")) {
                // SLL temporary patient identification (reservnummer)
                currentEvent.getPatient().getId().setType(OID_TEMPORARY_PATIENT_ID);
            } else if (Integer.valueOf(currentErsh.getPatient().getID().substring(6,8)) > 60) {
                // National co-ordination number (samordningsnummer) - the birth day has 60 added to it in order to identify it.
                currentEvent.getPatient().getId().setType(OID_COORDINATION_ID);
            } else {
                // Regular person identificator (personnummer)
                currentEvent.getPatient().getId().setType(OID_PATIENT_IDENTIFIER);
            }
            if (currentErsh.getPatient().getFödelsedatum() != null) {
                currentEvent.getPatient().setBirthDate(currentErsh.getPatient().getFödelsedatum().toXMLFormat().replace("-", ""));
            }
            if (currentErsh.getPatient().getKön() != null) {
                currentEvent.getPatient().setGender(new CVType());
                currentEvent.getPatient().getGender().setCodeSystem(OID_KV_KÖN);
                if (currentErsh.getPatient().getKön().equals(Kon.M)) {
                    currentEvent.getPatient().getGender().setCode("2");
                } else if (currentErsh.getPatient().getKön().equals(Kon.K)) {
                    currentEvent.getPatient().getGender().setCode("1");
                }
            }
            currentEvent.getPatient().setResidence(of.createResidenceType());
            if (currentErsh.getPatient().getLkf() != null && currentErsh.getPatient().getLkf().length() >= 6) {
                currentEvent.getPatient().getResidence().setRegion(new CVType());
                currentEvent.getPatient().getResidence().getRegion().setCode(currentErsh.getPatient().getLkf().substring(0, 2));
                currentEvent.getPatient().getResidence().getRegion().setCodeSystem(OID_KV_LÄN);
                currentEvent.getPatient().getResidence().getRegion().setCodeSystemName(OID_KV_LÄN_TEXT);
                currentEvent.getPatient().getResidence().setMunicipality(new CVType());
                currentEvent.getPatient().getResidence().getMunicipality().setCode(currentErsh.getPatient().getLkf().substring(2, 4));
                currentEvent.getPatient().getResidence().getMunicipality().setCodeSystem(OID_KV_KOMMUN);
                currentEvent.getPatient().getResidence().getMunicipality().setCodeSystemName(OID_KV_KOMMUN_TEXT);
                currentEvent.getPatient().getResidence().setParish(new CVType());
                currentEvent.getPatient().getResidence().getParish().setCode(currentErsh.getPatient().getLkf().substring(4, 6));
                currentEvent.getPatient().getResidence().getParish().setCodeSystem(OID_KV_FÖRSAMLING);
                currentEvent.getPatient().getResidence().getParish().setCodeSystemName(OID_KV_FÖRSAMLING_TEXT);
            }

            currentEvent.getPatient().setLocalResidence(currentErsh.getPatient().getBasområde());
        }

        // Care Unit Local Id
        currentEvent.setCareUnit(of.createCareUnitType());
        currentEvent.getCareUnit().setCareUnitLocalId(new IIType());
        currentEvent.getCareUnit().getCareUnitLocalId().setRoot(OID_HYBRID_GUID_IDENTIFIER);
        currentEvent.getCareUnit().getCareUnitLocalId().setExtension(SLL_CAREGIVER_HSA_ID + HYBRID_GUI_SEPARATOR + currentErsh.getSlutverksamhet());

        // Use the Startdatum from the Ersättningshändelse as the key for Code mapping lookup.
        Date stateDate = currentErsh.getStartdatum().toGregorianCalendar().getTime();
        String kombika = currentErsh.getSlutverksamhet();
        TermItem<FacilityState> mappedFacilities = cacheManager.getCurrentIndex().get(kombika);
        if (mappedFacilities != null) {
            // Contract
            currentEvent.setContracts(of.createCareEventTypeContracts());

            if (mappedFacilities.getState(stateDate) != null) {
                // Care Unit HSA-id from MEK
                String careUnitHSAid = mappedFacilities.getState(stateDate).getHSAMapping().getState(stateDate).getHsaId();
                currentEvent.getCareUnit().setCareUnitId(careUnitHSAid);

                // Find the contract mapping with the right type
                for (TermItem<CommissionState> commissionState : mappedFacilities.getState(stateDate).getCommissions()) {
                    if ("06".equals(commissionState.getState(stateDate).getAssignmentType())
                            || "07".equals(commissionState.getState(stateDate).getAssignmentType())
                            || "08".equals(commissionState.getState(stateDate).getAssignmentType())) {
                        CareContractType currentContract = of.createCareContractType();

                        currentContract.setId(of.createIIType());
                        currentContract.getId().setRoot(OID_HYBRID_GUID_IDENTIFIER);
                        currentContract.getId().setExtension(SLL_CAREGIVER_HSA_ID + HYBRID_GUI_SEPARATOR + commissionState.getId());
                        currentContract.setName(commissionState.getState(stateDate).getName());


                        currentContract.setContractType(of.createCVType());
                        currentContract.getContractType().setCodeSystem("SLL.CS.UPPDRAGSTYP");
                        currentContract.getContractType().setCodeSystemName("SLL Code Server definition from the 'UPPDRAGSTYP' table.");
                        currentContract.getContractType().setCode(commissionState.getState(stateDate).getCommissionType().getId());
                        currentContract.getContractType().setDisplayName(commissionState.getState(stateDate).getCommissionType().getState(stateDate).getName());

                        currentContract.setProviderOrganization(careUnitHSAid);
                        currentContract.setPayerOrganization("?Payor?");
                        currentContract.setRequesterOrganization(SLL_CAREGIVER_HSA_ID);

                        currentEvent.getContracts().getContract().add(currentContract);
                    }
                }

            } else {
		// No mapped facilities for the date, error state?
                LOG.error(String.format("No mapped facilities found for the care event (%s) from (%s) with date (%s) using kombika/slutverksamhet (%s). Transformation could not be completed.",
                                        currentErsId, currentFile, stateDate, kombika));
                // TODO: throw mapping exception
                // TODO roos: I am not sure the translation should continue with a translation this broken. Let's discuss it.
            }
        }

        // Set up mapping for the contact referral care unit to HSA-id.
        if (currentErsh.getHändelseklass().getVårdkontakt().getRemissFöre() != null) {
            TermItem<FacilityState> mappedFacilitiesForReferral = cacheManager.getCurrentIndex().get(currentErsh.getHändelseklass().getVårdkontakt().getRemissFöre().getKod());

            if (mappedFacilitiesForReferral != null) {
                String referralHsaId = mappedFacilitiesForReferral.getState(stateDate).getHSAMapping().getState(stateDate).getHsaId();

                // Referred From (HSA-id)
                currentEvent.setReferredFrom(referralHsaId);
            }
        }

        // Last updated time
        XMLGregorianCalendar date2 = null;
        try {
            GregorianCalendar c = new GregorianCalendar();
            c.setTime(updatedTime);
            date2 = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
            date2.setTimezone((TimeZone.getDefault().getRawOffset() + TimeZone.getDefault().getDSTSavings()) / 1000 / 60);

        } catch (DatatypeConfigurationException e) {
            // TODO: Fix
            e.printStackTrace();
        }
        currentEvent.setLastUpdatedTime(date2);

        // Deleted
        currentEvent.setDeleted(currentErsh.isMakulerad());

        // Date Period
        currentEvent.setDatePeriod(of.createDatePeriodType());
        currentEvent.getDatePeriod().setStart(currentErsh.getStartdatum().toXMLFormat().replace("-", ""));
        if (currentErsh.getSlutdatum() != null) {
            currentEvent.getDatePeriod().setEnd(currentErsh.getSlutdatum().toXMLFormat().replace("-", ""));
        }

        if (currentErsh.getHändelseklass() != null) {
            if (currentErsh.getHändelseklass().getVårdkontakt() != null) {
                // Emergency
                if (currentErsh.getHändelseklass().getVårdkontakt().getAkut() != null) {
                    currentEvent.setEmergency(currentErsh.getHändelseklass().getVårdkontakt().getAkut().equals("J"));
                }

                // Event Type
                if (currentErsh.getHändelseklass().getVårdkontakt().getHändelseform() != null) {
                    currentEvent.setEventTypeMain(of.createCVType());
                    currentEvent.getEventTypeMain().setCodeSystem(OID_KV_KONTAKTTYP);
                    currentEvent.getEventTypeMain().setCodeSystemName("KV kontakttyp");
                    currentEvent.getEventTypeMain().setCode(mapErsmoKontaktFormToKvKontakttyp(
                            currentErsh.getHändelseklass().getVårdkontakt().getHändelseform().toString()));

                    currentEvent.setEventTypeSub(of.createCVType());
                    // Create our locally defined OID with the "SLL.CS." + {getHändelsetyp()}
                    String nameOfHandelseTyp = mapNumericHändelsetypToTextRepresentation(currentErsh.getHändelseklass().getVårdkontakt().getHändelsetyp());
                    currentEvent.getEventTypeSub().setCodeSystem("SLL.CS." + nameOfHandelseTyp);
                    currentEvent.getEventTypeSub().setCodeSystemName("SLL Code Server definition from the '" + currentErsh.getHändelseklass().getVårdkontakt().getHändelsetyp() + "' table.");
                    currentEvent.getEventTypeSub().setCode(currentErsh.getHändelseklass().getVårdkontakt().getTyp());
                }

                // Fee Category
                currentEvent.setFeeCategory(of.createCVType());
                currentEvent.getFeeCategory().setCode(currentErsh.getHändelseklass().getVårdkontakt().getPatientavgift());
                if (currentErsh.getHändelseklass().getVårdkontakt().getAvgiftsklass().equals("006")) {
                    currentEvent.getFeeCategory().setCodeSystem("SLL.CS.TAXA");
                } else {
                    currentEvent.getFeeCategory().setCodeSystem("NO.OID: " + currentErsh.getHändelseklass().getVårdkontakt().getAvgiftsklass());
                }

                // Involved Professions
                currentEvent.setInvolvedProfessions(of.createCareEventTypeInvolvedProfessions());
                if (currentErsh.getHändelseklass().getVårdkontakt().getYrkeskategorier() != null && currentErsh.getHändelseklass().getVårdkontakt().getYrkeskategorier().getYrkeskategori() != null && currentErsh.getHändelseklass().getVårdkontakt().getYrkeskategorier().getYrkeskategori().size() > 0) {
                    for (Yrkeskategori kategori : currentErsh.getHändelseklass().getVårdkontakt().getYrkeskategorier().getYrkeskategori()) {
                        ProfessionType currentProfession = of.createProfessionType();
                        currentProfession.setCodeSystem("SLL.CS.VDG");
                        currentProfession.setCodeSystemName("SLL Code Server definition from the 'VDG' table.");
                        currentProfession.setCode(kategori.getKod());
                        currentEvent.getInvolvedProfessions().getProfession().add(currentProfession);
                    }
                }

                // Diagnoses
                currentEvent.setDiagnoses(of.createCareEventTypeDiagnoses());
                if (currentErsh.getHändelseklass().getVårdkontakt().getDiagnoser() != null && currentErsh.getHändelseklass().getVårdkontakt().getDiagnoser().getDiagnos() != null && currentErsh.getHändelseklass().getVårdkontakt().getDiagnoser().getDiagnos().size() > 0) {
                    for (Diagnos diagnos : currentErsh.getHändelseklass().getVårdkontakt().getDiagnoser().getDiagnos()) {
                        DiagnosisType currentDiagnosis = of.createDiagnosisType();
                        if (diagnos.getKlass().equals("008")) {
                            currentDiagnosis.setCodeSystem(OID_ICD10_SE);
                            currentDiagnosis.setCodeSystemName("Internationell statistisk klassifikation av sjukdomar och relaterade hälsoproblem, systematisk förteckning (ICD-10-SE)");
                        } else {
                            currentDiagnosis.setCodeSystem("NO.OID: " + diagnos.getKlass());
                        }
                        currentDiagnosis.setCode(diagnos.getKod());
                        currentEvent.getDiagnoses().getDiagnosis().add(currentDiagnosis);
                    }
                }

                // Conditions
                currentEvent.setConditions(of.createCareEventTypeConditions());
                if (currentErsh.getHändelseklass().getVårdkontakt().getTillståndslista() != null && currentErsh.getHändelseklass().getVårdkontakt().getTillståndslista().getTillstånd() != null && currentErsh.getHändelseklass().getVårdkontakt().getTillståndslista().getTillstånd().size() > 0) {
                    for (Tillstånd tillstånd : currentErsh.getHändelseklass().getVårdkontakt().getTillståndslista().getTillstånd()) {
                        ConditionType currentCondition = of.createConditionType();
                        if (tillstånd.getKlass().equals("010")) {
                            currentCondition.setCodeSystem("SLL.CS.TILLSTAND");
                        } else {
                            currentCondition.setCodeSystem("NO.OID: " + tillstånd.getKlass());
                        }
                        currentCondition.setCode(tillstånd.getKod());
                        currentEvent.getConditions().getCondition().add(currentCondition);
                    }
                }

                // Activities
                currentEvent.setActivities(of.createCareEventTypeActivities());
                if (currentErsh.getHändelseklass().getVårdkontakt().getÅtgärder() != null && currentErsh.getHändelseklass().getVårdkontakt().getÅtgärder().getÅtgärd() != null && currentErsh.getHändelseklass().getVårdkontakt().getÅtgärder().getÅtgärd().size() > 0) {
                    for (Åtgärd åtgärd : currentErsh.getHändelseklass().getVårdkontakt().getÅtgärder().getÅtgärd()) {
                        ActivityType currentActivity = of.createActivityType();
                        if (åtgärd.getKlass().equals("007")) {
                            currentActivity.setCodeSystem(OID_KVÅ);
                            //currentActivity.setCodeSystemName("Klassifikation av vårdåtgärder (KVÅ)");
                        } else if (åtgärd.getKlass().equals("020")) {
                            currentActivity.setCodeSystem(OID_ATC);
                            /*currentActivity.getActivityCode().setCodeSystemName("Anatomical Therapeutic Chemical " +
                                    "classification system (ATC)");*/
                        } else {
                            currentActivity.setCodeSystem("no.oid: " + åtgärd.getKlass());
                        }
                        currentActivity.setCode(åtgärd.getKod());
                        currentActivity.setDate(åtgärd.getDatum().toXMLFormat().replaceAll("-", ""));
                        currentEvent.getActivities().getActivity().add(currentActivity);
                    }
                }

                // Stay Before
                currentEvent.setStayBefore(of.createCVType());
                currentEvent.getStayBefore().setCode(currentErsh.getHändelseklass().getVårdkontakt().getVisteFöre().getKod());
                currentEvent.getStayBefore().setCodeSystem("SLL.CS.IKOD");

                // Stay After
                currentEvent.setStayAfter(of.createCVType());
                currentEvent.getStayAfter().setCode(currentErsh.getHändelseklass().getVårdkontakt().getVisteEfter().getKod());
                currentEvent.getStayAfter().setCodeSystem("SLL.CS.UKOD");

                // Deceased
                currentEvent.setDeceased(currentErsh.getHändelseklass().getVårdkontakt().getVisteEfter().getKod().equals("7"));
            }
        }
        return currentEvent;
    }

    /**
     * Maps a string representation of a care contact form (KontaktForm) from GVR to the nationally approved
     * "KV kontakttyp" code system.
     *
     * @param ersmoKontaktForm The string representation of a care contact form (KontaktForm). Could be "Öppenvårdskontakt",
     *                         "Slutenvårdstillfälle" or "Hemsjukvårdskontakt".
     * @return The mapped "KV kontakttyp" representation (1-4).
     */
    public static String mapErsmoKontaktFormToKvKontakttyp(String ersmoKontaktForm) {
        String trimmedInput = ersmoKontaktForm.trim().toLowerCase();

        switch (trimmedInput) {
            case "öppenvårdskontakt":
                return "2";
            case "slutenvårdstillfälle":
                return "1";
            case "hemsjukvårdskontakt":
                return "4";
            default:
                return "";
        }
    }

    /**
     * Maps a numeric Händelsetyp code to the corresponding text representation.
     * @param händelsetyp the numeric Händelsetyp code to map.
     * @return A String with the text representation of the Händelsetyp.
     */
    public static String mapNumericHändelsetypToTextRepresentation(String händelsetyp) {
        String trimmedInput = händelsetyp.trim();
        if ("013".equals(trimmedInput)) {
            return "BTYP";
        } else if ("014".equals(trimmedInput)) {
            return "VKTYP";
        }

        return "";
    }
}
