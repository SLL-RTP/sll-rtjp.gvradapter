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

import riv.followup.processdevelopment.reimbursement.v1.*;
import riv.followup.processdevelopment.reimbursement.v1.ObjectFactory;
import se.sll.ersmo.xml.indata.*;
import se.sll.reimbursementadapter.admincareevent.model.CommissionState;
import se.sll.reimbursementadapter.admincareevent.model.CommissionTypeState;
import se.sll.reimbursementadapter.admincareevent.model.FacilityState;
import se.sll.reimbursementadapter.admincareevent.model.HSAMappingState;
import se.sll.reimbursementadapter.admincareevent.model.TermItemCommission;
import se.sll.reimbursementadapter.exception.TransformationException;
import se.sll.reimbursementadapter.parser.TermItem;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

/**
 * Contains most of the actual transformation logic for the {@link ERSMOIndataToCareEventTransformer}
 * Contains only static methods and thus is completely stateless.
 */
public class TransformHelper {

    protected static final String SLL_CAREGIVER_HSA_ID = "SE2321000016-39KJ";
    protected static final String HYBRID_GUI_SEPARATOR = "+";
    private static final ObjectFactory of = new ObjectFactory();
    
    private static final Logger LOG = LoggerFactory.getLogger(TransformHelper.class);

    /**
     * Creates the SourceSystem structure in the incoming {@link riv.followup.processdevelopment.reimbursement.v1.CareEventType} using information from
     * the incoming {@link se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse}.
     *
     * @param ersmoIndata The {@link se.sll.ersmo.xml.indata.ERSMOIndata} to read information from.
     * @param currentEvent The {@link riv.followup.processdevelopment.reimbursement.v1.CareEventType} to write the new structure in.
     */
    protected static void createSourceSystemStructure(ERSMOIndata ersmoIndata, CareEventType currentEvent) {
        currentEvent.setSourceSystem(of.createSourceSystemType());
        currentEvent.getSourceSystem().setOrg(SLL_CAREGIVER_HSA_ID);
        currentEvent.getSourceSystem().setId(ersmoIndata.getKälla());
    }

    /**
     * Creates the InvolvedProfessions structure in the incoming {@link riv.followup.processdevelopment.reimbursement.v1.CareEventType} using information from
     * the incoming {@link se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse}.
     *
     * @param currentErsh The {@link se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse} to read information from.
     * @param currentEvent The {@link riv.followup.processdevelopment.reimbursement.v1.CareEventType} to write the new structure in.
     */
    protected static void createInvolvedProfessionsStructure(ERSMOIndata.Ersättningshändelse currentErsh, CareEventType currentEvent) {
        currentEvent.setInvolvedProfessions(of.createCareEventTypeInvolvedProfessions());
        if (currentErsh.getHändelseklass().getVårdkontakt().getYrkeskategorier() != null && currentErsh.getHändelseklass().getVårdkontakt().getYrkeskategorier().getYrkeskategori() != null && currentErsh.getHändelseklass().getVårdkontakt().getYrkeskategorier().getYrkeskategori().size() > 0) {
            for (Yrkeskategorier.Yrkeskategori kategori : currentErsh.getHändelseklass().getVårdkontakt().getYrkeskategorier().getYrkeskategori()) {
                currentEvent.getInvolvedProfessions().getProfession().add(getProfessionFromYrkeskategori(kategori));
            }
        }
    }

    /**
     * Creates a ProfessionType RIV object from the incoming Yrkeskategori.
     *
     * @param kategori THe source for the transformation.
     * @return The populated ProfessionType object.
     */
    protected static ProfessionType getProfessionFromYrkeskategori(Yrkeskategorier.Yrkeskategori kategori) {
        ProfessionType currentProfession = of.createProfessionType();
        currentProfession.setCodeSystem(OIDList.getOid(CodeSystem.SLL_CS_VDG));
        currentProfession.setCodeSystemName(OIDList.getName(CodeSystem.SLL_CS_VDG));
        currentProfession.setCode(kategori.getKod());
        return currentProfession;
    }

    /**
     * Creates the CareUnit structure in the incoming {@link riv.followup.processdevelopment.reimbursement.v1.CareEventType} using information from
     * the incoming {@link se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse}.
     *
     * @param currentErsh The {@link se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse} to read information from.
     * @param currentEvent The {@link riv.followup.processdevelopment.reimbursement.v1.CareEventType} to write the new structure in.
     */
    protected static String createCareUnitStructure(ERSMOIndata.Ersättningshändelse currentErsh, Path currentFile, CareEventType currentEvent, String currentErsId, Date stateDate, FacilityState mappedFacility) throws TransformationException {
        // Care Unit Local Id
        currentEvent.setCareUnit(of.createCareUnitType());
        currentEvent.getCareUnit().setCareUnitLocalId(new IIType());
        currentEvent.getCareUnit().getCareUnitLocalId().setRoot(OIDList.getOid(CodeSystem.HYBRID_GUID_IDENTIFIER));
        currentEvent.getCareUnit().getCareUnitLocalId().setExtension(SLL_CAREGIVER_HSA_ID + HYBRID_GUI_SEPARATOR + currentErsh.getSlutverksamhet());

        // Care Unit HSA-id from MEK
        if (mappedFacility != null) {
            TermItem<HSAMappingState> hsaMapping = mappedFacility.getHSAMapping();
            if (hsaMapping != null) {
                HSAMappingState hsaMappingState = hsaMapping.getState(stateDate);
                if (hsaMappingState != null) {
                    String careUnitHSAid = hsaMappingState.getHsaId();
                    currentEvent.getCareUnit().setCareUnitId(careUnitHSAid);
                    return careUnitHSAid;
                }
            }
        } else {
            currentEvent.getCareUnit().setCareUnitId("");
        }
        return null;
    }

    /**
     * Creates the Activity structure in the incoming {@link riv.followup.processdevelopment.reimbursement.v1.CareEventType} using information from
     * the incoming {@link se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse}.
     *
     * @param currentErsh The {@link se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse} to read information from.
     * @param currentEvent The {@link riv.followup.processdevelopment.reimbursement.v1.CareEventType} to write the new structure in.
     */
    protected static void createActivityStructure(ERSMOIndata.Ersättningshändelse currentErsh, CareEventType currentEvent) {
        currentEvent.setActivities(of.createCareEventTypeActivities());
        if (currentErsh.getHändelseklass().getVårdkontakt().getÅtgärder() != null && currentErsh.getHändelseklass().getVårdkontakt().getÅtgärder().getÅtgärd() != null && currentErsh.getHändelseklass().getVårdkontakt().getÅtgärder().getÅtgärd().size() > 0) {
            for (Åtgärder.Åtgärd åtgärd : currentErsh.getHändelseklass().getVårdkontakt().getÅtgärder().getÅtgärd()) {
                ActivityType currentActivity = getActivityFromÅtgärd(åtgärd);
                currentEvent.getActivities().getActivity().add(currentActivity);
            }
        }
    }

    /**
     * Creates a ActivityType RIV object from the incoming Åtgärd.
     *
     * @param åtgärd The source for the transformation.
     * @return The populated ActivityType object.
     */
    protected static ActivityType getActivityFromÅtgärd(Åtgärder.Åtgärd åtgärd) {
        ActivityType currentActivity = of.createActivityType();
        if (åtgärd.getKlass().equals("007")) {
            currentActivity.setCodeSystem(OIDList.getOid(CodeSystem.KVÅ));
        } else if (åtgärd.getKlass().equals("020")) {
            // TODO step 2, do not include ATC in reply
            currentActivity.setCodeSystem(OIDList.getOid(CodeSystem.ATC));
        } else {
            currentActivity.setCodeSystem("no.oid: " + åtgärd.getKlass());
        }
        currentActivity.setCode(åtgärd.getKod());
        currentActivity.setDate(åtgärd.getDatum().toXMLFormat().replaceAll("-", ""));
        return currentActivity;
    }

    /**
     * Creates the Condition structure in the incoming {@link riv.followup.processdevelopment.reimbursement.v1.CareEventType} using information from
     * the incoming {@link se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse}.
     *
     * @param currentErsh The {@link se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse} to read information from.
     * @param currentEvent The {@link riv.followup.processdevelopment.reimbursement.v1.CareEventType} to write the new structure in.
     */
    protected static void createConditionStructure(ERSMOIndata.Ersättningshändelse currentErsh, CareEventType currentEvent) {
        currentEvent.setConditions(of.createCareEventTypeConditions());
        if (currentErsh.getHändelseklass().getVårdkontakt().getTillståndslista() != null && currentErsh.getHändelseklass().getVårdkontakt().getTillståndslista().getTillstånd() != null && currentErsh.getHändelseklass().getVårdkontakt().getTillståndslista().getTillstånd().size() > 0) {
            for (Tillståndslista.Tillstånd tillstånd : currentErsh.getHändelseklass().getVårdkontakt().getTillståndslista().getTillstånd()) {
                ConditionType currentCondition = getConditionFromTillstånd(tillstånd);
                currentEvent.getConditions().getCondition().add(currentCondition);
            }
        }
    }

    /**
     * Creates a ConditionType RIV object from the incoming Tillstånd.
     *
     * @param tillstånd The source for the transformation.
     * @return The populated ConditionType object.
     */
    protected static ConditionType getConditionFromTillstånd(Tillståndslista.Tillstånd tillstånd) {
        ConditionType currentCondition = of.createConditionType();
        if (tillstånd.getKlass().equals("010")) {
            currentCondition.setCodeSystem(OIDList.getOid(CodeSystem.SLL_CS_TILLSTÅND));
            currentCondition.setCodeSystemName(OIDList.getName(CodeSystem.SLL_CS_TILLSTÅND));
        } else {
            currentCondition.setCodeSystem("NO.OID: " + tillstånd.getKlass());
        }
        currentCondition.setCode(tillstånd.getKod());
        return currentCondition;
    }

    /**
     * Creates the Diagnosis structure in the incoming {@link riv.followup.processdevelopment.reimbursement.v1.CareEventType} using information from
     * the incoming {@link se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse}.
     *
     * @param currentErsh The {@link se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse} to read information from.
     * @param currentEvent The {@link riv.followup.processdevelopment.reimbursement.v1.CareEventType} to write the new structure in.
     */
    protected static void createDiagnosisStructure(ERSMOIndata.Ersättningshändelse currentErsh, CareEventType currentEvent) {
        currentEvent.setDiagnoses(of.createCareEventTypeDiagnoses());
        if (currentErsh.getHändelseklass().getVårdkontakt().getDiagnoser() != null && currentErsh.getHändelseklass().getVårdkontakt().getDiagnoser().getDiagnos() != null && currentErsh.getHändelseklass().getVårdkontakt().getDiagnoser().getDiagnos().size() > 0) {
            for (Diagnoser.Diagnos diagnos : currentErsh.getHändelseklass().getVårdkontakt().getDiagnoser().getDiagnos()) {
                currentEvent.getDiagnoses().getDiagnosis().add(getDiagnosisFromDiagnos(diagnos));
            }
        }
    }

    /**
     * Creates a DiagnosisType RIV object from the incoming Diagnos.
     *
     * @param diagnos The source for the transformation.
     * @return The populated DiagnosisType object.
     */
    protected static DiagnosisType getDiagnosisFromDiagnos(Diagnoser.Diagnos diagnos) {
        DiagnosisType currentDiagnosis = of.createDiagnosisType();
        if (diagnos.getKlass().equals("008")) {
            currentDiagnosis.setCodeSystem(OIDList.getOid(CodeSystem.ICD10_SE));
            currentDiagnosis.setCodeSystemName(OIDList.getName(CodeSystem.ICD10_SE));
        } else {
            // TODO roos, temporary filter away 020 content (ATC) here.
            currentDiagnosis.setCodeSystem("NO.OID: " + diagnos.getKlass());
        }
        currentDiagnosis.setCode(diagnos.getKod());
        return currentDiagnosis;
    }

    /**
     * Instantiates and populates a RIV {@link riv.followup.processdevelopment.reimbursement.v1.CareContractType} from the provided source data.
     *
     * @param stateDate The State date to use for looking up Commission codes from the Code Server index.
     * @param careUnitHSAid The care unit HSA id for the current care event.
     * @param commissionState The CommissionState for the current Commission to extract contract information from.
     * @param payerOrganization The payerOrganization to use.
     * @return The populated {@link riv.followup.processdevelopment.reimbursement.v1.CareContractType}
     */
    protected static CareContractType getCareContractFromState(Date stateDate, String careUnitHSAid, TermItemCommission<CommissionState> commissionState, String payerOrganization) {
        CommissionState currentCommissionState = commissionState.getState(stateDate);
        
        // Create the care contract type.
        CareContractType currentContract = of.createCareContractType();

        // Contract Id
        currentContract.setId(of.createIIType());
        currentContract.getId().setRoot(OIDList.getOid(CodeSystem.HYBRID_GUID_IDENTIFIER));
        currentContract.getId().setExtension(SLL_CAREGIVER_HSA_ID + HYBRID_GUI_SEPARATOR + commissionState.getId());
        currentContract.setName(currentCommissionState.getName());

        // Contract type
        TermItem<CommissionTypeState> commissionType = currentCommissionState.getCommissionType();
        currentContract.setContractType(of.createCVType());
        currentContract.getContractType().setCodeSystem(OIDList.getOid(CodeSystem.SLL_CS_UPPDRAGSTYP));
        currentContract.getContractType().setCodeSystemName(OIDList.getName(CodeSystem.SLL_CS_UPPDRAGSTYP));
        currentContract.getContractType().setCode(commissionType.getId());
        CommissionTypeState currentCommissionType = commissionType.getState(stateDate);
        if (currentCommissionType != null) {
            currentContract.getContractType().setDisplayName(currentCommissionType.getName());
        }

        // RequesterOrganization
        currentContract.setRequesterOrganization(SLL_CAREGIVER_HSA_ID);

        // PayerOrganization
        currentContract.setPayerOrganization(payerOrganization);

        // ProviderOrganization
        if (careUnitHSAid != null) {
            currentContract.setProviderOrganization(careUnitHSAid);
        }

        return currentContract;
    }

    /**
     * Creates a PatientType from the information in the incoming {@link se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse.Patient}.
     *
     * @param ersättningPatient the incoming {@link se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse.Patient} to map data from.
     * @return the mapped PatientType.
     */
    protected static PatientType createRivPatientFromErsättningsPatient(ERSMOIndata.Ersättningshändelse.Patient ersättningPatient) {
        final PatientType rivPatient = of.createPatientType();

        // Set Patient Id.
        rivPatient.setId(of.createPersonIdType());

        rivPatient.getId().setId(ersättningPatient.getID());

        if (ersättningPatient.getID().startsWith("99")) {
            // SLL temporary rivPatient identification (reservnummer)
            rivPatient.getId().setType(OIDList.getOid(CodeSystem.RESERVNUMMER_SLL));
        } else if (Integer.valueOf(ersättningPatient.getID().substring(6,8)) > 60) {
            // National co-ordination number (samordningsnummer) - the birth day has 60 added to it in order to identify it.
            rivPatient.getId().setType(OIDList.getOid(CodeSystem.SAMORDNINGSNUMMER));
        } else {
            // Regular person identificator (personnummer)
            rivPatient.getId().setType(OIDList.getOid(CodeSystem.PERSONNUMMER));
        }

        // Patient birth date.
        if (ersättningPatient.getFödelsedatum() != null) {
            rivPatient.setBirthDate(ersättningPatient.getFödelsedatum().toXMLFormat().replace("-", ""));
        }

        // Patient gender.
        rivPatient.setGender(new CVType());
        rivPatient.getGender().setCodeSystem(OIDList.getOid(CodeSystem.KV_KÖN));
        rivPatient.getGender().setCodeSystemName(OIDList.getName(CodeSystem.KV_KÖN));

        if (ersättningPatient.getKön() == null) {
            // Unknown
            rivPatient.getGender().setCode("0");
        } else if (ersättningPatient.getKön().equals(Kon.M)) {
            // Male
            rivPatient.getGender().setCode("2");
        } else if (ersättningPatient.getKön().equals(Kon.K)) {
            // Female
            rivPatient.getGender().setCode("1");
        }


        // Patient residence region.
        String ersättningsLkf = ersättningPatient.getLkf();
        if (ersättningsLkf != null && ersättningsLkf.length() >= 6) {
            rivPatient.setResidence(createRivResidenceFromErsättningLkf(ersättningsLkf));
        }

        // Patient local residence.
        String basområde = ersättningPatient.getBasområde();
        if (basområde != null) {
            rivPatient.setLocalResidence(basområde);
        }

        return rivPatient;
    }

    /**
     * Creates a RIV {@link riv.followup.processdevelopment.reimbursement.v1.ResidenceType} from the incoming string with LKF information from the
     * source Ersättningshändelse.
     *
     * @param ersättningsLkf A string with LKF-information of the formnat "LLKKFF".
     * @return The mapped {@link riv.followup.processdevelopment.reimbursement.v1.ResidenceType}.
     */
    protected static ResidenceType createRivResidenceFromErsättningLkf(String ersättningsLkf) {
        final ResidenceType rivResidence = of.createResidenceType();
        
        // Patient residence region
        rivResidence.setRegion(new CVType());
        rivResidence.getRegion().setCode(ersättningsLkf.substring(0, 2));
        rivResidence.getRegion().setCodeSystem(OIDList.getOid(CodeSystem.KV_LÄN));
        rivResidence.getRegion().setCodeSystemName(OIDList.getName(CodeSystem.KV_LÄN));

        // Patient residence municipality
        rivResidence.setMunicipality(new CVType());
        rivResidence.getMunicipality().setCode(ersättningsLkf.substring(2, 4));
        rivResidence.getMunicipality().setCodeSystem(OIDList.getOid(CodeSystem.KV_KOMMUN));
        rivResidence.getMunicipality().setCodeSystemName(OIDList.getName(CodeSystem.KV_KOMMUN));

        // Patient residence parish
        rivResidence.setParish(new CVType());
        rivResidence.getParish().setCode(ersättningsLkf.substring(4, 6));
        rivResidence.getParish().setCodeSystem(OIDList.getOid(CodeSystem.KV_FÖRSAMLING));
        rivResidence.getParish().setCodeSystemName(OIDList.getName(CodeSystem.KV_FÖRSAMLING));
        
        return rivResidence;
    }

    /**
     * Returns the HSA-id for the payerOrganization for the current commissionState.
     *
     * @param kontaktForm The type of contact, primary care or inpatient care.
     * @param stateDate The date to use for lookup code mapping states.
     * @param currentFacility The currently active Facility.
     * @param commissionState The currently active Commission.
     * @param referredFromHsaId Previously looked up hsa id, may be null. 
     * @param currentFile 
     * @param currentErsId 
     * @param kombika 
     * @return the HSA-id for the payerOrganization for the current commissionState.
     */
    protected static String getPayerOrganization(Vkhform kontaktForm, Date stateDate, FacilityState currentFacility, 
                                                 TermItemCommission<CommissionState> commissionState, String requesterOrgHsa, 
                                                 String referredFromHsaId, String kombika, String currentErsId, Path currentFile) 
    {
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
            if (kontaktForm.equals(Vkhform.ÖPPENVÅRDSKONTAKT) && allowedPrimaryCareUnitTypes.contains(currentPayerFacility.getCareUnitType())) {
                payerOrganization = currentPayerFacility.getHSAMapping().getState(stateDate).getHsaId();
            }

            // Om det är en slutenvårdskontakt vars vårdenhetstyp finns med i allowedInpatientCareUnitTypes, mappa.
            if (kontaktForm.equals(Vkhform.SLUTENVÅRDSTILLFÄLLE) && allowedInpatientCareUnitTypes.contains(currentPayerFacility.getCareUnitType())) {
                payerOrganization = currentPayerFacility.getHSAMapping().getState(stateDate).getHsaId();
            }
        }
        
        if (payerOrganization != null) {
            LOG.warn(String.format("Looked up kombika using strang back reference algorithm, verify this (and remove warning if code is correct)! "
                                   + "kombika %s on care event %s in %s.", kombika, currentErsId, currentFile));
            return payerOrganization;
        }
        
        
        if ("0000".equals(currentFacility.getCustomerCode()) && referredFromHsaId != null) {
            // Fallback alternative way to look up payer.
            //
            // Verify that kundkod for kombika is 0000 (remittenten faktureras).
            // If there is a remittent lookup kombika for that.
            return referredFromHsaId;
        }
        
        return null;
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
    protected static List<FacilityState> getPotentialPayerFacilities(Date stateDate, TermItemCommission<CommissionState> commissionState) {
        List<FacilityState> payerFacilities = new ArrayList<>();
        if (commissionState != null) {
            List<TermItem<FacilityState>> backRefs = commissionState.getBackRefs();
            for (TermItem<FacilityState> currentBackRef : backRefs) {
                FacilityState state = currentBackRef.getState(stateDate);
                // If the current facility has a Customer Code of 9175 "Betalningsansvar samtlig medicinsk service", add it to the map
                // of potential payerFacilities.
                if (state != null && "9175".equals(state.getCustomerCode())) {
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
    protected static String mapErsmoKontaktFormToKvKontakttyp(Vkhform ersmoKontaktForm) {
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
    protected static String mapNumericHändelsetypToTextRepresentation(String händelsetyp) {
        String trimmedInput = händelsetyp.trim();
        if ("013".equals(trimmedInput)) {
            return "BTYP";
        } else if ("014".equals(trimmedInput)) {
            return "VKTYP";
        }

        return "";
    }

    /**
     * Creates a XMLGregorianCalendar with correct time zone and DST information from an
     * incoming Date.
     * @param inDate The date to convert from.
     * @return The converted XMLGregorianCalendar result.
     * @throws se.sll.reimbursementadapter.exception.TransformationException When the calendar object could not be instansiated.
     */
    protected static XMLGregorianCalendar createXMLCalendarFromDate(Date inDate) throws TransformationException {
        XMLGregorianCalendar xmlGregorianCalendar;
        try {
            GregorianCalendar c = new GregorianCalendar();
            c.setTime(inDate);
            xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
            //xmlGregorianCalendar.setTimezone((TimeZone.getDefault().getRawOffset() /*+ TimeZone.getDefault().getDSTSavings()*/) / 1000 / 60);

        } catch (DatatypeConfigurationException e) {
            throw new TransformationException("DatatypeConfigurationException occured when creating a new DatatypeFactory.");
        }
        return xmlGregorianCalendar;
    }

    /**
     * Creates the event type structure (EventTypeMain and EventTypeSub) in the incoming {@link CareEventType}
     * from the incoming {@link ERSMOIndata.Ersättningshändelse} and other helper parameters.
     *
     * @param currentErsh The {@link ERSMOIndata.Ersättningshändelse} to use as a main mapping source.
     * @param careEventType The {@link CareEventType} to create the event type structure in.
     * @param händelseform The form of care event.
     * @param händelsetyp The type of care contact.
     */
    static void createEventTypeStructure(ERSMOIndata.Ersättningshändelse currentErsh, CareEventType careEventType, Vkhform händelseform, String händelsetyp) {
        careEventType.setEventTypeMain(of.createCVType());
        careEventType.getEventTypeMain().setCodeSystem(OIDList.getOid(CodeSystem.KV_KONTAKTTYP));
        careEventType.getEventTypeMain().setCodeSystemName(OIDList.getName(CodeSystem.KV_KONTAKTTYP));
        careEventType.getEventTypeMain().setCode(mapErsmoKontaktFormToKvKontakttyp(händelseform));

        if (händelsetyp != null) {
            careEventType.setEventTypeSub(of.createCVType());
            // Create our locally defined OID with the "SLL.CS." + {getHändelsetyp()}
            String nameOfHandelseTyp = mapNumericHändelsetypToTextRepresentation(händelsetyp);
            careEventType.getEventTypeSub().setCodeSystem("SLL.CS." + nameOfHandelseTyp);
            careEventType.getEventTypeSub().setCodeSystemName("SLL Code Server definition from the '" + händelsetyp + "' table.");
            careEventType.getEventTypeSub().setCode(currentErsh.getHändelseklass().getVårdkontakt().getTyp());
        }
    }
}