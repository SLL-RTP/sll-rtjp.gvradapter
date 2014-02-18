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
package se.sll.gvradapter.gvr.transform;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import riv.followup.processdevelopment.reimbursement.v1.CVType;
import riv.followup.processdevelopment.reimbursement.v1.CareEventType;
import riv.followup.processdevelopment.reimbursement.v1.ContractType;
import riv.followup.processdevelopment.reimbursement.v1.GenderType;
import riv.followup.processdevelopment.reimbursement.v1.ObjectFactory;
import se.sll.ersmo.xml.indata.*;
import se.sll.ersmo.xml.indata.Diagnoser.Diagnos;
import se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse;
import se.sll.ersmo.xml.indata.Tillståndslista.Tillstånd;
import se.sll.ersmo.xml.indata.Yrkeskategorier.Yrkeskategori;
import se.sll.ersmo.xml.indata.Åtgärder.Åtgärd;
import se.sll.gvradapter.admincareevent.model.CommissionState;
import se.sll.gvradapter.admincareevent.model.FacilityState;
import se.sll.gvradapter.admincareevent.service.CodeServerMEKCacheManagerService;
import se.sll.gvradapter.parser.TermItem;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

/**
 * Transforms a single ERSMOIndata XML object to a number of CareEventType XML objects.
 * Transformation rules implement the "inrapportering_gvr_meddelandeinnehall_0.3"-specification.
 */
public class ERSMOIndataToReimbursementEventTransformer {
	
	private static final Logger log = LoggerFactory.getLogger(ERSMOIndataToReimbursementEventTransformer.class);

	public static List<CareEventType> doTransform(ERSMOIndata ersmoIndata, Date fileUpdatedTime) {
		log.debug("Entering ERSMOIndataToReimbursementEventTransformer.doTransform()");
		// Instantiate the Cache Manager.
		CodeServerMEKCacheManagerService cacheManager = CodeServerMEKCacheManagerService.getInstance();
		// Create the response object.
		List<CareEventType> responseList = new ArrayList<CareEventType>();
		
		// Iterate over all the ersmoIndata.getErsättningshändelse() and convert them to CareEventType.
		for (Ersättningshändelse currentErsh : ersmoIndata.getErsättningshändelse()) {
            CareEventType currentEvent = createCareEventFromErsättningshändelse(currentErsh, ersmoIndata, cacheManager, fileUpdatedTime);
			responseList.add(currentEvent);
		}

		return responseList;
	}

    private static CareEventType createCareEventFromErsättningshändelse(Ersättningshändelse currentErsh, ERSMOIndata ersmoIndata, CodeServerMEKCacheManagerService cacheManager, Date updatedTime) {
        ObjectFactory of = new ObjectFactory();
        CareEventType currentEvent = of.createCareEventType();

        currentEvent.setId(currentErsh.getID());

        // Source System
        currentEvent.setSourceSystem(of.createSourceType());
        currentEvent.getSourceSystem().setOrganization("1.2.752.97.??");
        currentEvent.getSourceSystem().setName(ersmoIndata.getKälla());

        // Patient
        if (currentErsh.getPatient() != null) {
            currentEvent.setPatient(of.createPatientType());
            currentEvent.getPatient().setId(of.createPersonIdType());
            currentEvent.getPatient().getId().setId(currentErsh.getPatient().getID());
            if (currentErsh.getPatient().getID().startsWith("99")) {
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
                } /*else if (currentErsh.getPatient().getKön().equals("X")) {
                    currentEvent.getPatient().setGender(GenderType.U);
                }*/
            }
            currentEvent.getPatient().setResidency(of.createPatientTypeResidency());
            if (currentErsh.getPatient().getLkf() != null && currentErsh.getPatient().getLkf().length() >= 6) {
                currentEvent.getPatient().getResidency().setRegion(currentErsh.getPatient().getLkf().substring(0, 2));
                currentEvent.getPatient().getResidency().setMunicipality(currentErsh.getPatient().getLkf().substring(2, 4));
                currentEvent.getPatient().getResidency().setParish(currentErsh.getPatient().getLkf().substring(4, 6));
            }

            CVType patientExtras = of.createCVType();
            patientExtras.setOriginalText("Not mapped yet!");

            JAXBElement<CVType> test2 = new JAXBElement<CVType>(new QName("urn:riv:followup:processdevelopment:1","Extras"),
                    CVType.class, patientExtras);

            currentEvent.getPatient().getAny().add(test2);
        }

        // TODO: Use the date for the reimbursement event instead?
        Date stateDate = new Date();
        TermItem<FacilityState> mappedFacilities = cacheManager.getCurrentIndex().get(currentErsh.getSlutverksamhet());
        if (mappedFacilities != null) {
            // Contract
            currentEvent.setContracts(of.createCareEventTypeContracts());

            for (TermItem<CommissionState> commissionState : mappedFacilities.getState(stateDate).getCommissions()) {
                if ("06".equals(commissionState.getState(stateDate).getAssignmentType())
                        || "07".equals(commissionState.getState(stateDate).getAssignmentType())
                        || "08".equals(commissionState.getState(stateDate).getAssignmentType())) {
                    ContractType currentContract = of.createContractType();

                    currentContract.setId(of.createIIType());
                    currentContract.getId().setRoot("no.oid");
                    currentContract.getId().setExtension(commissionState.getId());

                    currentContract.setContractType(of.createCVType());
                    currentContract.getContractType().setOriginalText(commissionState.getState(stateDate).getCommissionType().getId());
                    currentContract.getContractType().setDisplayName(commissionState.getState(stateDate).getCommissionType().getState(stateDate).getName());

                    currentContract.setProviderOrganization(currentErsh.getSlutverksamhet());

                    currentEvent.getContracts().getContract().add(currentContract);
                }
            }

            // Care Unit HSA-id
            currentEvent.setCareUnit(of.createCareUnitType());
            currentEvent.getCareUnit().setCareUnitHsaId(mappedFacilities.getState(new Date()).getHSAMapping().getState(new Date()).getHsaId());

            // Care Unit Local Id
            currentEvent.getCareUnit().setCareUnitLocalId(of.createCVType());
            currentEvent.getCareUnit().getCareUnitLocalId().setCode(currentErsh.getSlutverksamhet());
            currentEvent.getCareUnit().getCareUnitLocalId().setCodeSystem("no.oid (KOMBIKA)");
            /*CVType careUnitExtras = of.createCVType();
            careUnitExtras.setCode(currentErsh.getSlutverksamhet());
            careUnitExtras.setOriginalText("Not properly mapped yet!");

            JAXBElement<CVType> test2 = new JAXBElement<CVType>(new QName("urn:riv:followup:processdevelopment:1","CareUnitLocalId"),
                    CVType.class, careUnitExtras);
            currentEvent.getCareUnit().getAny().add(test2);*/
        }

        // Last updated time
        currentEvent.setLastUpdatedTime((new SimpleDateFormat("yyyyMMddHHmmssSSS")).format(updatedTime));

        // Deleted
        currentEvent.setDeleted(currentErsh.isMakulerad());

        // Date Period
        currentEvent.setDatePeriod(of.createSplitDatePeriodType());
        currentEvent.getDatePeriod().setStartDate(currentErsh.getStartdatum().toXMLFormat().replace("-", ""));
        if (currentErsh.getSlutdatum() != null) {
            currentEvent.getDatePeriod().setEndDate(currentErsh.getSlutdatum().toXMLFormat().replace("-", ""));
        }

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
                    currentEvent.getEventType().getMainType().setCodeSystem("no.oid");
                    currentEvent.getEventType().getMainType().setOriginalText(currentErsh.getHändelseklass().getVårdkontakt().getHändelseform().toString());

                    currentEvent.getEventType().setSubType(of.createCVType());
                    currentEvent.getEventType().getSubType().setCodeSystem("no.oid");
                    currentEvent.getEventType().getSubType().setOriginalText(currentErsh.getHändelseklass().getVårdkontakt().getTyp() + "+" + currentErsh.getHändelseklass().getVårdkontakt().getHändelsetyp());
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
                        CVType currentProfession = of.createCVType();
                        currentProfession.setCodeSystem("NO.OID: " + kategori.getKlass()); // TODO: Convert to OID
                        currentProfession.setCode(kategori.getKod());
                        currentEvent.getInvolvedProfessions().getProfession().add(currentProfession);
                    }
                }

                // Diagnoses
                currentEvent.setDiagnoses(of.createCareEventTypeDiagnoses());
                if (currentErsh.getHändelseklass().getVårdkontakt().getDiagnoser() != null && currentErsh.getHändelseklass().getVårdkontakt().getDiagnoser().getDiagnos() != null && currentErsh.getHändelseklass().getVårdkontakt().getDiagnoser().getDiagnos().size() > 0) {
                    for (Diagnos diagnos : currentErsh.getHändelseklass().getVårdkontakt().getDiagnoser().getDiagnos()) {
                        CVType currentDiagnosis = of.createCVType();
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
                        CVType currentCondition = of.createCVType();
                        currentCondition.setCodeSystem("NO.OID: " + tillstånd.getKlass()); // TODO: Convert to OID
                        currentCondition.setCode(tillstånd.getKod());
                        currentEvent.getConditions().getCondition().add(currentCondition);
                    }
                }

                // Activities
                currentEvent.setActivities(of.createCareEventTypeActivities());
                if (currentErsh.getHändelseklass().getVårdkontakt().getÅtgärder() != null && currentErsh.getHändelseklass().getVårdkontakt().getÅtgärder().getÅtgärd() != null && currentErsh.getHändelseklass().getVårdkontakt().getÅtgärder().getÅtgärd().size() > 0) {
                    for (Åtgärd åtgärd : currentErsh.getHändelseklass().getVårdkontakt().getÅtgärder().getÅtgärd()) {
                        CVType currentActivity = of.createCVType();
                        if (åtgärd.getKlass().equals("007")) {
                            currentActivity.setCodeSystem("1.2.752.116.1.3.2.1.4");
                            currentActivity.setCodeSystemName("Klassifikation av vårdåtgärder (KVÅ)");
                        } else if (åtgärd.getKlass().equals("020")) {
                            currentActivity.setCodeSystem("1.2.752.129.2.2.3.1.1");
                            currentActivity.setCodeSystemName("Anatomical Therapeutic Chemical classification system (ATC)");
                        } else {
                            currentActivity.setCodeSystem("no.oid: " + åtgärd.getKlass());
                        }
                        currentActivity.setCode(åtgärd.getKod());
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

}

