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
import riv.followup.processdevelopment.reimbursement.v1.ObjectFactory;
import se.sll.ersmo.xml.indata.*;
import se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse;
import se.sll.reimbursementadapter.admincareevent.model.CommissionState;
import se.sll.reimbursementadapter.admincareevent.model.FacilityState;
import se.sll.reimbursementadapter.admincareevent.model.HSAMappingState;
import se.sll.reimbursementadapter.admincareevent.model.TermItemCommission;
import se.sll.reimbursementadapter.admincareevent.service.CodeServerMEKCacheManagerService;
import se.sll.reimbursementadapter.exception.TransformationException;
import se.sll.reimbursementadapter.parser.TermItem;

import java.nio.file.Path;
import java.util.*;

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
     * @param currentFile The current file that the ersmoIndata originates from. Used for logging.
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
     *                     to use for looking up additional information not available in the source data.
     * @param updatedTime The update time for the file the ERSMOIndata was read from. This is used
     *                    for setting the "lastUpdatedTime" parameter in the transformation, which
     *                    is not available in the source data.
     * @param currentFile The current file that the ersmoIndata originates from. Used for logging.
     * @return The transformed {@link riv.followup.processdevelopment.reimbursement.v1.CareEventType}.
     */
    static CareEventType createCareEventFromErsättningshändelse(ERSMOIndata.Ersättningshändelse currentErsh,
                                                                ERSMOIndata ersmoIndata,
                                                                CodeServerMEKCacheManagerService cacheManager,
                                                                Date updatedTime, Path currentFile) throws TransformationException {
        ObjectFactory of = new ObjectFactory();
        CareEventType currentEvent = of.createCareEventType();

        currentEvent.setId(currentErsh.getID());

        String currentErsId = currentErsh.getID();

        try {
            // Source System
            TransformHelper.createSourceSystemStructure(ersmoIndata, currentEvent);

            // If the Händelseklass is null och the Händelseklass is not of type "Vårdkontakt", throw a mapping exception.
            if (currentErsh.getHändelseklass() == null || currentErsh.getHändelseklass().getVårdkontakt() == null) {
                fatal(String.format("Could not find any Händelseklass/Vårdkontakt in care event %s in %s.", currentErsId, currentFile));
            }

            // Use the Startdatum from the Ersättningshändelse as the key for Code mapping lookup.
            Date stateDate = currentErsh.getStartdatum().toGregorianCalendar().getTime();
            String kombika = currentErsh.getSlutverksamhet();
            TermItem<FacilityState> mappedFacilities = cacheManager.getCurrentIndex().get(kombika);
            if (mappedFacilities == null) {
                fatal(String.format("Did not find code server data for kombika %s on care event %s in %s.",
                                    kombika, currentErsId, currentFile));
            }

            if (mappedFacilities.getState(stateDate) == null) {
                fatal(String.format("Did not find code server data for kombika %s and date %s on care event %s in %s.",
                                    kombika, stateDate, currentErsId, currentFile));
            }

            FacilityState mappedFacility = mappedFacilities.getState(stateDate);
            if (mappedFacility == null) {
                fatal(String.format("Did not find code server data for kombika %s for date %s on care event %s in %s.",
                                    kombika, stateDate, currentErsId, currentFile));
            }

            // Patient            

            if (currentErsh.getPatient() != null) {
                currentEvent.setPatient(TransformHelper.createRivPatientFromErsättningsPatient(currentErsh.getPatient()));
            }

            // Care Unit
            String careUnitHSAid = TransformHelper.createCareUnitStructure(currentErsh, currentFile, currentEvent, currentErsId, stateDate, mappedFacility);

            // Contract
            currentEvent.setContracts(of.createCareEventTypeContracts());

            // Find the contract mapping with the right type
            Vkhform händelseform = currentErsh.getHändelseklass().getVårdkontakt().getHändelseform();
            for (TermItemCommission<CommissionState> commissionState : mappedFacilities.getState(stateDate).getCommissions()) {

                // TODO roos Is this really a good idea? Got null pointer for care event with kombika 19137011000 at 2013-03-01  before.
                if (commissionState == null) {
                    continue;
                }
                CommissionState filteredCommissionState = commissionState.getState(stateDate);
                if (filteredCommissionState == null) {
                    continue;
                }

                String assignmentType = filteredCommissionState.getAssignmentType();
                if ("06".equals(assignmentType) || "07".equals(assignmentType) || "08".equals(assignmentType)) {
                    // Lookup the payer organisation. Extracted from getCareContractFromState due to number of parameters.
                    String payerOrganization = TransformHelper.getPayerOrganization(händelseform, stateDate, mappedFacilities.getState(stateDate), commissionState, TransformHelper.SLL_CAREGIVER_HSA_ID);

                    // Map the current commission information to a CareContractType and add it to the currentEvent list.
                    CareContractType currentContract = TransformHelper.getCareContractFromState(stateDate, careUnitHSAid, commissionState, payerOrganization);
                    currentEvent.getContracts().getContract().add(currentContract);
                }
            }

            // Set up mapping for the contact referral care unit to HSA-id.
            RemissFöre referralBefore = currentErsh.getHändelseklass().getVårdkontakt().getRemissFöre();
            if (referralBefore != null) {
                TermItem<FacilityState> mappedFacilitiesForReferral = cacheManager.getCurrentIndex().get(referralBefore.getKod());

                if (mappedFacilitiesForReferral != null) {
                    // TODO roos stateDate is the wrong date to use. The referral kombika may have expired but was valid at the time of the referral.
                    FacilityState facilityState = mappedFacilitiesForReferral.getState(stateDate);
                    if (facilityState != null) {
                        HSAMappingState hsaMappingState = facilityState.getHSAMapping().getState(stateDate);
                        if (hsaMappingState != null) {
                            // Referred From (HSA-id)
                            currentEvent.setReferredFrom(hsaMappingState.getHsaId());
                        }
                    }
                }
            }

            // Last updated time
            currentEvent.setLastUpdatedTime(TransformHelper.createXMLCalendarFromDate(updatedTime));

            // Deleted
            currentEvent.setDeleted(currentErsh.isMakulerad());

            // Date Period
            currentEvent.setDatePeriod(of.createDatePeriodType());
            currentEvent.getDatePeriod().setStart(currentErsh.getStartdatum().toXMLFormat().replace("-", ""));
            if (currentErsh.getSlutdatum() != null) {
                currentEvent.getDatePeriod().setEnd(currentErsh.getSlutdatum().toXMLFormat().replace("-", ""));
            }

            // Emergency
            if (currentErsh.getHändelseklass().getVårdkontakt().getAkut() != null) {
                currentEvent.setEmergency(currentErsh.getHändelseklass().getVårdkontakt().getAkut().equals("J"));
            }

            // Event Type
            if (händelseform != null) {
                String händelsetyp = currentErsh.getHändelseklass().getVårdkontakt().getHändelsetyp();
                TransformHelper.createEventTypeStructure(currentErsh, currentEvent, händelseform, händelsetyp);
            }

            // Fee Category
            String avgiftsklass = currentErsh.getHändelseklass().getVårdkontakt().getAvgiftsklass();
            if (avgiftsklass == null || "006".equals(avgiftsklass)) {
                currentEvent.setFeeCategory(of.createCVType());
                currentEvent.getFeeCategory().setCodeSystem("SLL.CS.TAXA");
                currentEvent.getFeeCategory().setCode(currentErsh.getHändelseklass().getVårdkontakt().getPatientavgift());
            }
            else {
                fatal(String.format("Found unexpected Avgiftsklass %s on care event %s in %s.", avgiftsklass, currentErsId, currentFile));
            }

            // Involved Professions
            TransformHelper.createInvolvedProfessionsStructure(currentErsh, currentEvent);

            // Diagnoses
            TransformHelper.createDiagnosisStructure(currentErsh, currentEvent);

            // Conditions
            TransformHelper.createConditionStructure(currentErsh, currentEvent);

            // Activities
            TransformHelper.createActivityStructure(currentErsh, currentEvent);

            // Stay Before
            VisteFöre stayBefore = currentErsh.getHändelseklass().getVårdkontakt().getVisteFöre();
            if (stayBefore != null) {
                currentEvent.setStayBefore(of.createCVType());
                currentEvent.getStayBefore().setCode(stayBefore.getKod());
                currentEvent.getStayBefore().setCodeSystem("SLL.CS.IKOD");
            }

            // Stay After
            VisteEfter stayAfter = currentErsh.getHändelseklass().getVårdkontakt().getVisteEfter();
            if (stayAfter != null) {
                currentEvent.setStayAfter(of.createCVType());
                currentEvent.getStayAfter().setCode(stayAfter.getKod());
                currentEvent.getStayAfter().setCodeSystem("SLL.CS.UKOD");
            }

            // Deceased
            currentEvent.setDeceased(stayAfter != null && "7".equals(stayAfter.getKod()));

        }
        catch (Exception e) {
            if (e instanceof TransformationException) {
                throw e;
            }
            throw new TransformationException(String.format("Transformation of care event %s in %s failed fatally: %s", currentErsId, currentFile, e.getMessage()), e);
        }
        
        return currentEvent;
    }

    private static void fatal(String msg) throws TransformationException 
    {
        ERSMOIndataToCareEventTransformer.LOG.error(msg);
        throw new TransformationException(msg);
    }

}