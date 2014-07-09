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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import riv.followup.processdevelopment.reimbursement.v1.CareContractType;
import riv.followup.processdevelopment.reimbursement.v1.CareEventType;
import riv.followup.processdevelopment.reimbursement.v1.ObjectFactory;
import se.sll.ersmo.xml.indata.ERSMOIndata;
import se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse;
import se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse.Patient;
import se.sll.ersmo.xml.indata.RemissFöre;
import se.sll.ersmo.xml.indata.VisteEfter;
import se.sll.ersmo.xml.indata.VisteFöre;
import se.sll.ersmo.xml.indata.Vkhform;
import se.sll.ersmo.xml.indata.Vårdkontakt;
import se.sll.ersmo.xml.indata.Åtgärder;
import se.sll.reimbursementadapter.admincareevent.model.CommissionState;
import se.sll.reimbursementadapter.admincareevent.model.FacilityState;
import se.sll.reimbursementadapter.admincareevent.model.HSAMappingState;
import se.sll.reimbursementadapter.admincareevent.model.TermItemCommission;
import se.sll.reimbursementadapter.admincareevent.service.CodeServerMEKCacheManagerService;
import se.sll.reimbursementadapter.exception.TransformationException;
import se.sll.reimbursementadapter.parser.TermItem;

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
        int numberOfFilteredCareEvents = 0;
        for (Ersättningshändelse currentErsh : list) {
            if (currentErsh.getHändelseklass().getVårdkontakt() != null) {
                CareEventType currentEvent = createCareEventFromErsättningshändelse(currentErsh, ersmoIndata, cacheManager, fileUpdatedTime, currentFile);
                if (currentEvent != null) {
                    responseList.add(currentEvent);
                } else {
                    numberOfFilteredCareEvents++;
                }
            } else {
                numberOfFilteredCareEvents++;
            }
        }
        LOG.info(String.format("%d/%d care events filtered away.", numberOfFilteredCareEvents, list.size()));
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

            Vårdkontakt vårdkontakt = currentErsh.getHändelseklass().getVårdkontakt();

            String kombika = currentErsh.getSlutverksamhet();
            
            TermItem<FacilityState> avd = cacheManager.getCurrentIndex().get(kombika);
            if (avd == null) {
                // #214 We need to handle this somehow, not skip like now, like rereading later?
                LOG.warn(String.format("Coult not look up facilities (AVD) for kombika %s on care event %s in %s, skipping, fix in #214?",
                                        kombika, currentErsId, currentFile));
                return null;
            }
            
            //
            // Find a good date to use as state date. 
            //
            
            Date stateDate = null;
            FacilityState currentAvd = null;
            
            // Start using the Startdatum from the Ersättningshändelse. 
            stateDate = getLookupDate(currentErsh.getStartdatum());
            
            currentAvd = avd.getState(stateDate);
            
            if (currentAvd == null) {
                // Try to use the dates in activities.
                Åtgärder åtgärder = currentErsh.getHändelseklass().getVårdkontakt().getÅtgärder();
                if (åtgärder != null) {
                    for (Åtgärder.Åtgärd åtgärd : åtgärder.getÅtgärd()) {
                        stateDate = getLookupDate(åtgärd.getDatum());
                        currentAvd = avd.getState(stateDate);
                        if (currentAvd != null) break;
                    }
                }
            }
            
            if (currentAvd == null && currentErsh.getSlutdatum() != null) {
                // Try with Slutdatum from the Ersättningshändelse.
                stateDate = getLookupDate(currentErsh.getSlutdatum());
                currentAvd = avd.getState(stateDate);
            }
            
            if (currentAvd == null) {
                LOG.debug(String.format("Did not find code server data for kombika %s and date %s on care event %s in %s, skipping.",
                                        kombika, stateDate, currentErsId, currentFile));
                return null;
            }
        
            //
            // Translate all the data.
            // 
            
            // Patient            
            Patient patient = currentErsh.getPatient();
            if (patient != null) {
                currentEvent.setPatient(TransformHelper.createRivPatientFromErsättningsPatient(patient));
            }

            // Care Unit
            String careUnitHSAid = TransformHelper.createCareUnitStructure(
                    currentErsh, currentFile, currentEvent, currentErsId, stateDate, currentAvd);

            // Set up mapping for the contact referral care unit to HSA-id.
            String referredFromHsaId = null;
            RemissFöre referralBefore = vårdkontakt.getRemissFöre();
            if (referralBefore != null) {
                TermItem<FacilityState> mappedFacilitiesForReferral = cacheManager.getCurrentIndex().get(referralBefore.getKod());

                if (mappedFacilitiesForReferral != null) {
                    
                    FacilityState facilityState = mappedFacilitiesForReferral.getState(stateDate);
                    if (facilityState != null) {
                        TermItem<HSAMappingState> hsaMapping = facilityState.getHSAMapping();
                        if (hsaMapping != null) {
                            HSAMappingState hsaMappingState = hsaMapping.getState(stateDate);
                            if (hsaMappingState != null) {
                                referredFromHsaId = hsaMappingState.getHsaId();
                            }
                        }
                    }
                    
                    if (referredFromHsaId == null) {
                        // Try with another state date for the referral kombika. The kombika may have expired but was valid at the time of the referral.
                        facilityState = mappedFacilitiesForReferral.getStateBefore(stateDate);
                        if (facilityState != null) {
                            TermItem<HSAMappingState> hsaMapping = facilityState.getHSAMapping();
                            if (hsaMapping != null) {
                                HSAMappingState hsaMappingState = hsaMapping.getStateBefore(stateDate);
                                if (hsaMappingState != null) {
                                    referredFromHsaId = hsaMappingState.getHsaId();
                                    LOG.debug(String.format("Looked up referral hsa id by using getStateBefore for referral kombika %s and original state date %s on care event %s in %s.",
                                                            referralBefore.getKod(), stateDate, currentErsId, currentFile));
                                }
                            }
                        }
                    }
                    
                    if (referredFromHsaId != null) {
                        currentEvent.setReferredFrom(referredFromHsaId);
                    }
                }
            }
            
            // Contract
            currentEvent.setContracts(of.createCareEventTypeContracts());
            
            // Find the contract mapping with the right type
            Vkhform händelseform = vårdkontakt.getHändelseform();
            if (currentAvd != null) {
                // Loop over commissions (SAMVERKS).
                for (TermItemCommission<CommissionState> commissionState : currentAvd.getCommissions()) {
                    
                    if (commissionState == null) {
                        continue;
                    }
                    CommissionState currentCommissionState = commissionState.getState(stateDate);
                    if (currentCommissionState == null) {
                        continue;
                    } 
                    
                    if (!currentCommissionState.getFollowsTemplate()) {
                        // This care event uses a test SAMVERKS, log and skip it.
                        LOG.debug(String.format("Care event using test SAMVERKS, kombika %s, date %s, care event %s, file %s, skipping.",
                                               kombika, stateDate, currentErsId, currentFile));
                        return null;
                    }
                    
                    String assignmentType = currentCommissionState.getAssignmentType();
                    if ("06".equals(assignmentType) || "07".equals(assignmentType) || "08".equals(assignmentType)) {
                        // Lookup the payer organization. Extracted from getCareContractFromState due to number of parameters.
                        String payerOrganization = TransformHelper.getPayerOrganization(händelseform, stateDate, avd.getState(stateDate),
                                commissionState, TransformHelper.SLL_CAREGIVER_HSA_ID, referredFromHsaId,
                                kombika, currentErsId, currentFile);

                        // Map the current commission information to a CareContractType and add it to the currentEvent list.
                        CareContractType currentContract = TransformHelper.getCareContractFromState(
                                stateDate, careUnitHSAid, commissionState, payerOrganization);
                        currentEvent.getContracts().getContract().add(currentContract);
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
            if (vårdkontakt.getAkut() != null) {
                currentEvent.setEmergency(vårdkontakt.getAkut().equals("J"));
            }

            // Event Type
            if (händelseform != null) {
                String händelsetyp = vårdkontakt.getHändelsetyp();
                TransformHelper.createEventTypeStructure(currentErsh, currentEvent, händelseform, händelsetyp);
            }

            // Fee Category
            String avgiftsklass = vårdkontakt.getAvgiftsklass();
            if (avgiftsklass == null || "006".equals(avgiftsklass)) {
                currentEvent.setFeeCategory(of.createCVType());
                currentEvent.getFeeCategory().setCodeSystem("SLL.CS.TAXA");
                currentEvent.getFeeCategory().setCode(vårdkontakt.getPatientavgift());
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
            VisteFöre stayBefore = vårdkontakt.getVisteFöre();
            if (stayBefore != null) {
                currentEvent.setStayBefore(of.createCVType());
                currentEvent.getStayBefore().setCode(stayBefore.getKod());
                currentEvent.getStayBefore().setCodeSystem("SLL.CS.IKOD");
            }

            // Stay After
            VisteEfter stayAfter = vårdkontakt.getVisteEfter();
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
            throw new TransformationException(String.format("Transformation of care event %s in %s failed fatally: %s",
                    currentErsId, currentFile, e.getMessage()), e);
        }
        
        return currentEvent;
    }

    private static Date getLookupDate(XMLGregorianCalendar xmlCalendar) 
    {
        if (xmlCalendar == null) return null;
        return getLookupDate(xmlCalendar.toGregorianCalendar());
    }

    private static Date getLookupDate(GregorianCalendar calendar) 
    {
        if (calendar == null) return null;
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        return calendar.getTime();
    }
    
    private static void fatal(String msg) throws TransformationException 
    {
        ERSMOIndataToCareEventTransformer.LOG.error(msg);
        throw new TransformationException(msg);
    }

}
