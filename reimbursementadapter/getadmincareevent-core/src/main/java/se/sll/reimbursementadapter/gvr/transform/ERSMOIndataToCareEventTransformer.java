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
import se.sll.ersmo.xml.indata.Vkhform;
import se.sll.ersmo.xml.indata.Yrkeskategorier.Yrkeskategori;
import se.sll.ersmo.xml.indata.Åtgärder.Åtgärd;
import se.sll.reimbursementadapter.admincareevent.model.CommissionState;
import se.sll.reimbursementadapter.admincareevent.model.FacilityState;
import se.sll.reimbursementadapter.admincareevent.model.HSAMappingState;
import se.sll.reimbursementadapter.admincareevent.model.TermItemCommission;
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

    // Kodverk
    private static final String OID_TEMPORARY_PATIENT_ID = "1.2.752.97.3.1.3";
    private static final String OID_COORDINATION_ID = "1.2.752.129.2.1.3.3";
    private static final String OID_PATIENT_IDENTIFIER = "1.2.752.129.2.1.3.1";
    private static final String OID_HYBRID_GUID_IDENTIFIER = "1.2.752.129.2.1.2.1";
    private static final String OID_ICD10_SE = "1.2.752.116.1.1.1.1.3";
    private static final String OID_KVÅ = "1.2.752.116.1.3.2.1.4";
    private static final String OID_ATC = "1.2.752.129.2.2.3.1.1";

    // Formella kodverk enligt V-TIM 2.0.
    private static final String OID_KV_KÖN = "1.2.752.129.2.2.1.1";
    private static final String OID_KV_KONTAKTTYP = "1.2.752.129.2.2.2.25";
    private static final String OID_KV_LÄN = "1.2.752.129.2.2.1.18";
    private static final String OID_KV_LÄN_TEXT = "KV_LÄN - Länskod enligt SCB";
    private static final String OID_KV_KOMMUN = "1.2.752.129.2.2.1.17";
    private static final String OID_KV_KOMMUN_TEXT = "KV_KOMMUN - Kommunkod enligt SCB";
    private static final String OID_KV_FÖRSAMLING = "1.2.752.129.2.2.1.16";
    private static final String OID_KV_FÖRSAMLING_TEXT = "KV_FÖRSAMLING - Församlingskod enligt SCB";

    // Egna kodverk skapade från Codeserver.
    private static final String OID_SLL_CS_UPPDRAGSTYP = "SLL.CS.UPPDRAGSTYP";
    private static final String OID_SLL_CS_UPPDRADSTYP_TEXT = "SLL Code Server definition from the 'UPPDRAGSTYP' table.";

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
    public static List<CareEventType> doTransform(ERSMOIndata ersmoIndata, Date fileUpdatedTime, Path currentFile) throws TransformationException {
        // Instantiate the Cache Manager.
        CodeServerMEKCacheManagerService cacheManager = CodeServerMEKCacheManagerService.getInstance();
        // Create the response object.
        List<CareEventType> responseList = new ArrayList<>();
        // Iterate over all the ersmoIndata.getErsättningshändelse() and convert them to CareEventType.
        List<Ersättningshändelse> list = ersmoIndata.getErsättningshändelse();
        LOG.info(String.format("Transforming file %s with %d care events updated at %s.", currentFile, list.size(), fileUpdatedTime));
        for (Ersättningshändelse currentErsh : list) {
            if (currentErsh.getHändelseklass().getVårdkontakt() != null) {
                CareEventType currentEvent = createCareEventFromErsättningshändelse(currentErsh, ersmoIndata, cacheManager, 
                                                                                    fileUpdatedTime, currentFile);
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
                                                                        Date updatedTime, Path currentFile) throws TransformationException {
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
            currentEvent.setPatient(createRivPatientFromErsättningsPatient(currentErsh.getPatient()));
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
                final FacilityState state1 = mappedFacilities.getState(stateDate);
                if (state1 == null) {
                    LOG.error(String.format("The specified facility code '%s' does not exist in the Facility file for the date '%s'.", mappedFacilities.getId(), stateDate));
                    throw new TransformationException(String.format("The specified Facility (Kombika) code '%s' does not exist in the Facility (AVD) file for the date '%s'. Source file %s and care event %s.", mappedFacilities.getId(), stateDate, currentErsId, currentFile));
                }

                TermItem<HSAMappingState> hsaMappingState = state1.getHSAMapping();
                String careUnitHSAid;
                if (hsaMappingState != null) {
                    careUnitHSAid = hsaMappingState.getState(stateDate).getHsaId();
                    currentEvent.getCareUnit().setCareUnitId(careUnitHSAid);
                } else {
                    LOG.info("The specified facility code <" + mappedFacilities.getId() + "> does not exist in the HSA(MEK) mapping file.");
                    throw new TransformationException(String.format("The specified Facility code (Kombika) '%s' does not exist in the Facility (AVD) file for the date '%s'. Source file %s and care event %s.", mappedFacilities.getId(), stateDate, currentErsId, currentFile));
                }

                // Find the contract mapping with the right type
                for (TermItemCommission<CommissionState> commissionState : mappedFacilities.getState(stateDate).getCommissions()) {
                    if ("06".equals(commissionState.getState(stateDate).getAssignmentType())
                            || "07".equals(commissionState.getState(stateDate).getAssignmentType())
                            || "08".equals(commissionState.getState(stateDate).getAssignmentType())) {
                        CareContractType currentContract = of.createCareContractType();

                        // Contract Id
                        currentContract.setId(of.createIIType());
                        currentContract.getId().setRoot(OID_HYBRID_GUID_IDENTIFIER);
                        currentContract.getId().setExtension(SLL_CAREGIVER_HSA_ID + HYBRID_GUI_SEPARATOR + commissionState.getId());
                        currentContract.setName(commissionState.getState(stateDate).getName());

                        // Contract type
                        currentContract.setContractType(of.createCVType());
                        currentContract.getContractType().setCodeSystem(OID_SLL_CS_UPPDRAGSTYP);
                        currentContract.getContractType().setCodeSystemName(OID_SLL_CS_UPPDRADSTYP_TEXT);
                        currentContract.getContractType().setCode(commissionState.getState(stateDate).getCommissionType().getId());
                        currentContract.getContractType().setDisplayName(commissionState.getState(stateDate).getCommissionType().getState(stateDate).getName());


                        // RequesterOrganization
                        currentContract.setRequesterOrganization(SLL_CAREGIVER_HSA_ID);

                        // PayerOrganization
                        currentContract.setPayerOrganization(getPayerOrganization(currentErsh.getHändelseklass().getVårdkontakt().getHändelseform() , stateDate, mappedFacilities.getState(stateDate), commissionState, currentContract.getRequesterOrganization()));

                        // ProviderOrganization
                        currentContract.setProviderOrganization(careUnitHSAid);

                        currentEvent.getContracts().getContract().add(currentContract);
                    }
                }
            } else {
                LOG.error(String.format("Did not find code server data with date %s for kombika %s on care event %s in %s.",
                                        stateDate, kombika, currentErsId, currentFile));
                throw new TransformationException(String.format("Did not find code server data with date %s for kombika %s on care event %s in %s.",
                        stateDate, kombika, currentErsId, currentFile));
            }
        }
        else {
            LOG.error(String.format("Did not find code server data for kombika %s on care event %s in %s.",
                                    kombika, currentErsId, currentFile));
            throw new TransformationException(String.format("Did not find code server data for kombika %s on care event %s in %s.",
                    kombika, currentErsId, currentFile));
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
        XMLGregorianCalendar date2;
        try {
            GregorianCalendar c = new GregorianCalendar();
            c.setTime(updatedTime);
            date2 = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
            date2.setTimezone((TimeZone.getDefault().getRawOffset() + TimeZone.getDefault().getDSTSavings()) / 1000 / 60);

        } catch (DatatypeConfigurationException e) {
            LOG.error("DatatypeConfigurationException occured when creating a new DatatypeFactory.", e);
            throw new TransformationException("Internal occured when creating a new DatatypeFactory.");
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
                            currentErsh.getHändelseklass().getVårdkontakt().getHändelseform()));

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
                        } else if (åtgärd.getKlass().equals("020")) {
                            currentActivity.setCodeSystem(OID_ATC);
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
     * Creates a PatientType from the information in the incoming {@link Ersättningshändelse.Patient}.
     *
     * @param ersättningPatient the incoming {@link Ersättningshändelse.Patient} to map data from.
     * @return the mapped PatientType.
     */
    private static PatientType createRivPatientFromErsättningsPatient(Ersättningshändelse.Patient ersättningPatient) {
        ObjectFactory of = new ObjectFactory();
        final PatientType rivPatient = of.createPatientType();

        // Set Patient Id.
        rivPatient.setId(of.createPersonIdType());

        rivPatient.getId().setId(ersättningPatient.getID());

        if (ersättningPatient.getID().startsWith("99")) {
            // SLL temporary rivPatient identification (reservnummer)
            rivPatient.getId().setType(OID_TEMPORARY_PATIENT_ID);
        } else if (Integer.valueOf(ersättningPatient.getID().substring(6,8)) > 60) {
            // National co-ordination number (samordningsnummer) - the birth day has 60 added to it in order to identify it.
            rivPatient.getId().setType(OID_COORDINATION_ID);
        } else {
            // Regular person identificator (personnummer)
            rivPatient.getId().setType(OID_PATIENT_IDENTIFIER);
        }

        // Patient birth date.
        if (ersättningPatient.getFödelsedatum() != null) {
            rivPatient.setBirthDate(ersättningPatient.getFödelsedatum().toXMLFormat().replace("-", ""));
        }

        // Patient gender.
        if (ersättningPatient.getKön() != null) {
            rivPatient.setGender(new CVType());
            rivPatient.getGender().setCodeSystem(OID_KV_KÖN);
            if (ersättningPatient.getKön().equals(Kon.M)) {
                rivPatient.getGender().setCode("2");
            } else if (ersättningPatient.getKön().equals(Kon.K)) {
                rivPatient.getGender().setCode("1");
            }
        }

        // Patient residence region.
        rivPatient.setResidence(createRivResidenceFromErsättningLkf(ersättningPatient.getLkf()));

        rivPatient.setLocalResidence(ersättningPatient.getBasområde());
        return rivPatient;
    }

    /**
     * Creates a RIV {@link ResidenceType} from the incoming string with LKF information from the
     * source Ersättningshändelse.
     *
     * @param ersättningsLkf A string with LKF-information of the formnat "LLKKFF".
     * @return The mapped {@link ResidenceType}.
     */
    public static ResidenceType createRivResidenceFromErsättningLkf(String ersättningsLkf) {
        ObjectFactory of = new ObjectFactory();
        final ResidenceType rivResidence = of.createResidenceType();
        if (ersättningsLkf != null && ersättningsLkf.length() >= 6) {
            // Patient residence region
            rivResidence.setRegion(new CVType());
            rivResidence.getRegion().setCode(ersättningsLkf.substring(0, 2));
            rivResidence.getRegion().setCodeSystem(OID_KV_LÄN);
            rivResidence.getRegion().setCodeSystemName(OID_KV_LÄN_TEXT);

            // Patient residence municipality
            rivResidence.setMunicipality(new CVType());
            rivResidence.getMunicipality().setCode(ersättningsLkf.substring(2, 4));
            rivResidence.getMunicipality().setCodeSystem(OID_KV_KOMMUN);
            rivResidence.getMunicipality().setCodeSystemName(OID_KV_KOMMUN_TEXT);

            // Patient residence parish
            rivResidence.setParish(new CVType());
            rivResidence.getParish().setCode(ersättningsLkf.substring(4, 6));
            rivResidence.getParish().setCodeSystem(OID_KV_FÖRSAMLING);
            rivResidence.getParish().setCodeSystemName(OID_KV_FÖRSAMLING_TEXT);
        }
        return rivResidence;
    }

    /**
     * Returns the HSA-id for the payerOrganization for the current commissionState.
     *
     * @param kontaktForm The type of contact, primary care or inpatient care.
     * @param stateDate The date to use for lookup code mapping states.
     * @param currentFacility The currently active Facility.
     * @param commissionState The currently active Commission.
     * @return the HSA-id for the payerOrganization for the current commissionState.
     */
    public static String getPayerOrganization(Vkhform kontaktForm, Date stateDate, FacilityState currentFacility, TermItemCommission<CommissionState> commissionState, String requesterOrgHsa) {
        String payerOrganization = null;
        List<String> allowedPrimaryCareUnitTypes = Arrays.asList("31", "40", "42", "43", "44", "45", "46", "48", "50", "51", "78", "90", "95", "99");
        List<String> allowedInpatientCareUnitTypes = Arrays.asList("10", "11", "20");

        // Steps to look up payer org from care event kombika:
        // lookup of AVD from kombika
        // lookup of SAMVERKS from AVD where SAMVERKStyp is in 06 07 08 (done before this code)
        // lookup of all other kombika that is connected to the SAMVERKS
        // lookup of AVDs from kombikas
        // select AVD that has correct (9175) KUND and AVDELNINGSTYP/MOTTAGNINSTYP is correct (in list above) in regards to öppenvård/slutenvård
        // => profit

        if (currentFacility != null && !"0000".equals(currentFacility.getCustomerCode())) {
            return requesterOrgHsa;
        }
        
        for (FacilityState currentPayerFacility : getPotentialPayerFacilities(stateDate, commissionState)) {
            
            // Om det är en öppenvårdskontakt vars vårdenhetstyp finns med i allowedPrimaryCareUnitTypes, mappa.
            if (kontaktForm.equals(Vkhform.ÖPPENVÅRDSKONTAKT)
                    && allowedPrimaryCareUnitTypes.contains(currentPayerFacility.getCareUnitType())) {
                payerOrganization = currentPayerFacility.getHSAMapping().getState(stateDate).getHsaId();
            }
            
            // Om det är en slutenvårdskontakt vars vårdenhetstyp finns med i allowedInpatientCareUnitTypes, mappa.
            if (kontaktForm.equals(Vkhform.SLUTENVÅRDSTILLFÄLLE)
                    && allowedInpatientCareUnitTypes.contains(currentPayerFacility.getCareUnitType())) {
                payerOrganization = currentPayerFacility.getHSAMapping().getState(stateDate).getHsaId();
            }
        }
        return payerOrganization;
    }

    /**
     * Returns a list of FacilityStates that are candidates for usage as payer facilities. The logic works in two steps:
     * 1: If the currentFacility is of the customer type (KUNDKOD) '0000', then the payer facility must be located using a backreference
     * to lookup all the care units connected to the current CommissionState. If there is a unit that has a customer code
     * of '9175', the unit is added to the list of payer candidates.
     *
     * @param stateDate The date to use for lookup code mapping states.
     * @param commissionState The currently active Commission.
     * @return A List with the FacilityState for all the potential payerFacilities.
     */
    public static List<FacilityState> getPotentialPayerFacilities(Date stateDate, TermItemCommission<CommissionState> commissionState) {
        List<FacilityState> payerFacilities = new ArrayList<>();
        if (commissionState != null) {
            List<TermItem<FacilityState>> backRefs = commissionState.getBackRefs();
            for (TermItem<FacilityState> currentBackRef : backRefs) {
                FacilityState state = currentBackRef.getState(stateDate);
                // If the current facility has a Customer Code of 9175 "Betalningsansvar samtlig medicinsk service", add it to the map
                // of potential payerFacilities.
                if (state != null && state.getCustomerCode().equals("9175")) {
                    payerFacilities.add(state);
                }
            }
        }
        return payerFacilities;
    }

    /**
     * Maps a string representation of a care contact form (KontaktForm) from GVR to the nationally approved
     * "KV kontakttyp" code system.
     *
     * @param ersmoKontaktForm The string representation of a care contact form (KontaktForm). Could be "Öppenvårdskontakt",
     *                         "Slutenvårdstillfälle" or "Hemsjukvårdskontakt".
     * @return The mapped "KV kontakttyp" representation (1-4).
     */
    public static String mapErsmoKontaktFormToKvKontakttyp(Vkhform ersmoKontaktForm) {
        switch (ersmoKontaktForm) {
            case ÖPPENVÅRDSKONTAKT:
                return "2";
            case SLUTENVÅRDSTILLFÄLLE:
                return "1";
            case HEMSJUKVÅRDSKONTAKT:
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
