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

import java.text.SimpleDateFormat;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import riv.followup.processdevelopment.reimbursement.v1.*;
import riv.followup.processdevelopment.reimbursement.v1.CareContractType;
import riv.followup.processdevelopment.reimbursement.v1.ObjectFactory;
import se.sll.ersmo.xml.indata.*;
import se.sll.ersmo.xml.indata.Diagnoser.Diagnos;
import se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse;
import se.sll.ersmo.xml.indata.Tillståndslista.Tillstånd;
import se.sll.ersmo.xml.indata.Yrkeskategorier.Yrkeskategori;
import se.sll.ersmo.xml.indata.Åtgärder.Åtgärd;
import se.sll.reimbursementadapter.admincareevent.model.CommissionState;
import se.sll.reimbursementadapter.admincareevent.model.FacilityState;
import se.sll.reimbursementadapter.admincareevent.service.CodeServerMEKCacheManagerService;
import se.sll.reimbursementadapter.parser.TermItem;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

/**
 * Transforms a single ERSMOIndata XML object to a number of CareEventType XML objects.
 * Transformation rules implement the "inrapportering_gvr_meddelandeinnehall_0.3"-specification.
 */
public class ERSMOIndataToCareEventTransformer {
	
	private static final Logger LOG = LoggerFactory.getLogger(ERSMOIndataToCareEventTransformer.class);

    /**
     * Transforms a single {@link se.sll.ersmo.xml.indata.ERSMOIndata} object to a list of
     * {@link riv.followup.processdevelopment.reimbursement.v1.CareEventType} objects.
     *
     * @param ersmoIndata The {@link se.sll.ersmo.xml.indata.ERSMOIndata} object to transform from.
     * @param fileUpdatedTime The update time for the file the ERSMOIndata was read from. This is used
     *                        for setting the "lastUpdatedTime" parameter in the transformation, which
     *                        is not available in the source data.
     * @return The transformed list of {@link riv.followup.processdevelopment.reimbursement.v1.CareEventType} objects
     */
	public static List<CareEventType> doTransform(ERSMOIndata ersmoIndata, Date fileUpdatedTime) {
		LOG.debug("Entering ERSMOIndataToCareEventTransformer.doTransform()");
		// Instantiate the Cache Manager.
		CodeServerMEKCacheManagerService cacheManager = CodeServerMEKCacheManagerService.getInstance();
		// Create the response object.
		List<CareEventType> responseList = new ArrayList<>();
		
		// Iterate over all the ersmoIndata.getErsättningshändelse() and convert them to CareEventType.
		for (Ersättningshändelse currentErsh : ersmoIndata.getErsättningshändelse()) {
            CareEventType currentEvent = createCareEventFromErsättningshändelse(currentErsh, ersmoIndata, cacheManager, fileUpdatedTime);
			responseList.add(currentEvent);
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
     * @return The transformed {@link riv.followup.processdevelopment.reimbursement.v1.CareEventType}.
     */
    private static CareEventType createCareEventFromErsättningshändelse(Ersättningshändelse currentErsh, ERSMOIndata ersmoIndata, CodeServerMEKCacheManagerService cacheManager, Date updatedTime) {
        ObjectFactory of = new ObjectFactory();
        CareEventType currentEvent = of.createCareEventType();

        currentEvent.setId(currentErsh.getID());

        // Source System
        currentEvent.setSourceSystem(of.createSourceSystemType());
        currentEvent.getSourceSystem().setOrganization("1.2.752.97.??");
        currentEvent.getSourceSystem().setName(ersmoIndata.getKälla());

        // Patient
        if (currentErsh.getPatient() != null) {
            currentEvent.setPatient(of.createPatientType());
            currentEvent.getPatient().setId(of.createPersonIdType());
            currentEvent.getPatient().getId().setId(currentErsh.getPatient().getID());
            if (currentErsh.getPatient().getID().startsWith("99")) {
                // SLL temporary patient identification
                currentEvent.getPatient().getId().setType("1.2.752.97.3.1.3");
            } else {
                currentEvent.getPatient().getId().setType("1.2.752.129.2.1.3.1");
            } // TODO: Fix the logic and add OID:s for temporary identities.
            if (currentErsh.getPatient().getFödelsedatum() != null) {
                currentEvent.getPatient().setBirthDate(currentErsh.getPatient().getFödelsedatum().toXMLFormat().replace("-", ""));
            }
            if (currentErsh.getPatient().getKön() != null) {
                if (currentErsh.getPatient().getKön().equals(Kon.M)) {
                    currentEvent.getPatient().setGender(GenderType.M);
                } else if (currentErsh.getPatient().getKön().equals(Kon.K)) {
                    currentEvent.getPatient().setGender(GenderType.F);
                }
            }
            currentEvent.getPatient().setResidence(of.createResidenceType());
            if (currentErsh.getPatient().getLkf() != null && currentErsh.getPatient().getLkf().length() >= 6) {
                currentEvent.getPatient().getResidence().setRegion(new CVType());
                currentEvent.getPatient().getResidence().getRegion().setCode(currentErsh.getPatient().getLkf()
                        .substring(0, 2));
                currentEvent.getPatient().getResidence().setMunicipality(new CVType());
                currentEvent.getPatient().getResidence().getMunicipality().setCode(currentErsh.getPatient().getLkf().substring(2, 4));
                currentEvent.getPatient().getResidence().setParish(new CVType());
                currentEvent.getPatient().getResidence().getParish().setCode(currentErsh.getPatient().getLkf().substring(4, 6));
            }

            CVType patientExtras = of.createCVType();
            patientExtras.setOriginalText("Not mapped yet!");

            JAXBElement<CVType> extrasElement = new JAXBElement<>(new QName("urn:riv:followup:processdevelopment:reimbursement:extras:1","Extras"),
                    CVType.class, patientExtras);

            currentEvent.getPatient().getAny().add(extrasElement);
        }

        // Care Unit Local Id
        currentEvent.setCareUnit(of.createCareUnitType());
        currentEvent.getCareUnit().setCareUnitLocalId(new IIType());
        currentEvent.getCareUnit().getCareUnitLocalId().setRoot("1.2.752.129.2.1.2.1");
        currentEvent.getCareUnit().getCareUnitLocalId().setExtension("SLL.OID+" + currentErsh.getSlutverksamhet()); // TODO: SLL OID

        // Use the Startdatum from the Ersättningshändelse as the key for Code mapping lookup.
        Date stateDate = currentErsh.getStartdatum().toGregorianCalendar().getTime();
        TermItem<FacilityState> mappedFacilities = cacheManager.getCurrentIndex().get(currentErsh.getSlutverksamhet());
        if (mappedFacilities != null) {
            // Contract
            currentEvent.setContracts(of.createCareEventTypeContracts());

            for (TermItem<CommissionState> commissionState : mappedFacilities.getState(stateDate).getCommissions()) {
                if ("06".equals(commissionState.getState(stateDate).getAssignmentType())
                        || "07".equals(commissionState.getState(stateDate).getAssignmentType())
                        || "08".equals(commissionState.getState(stateDate).getAssignmentType())) {
                    CareContractType currentContract = of.createCareContractType();

                    currentContract.setId(of.createIIType());
                    currentContract.getId().setRoot("1.2.752.129.2.1.2.1");
                    currentContract.getId().setExtension("SLL.OID+" + commissionState.getId()); // TODO: SLL OID

                    currentContract.setContractType(of.createCVType());
                    currentContract.getContractType().setCodeSystem("SLL.CS.UPPDRAGSTYP");
                    currentContract.getContractType().setCodeSystemName("SLL Code Server definition from the 'UPPDRAGSTYP' table.");
                    currentContract.getContractType().setCode(commissionState.getState(stateDate).getCommissionType().getId());
                    currentContract.getContractType().setDisplayName(commissionState.getState(stateDate).getCommissionType().getState(stateDate).getName());

                    // TODO: Mappa till HSA.
                    currentContract.setProviderOrganization(currentErsh.getSlutverksamhet());

                    currentEvent.getContracts().getContract().add(currentContract);
                }
            }

            // Care Unit HSA-id from MEK
            currentEvent.getCareUnit().setCareUnitId(mappedFacilities.getState(stateDate).getHSAMapping().getState(stateDate).getHsaId());
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
        /*currentEvent.setDatePeriod(of.createDatePeriodType());
        currentEvent.getDatePeriod().setStartDate(currentErsh.getStartdatum().toXMLFormat().replace("-", ""));
        if (currentErsh.getSlutdatum() != null) {
            currentEvent.getDatePeriod().setEndDate(currentErsh.getSlutdatum().toXMLFormat().replace("-", ""));
        }*/

        if (currentErsh.getHändelseklass() != null) {
            if (currentErsh.getHändelseklass().getVårdkontakt() != null) {
                // Emergency
                if (currentErsh.getHändelseklass().getVårdkontakt().getAkut() != null) {
                    currentEvent.setEmergency(currentErsh.getHändelseklass().getVårdkontakt().getAkut().equals("J"));
                }

                // Event Type
                currentEvent.setEventType(of.createEventTypeType());
                if (currentErsh.getHändelseklass().getVårdkontakt().getHändelseform() != null) {
                    currentEvent.getEventType().setMainType(of.createCVType());
                    currentEvent.getEventType().getMainType().setCodeSystem("1.2.752.129.2.2.2.25");
                    currentEvent.getEventType().getMainType().setCodeSystemName("KV kontakttyp");
                    currentEvent.getEventType().getMainType().setCode(mapErsmoKontaktFormToKvKontakttyp(
                            currentErsh.getHändelseklass().getVårdkontakt().getHändelseform().toString()));

                    currentEvent.getEventType().setSubType(of.createCVType());
                    // Create our locally defined OID with the "SLL.CS." + {getHändelsetyp()}
                    currentEvent.getEventType().getSubType().setCodeSystem("SLL.CS." + currentErsh.getHändelseklass().getVårdkontakt().getHändelsetyp());
                    currentEvent.getEventType().getSubType().setCodeSystem("SLL Code Server definition from the '" + currentErsh.getHändelseklass().getVårdkontakt().getHändelsetyp() + "' table.");
                    currentEvent.getEventType().getSubType().setCode(currentErsh.getHändelseklass().getVårdkontakt().getTyp());
                }

                // Fee Category
                currentEvent.setFeeCategory(of.createCVType());
                currentEvent.getFeeCategory().setOriginalText(currentErsh.getHändelseklass().getVårdkontakt().getAvgiftsklass());
                //currentEvent.getFeeCategory().setCode(currentErsh.getHändelseklass().getVårdkontakt().getAvgiftsklass());
                //currentEvent.getFeeCategory().setCodeSystem("????");

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
                            currentDiagnosis.setCodeSystem("1.2.752.116.1.1.1.1.3");
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
                        currentCondition.setCodeSystem("NO.OID: " + tillstånd.getKlass()); // TODO: Convert to OID
                        currentCondition.setCode(tillstånd.getKod());
                        currentEvent.getConditions().getCondition().add(currentCondition);
                    }
                }

                // Activities
                currentEvent.setActivities(of.createCareEventTypeActivities());
                if (currentErsh.getHändelseklass().getVårdkontakt().getÅtgärder() != null && currentErsh.getHändelseklass().getVårdkontakt().getÅtgärder().getÅtgärd() != null && currentErsh.getHändelseklass().getVårdkontakt().getÅtgärder().getÅtgärd().size() > 0) {
                    for (Åtgärd åtgärd : currentErsh.getHändelseklass().getVårdkontakt().getÅtgärder().getÅtgärd()) {
                        ActivityType currentActivity = of.createActivityType();
                        currentActivity.setActivityCode(of.createCVType());
                        if (åtgärd.getKlass().equals("007")) {
                            currentActivity.getActivityCode().setCodeSystem("1.2.752.116.1.3.2.1.4");
                            currentActivity.getActivityCode().setCodeSystemName("Klassifikation av vårdåtgärder (KVÅ)");
                        } else if (åtgärd.getKlass().equals("020")) {
                            currentActivity.getActivityCode().setCodeSystem("1.2.752.129.2.2.3.1.1");
                            currentActivity.getActivityCode().setCodeSystemName("Anatomical Therapeutic Chemical " +
                                    "classification system (ATC)");
                        } else {
                            currentActivity.getActivityCode().setCodeSystem("no.oid: " + åtgärd.getKlass());
                        }
                        currentActivity.getActivityCode().setCode(åtgärd.getKod());
                        // TODO: Mapping
                        currentActivity.setDate("???");
                        currentActivity.setActivityTime("???");
                        currentEvent.getActivities().getActivity().add(currentActivity);
                    }
                }

                // Referred From
                currentEvent.setReferredFrom("1.2.752.97.??:" + currentErsh.getHändelseklass().getVårdkontakt().getRemissFöre().getKod());

                // Stay Before
                currentEvent.setStayBefore(of.createCVType());
                currentEvent.getStayBefore().setCode(currentErsh.getHändelseklass().getVårdkontakt().getVisteFöre().getKod());
                currentEvent.getStayBefore().setCodeSystem("NO.OID: " + currentErsh.getHändelseklass().getVårdkontakt().getVisteFöre().getKod());

                // Stay After
                currentEvent.setStayAfter(of.createCVType());
                currentEvent.getStayAfter().setCode(currentErsh.getHändelseklass().getVårdkontakt().getVisteEfter().getKod());
                currentEvent.getStayAfter().setCodeSystem("NO.OID: " + currentErsh.getHändelseklass().getVårdkontakt().getVisteEfter().getKod());

                // Deceased
                //currentEvent.setDeceased(true); TODO: Mapping??
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

        if ("öppenvårdskontakt".equals(trimmedInput)) {
            return "2";
        } else if ("slutenvårdstillfälle".equals(trimmedInput)) {
            return "1";
        } else if ("hemsjukvårdskontakt".equals(trimmedInput)) {
            return "4";
        }

        return "";
    }

}

