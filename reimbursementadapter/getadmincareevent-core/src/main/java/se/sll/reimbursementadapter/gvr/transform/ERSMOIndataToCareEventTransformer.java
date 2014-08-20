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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
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
import se.sll.reimbursementadapter.gvr.RetryBin;
import se.sll.reimbursementadapter.parser.TermItem;

/**
 * Transforms a single ERSMOIndata XML object to a number of CareEventType XML objects.
 * Transformation rules implement the "inrapportering_gvr_meddelandeinnehall_0.3"-specification.
 */
public class ERSMOIndataToCareEventTransformer {
    
    private static final Logger LOG = LoggerFactory.getLogger(ERSMOIndataToCareEventTransformer.class);

    private enum Status {
        LOOKUP_FAIL,
        TEST_SAMVERKS,
        OK
    }
    
    /**
     * Transforms a list of {@link se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse} (taken from
     * a single {@link se.sll.ersmo.xml.indata.ERSMOIndata} object) to a list of
     * {@link riv.followup.processdevelopment.reimbursement.v1.CareEventType} objects.
     *
     * @param retryBin The retry bin to add or remove Ersättningshändelse objects from.
     * @param addLookupFails If true lookup fails are added to the response and retry bin. When processing the retry bin, this will be set to false. 
     * @param responseList The list to add transformed Ersättningshändelse objects to.
     * @param sourceList The list of {@link se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse} to be transformed.
     * @param fileUpdatedTime The update time for the file the ERSMOIndata was read from. This is used
     *                        for setting the "lastUpdatedTime" parameter in the transformation, which
     *                        is not available in the source data.
     * @param currentFile The current file that the ERSMOIndata originates from. Used for logging.
     * @throws TransformationException              
     * @throws DatatypeConfigurationException
     */
    public static void doTransform(RetryBin retryBin, Boolean addLookupFails, List<CareEventType> responseList, 
                                   List<Ersättningshändelse> sourceList, Date fileUpdatedTime, Path currentFile) 
            throws TransformationException, DatatypeConfigurationException 
    {
        ObjectFactory of = new ObjectFactory();

        // Instantiate the Cache Manager.
        CodeServerMEKCacheManagerService cacheManager = CodeServerMEKCacheManagerService.getInstance();
        
        LOG.info(String.format("Transforming file %s with %d care events updated at %s.", currentFile, sourceList.size(), fileUpdatedTime));

        int lookupFailCount = 0;
        int testSamverksCount = 0;
        int okCount = 0;

        for (Ersättningshändelse ersh : sourceList) {
            if (ersh.getHändelseklass().getVårdkontakt() != null) {
                
                CareEventType careEvent = of.createCareEventType();
                Status status = populateCareEventFromErsättningshändelse(of, careEvent, ersh, cacheManager, fileUpdatedTime, currentFile);
                
                switch (status) {
                case LOOKUP_FAIL:
                    ++lookupFailCount;
                    if (addLookupFails) {
                        retryBin.put(ersh, fileUpdatedTime); 
                        responseList.add(careEvent);
                    }
                    break;
                case TEST_SAMVERKS:
                    ++testSamverksCount;
                    break;
                case OK:
                    ++okCount;
                    retryBin.remove(ersh.getID());
                    responseList.add(careEvent);
                    break;
                default:
                    fatal("This is a bug.");
                }
                
            }
        }
        
        LOG.info(String.format("Transform finished %d lookup fails (%sadded), %d test samverks skipped, %d was ok.", lookupFailCount, addLookupFails ? "" : "not ", testSamverksCount, okCount));
    }
    
    /**
     * <p>Transforms a single source {@link se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse} into a single, complete
     * {@link riv.followup.processdevelopment.reimbursement.v1.CareEventType} object.</p>
     *
     * <p>To handle the lookup of local care contract data as well as transformation from the local care unit
     * id format (KOMBIKA) to the national HSA format, the stored cache structure in
     * {@link se.sll.reimbursementadapter.admincareevent.service.CodeServerMEKCacheManagerService} is used.</p>
     * @param of 
     * @param careEvent The {@link riv.followup.processdevelopment.reimbursement.v1.CareEventType} to populate.
     *
     * @param ersh The {@link se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse} to transform from.
     * @param cacheManager The {@link se.sll.reimbursementadapter.admincareevent.service.CodeServerMEKCacheManagerService}
     *                     to use for looking up additional information not available in the source data.
     * @param updatedTime The update time for the file the ERSMOIndata was read from. This is used
     *                    for setting the "lastUpdatedTime" parameter in the transformation, which
     *                    is not available in the source data.
     * @param currentFile The current file that the ersmoIndata originates from. Used for logging.
     * @return The status of the transformation.
     * @throws TransformationException
     */
    static Status populateCareEventFromErsättningshändelse(ObjectFactory of, CareEventType careEvent, ERSMOIndata.Ersättningshändelse ersh,
                                                           CodeServerMEKCacheManagerService cacheManager,
                                                           Date updatedTime, Path currentFile) throws TransformationException 
    {
        Status status = Status.OK;
        
        String ershId = ersh.getID();
        
        careEvent.setId(ershId);

        try {
            // Source System
            TransformHelper.createSourceSystemStructure(careEvent);

            Vårdkontakt vårdkontakt = ersh.getHändelseklass().getVårdkontakt();

            String kombika = ersh.getSlutverksamhet();
            
            TermItem<FacilityState> avd = cacheManager.getCurrentIndex().get(kombika);
            if (avd == null) {
            	// TODO: Why not keep the logging here? But use debug or info instead of warn.
                status = Status.LOOKUP_FAIL;
            }
            
            //
            // Find a good date to use as state date. 
            //
            
            Date stateDate = null;
            FacilityState currentAvd = null;
            
            // Start using the Startdatum from the Ersättningshändelse. 
            stateDate = getLookupDate(ersh.getStartdatum());
            
            if (avd != null) {
                currentAvd = avd.getState(stateDate);
            }
            
            if (avd != null && currentAvd == null) {
                // Try to use the dates in activities.
                Åtgärder åtgärder = ersh.getHändelseklass().getVårdkontakt().getÅtgärder();
                if (åtgärder != null) {
                    for (Åtgärder.Åtgärd åtgärd : åtgärder.getÅtgärd()) {
                        stateDate = getLookupDate(åtgärd.getDatum());
                        currentAvd = avd.getState(stateDate);
                        if (currentAvd != null) break;
                    }
                }
            }
            
            if (avd != null && currentAvd == null && ersh.getSlutdatum() != null) {
                // Try with Slutdatum from the Ersättningshändelse.
                stateDate = getLookupDate(ersh.getSlutdatum());
                currentAvd = avd.getState(stateDate);
            }
            
            if (currentAvd == null) {
                status = Status.LOOKUP_FAIL;
            }
        
            //
            // Translate all the data.
            // 
            
            // Patient            
            Patient patient = ersh.getPatient();
            if (patient != null) {
                careEvent.setPatient(TransformHelper.createRivPatientFromErsättningsPatient(patient));
            }

            // Care Unit
            String careUnitHSAid = TransformHelper.createCareUnitStructure(ersh, currentFile, careEvent, 
                                                                           ershId, stateDate, currentAvd);

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
                                }
                            }
                        }
                    }
                    
                    if (referredFromHsaId != null) {
                        careEvent.setReferredFrom(referredFromHsaId);
                    }
                    else {
                        status = Status.LOOKUP_FAIL;
                    }
                }
            }
            
            // Contract
            careEvent.setContracts(of.createCareEventTypeContracts());
            
            // Find the contract mapping with the right type
            Vkhform händelseform = vårdkontakt.getHändelseform();
            if (currentAvd != null) {
                // Loop over commissions (SAMVERKS).
                for (TermItemCommission<CommissionState> samverks : currentAvd.getCommissions()) {

                    if (samverks == null) {
                        continue;
                    }
                    CommissionState currentSamverks = samverks.getState(stateDate);

                    if (currentSamverks == null) {
                        continue;
                    }

                    if (currentSamverks.getFollowsTemplate() != null && currentSamverks.getFollowsTemplate() == false) {
                        // This care event uses a test SAMVERKS, skip it.
                        return Status.TEST_SAMVERKS;
                    }
                    
                    String assignmentType = currentSamverks.getAssignmentType();
                    if ("06".equals(assignmentType) || "07".equals(assignmentType) || "08".equals(assignmentType)) {
                        // Lookup the payer organization. Extracted from getCareContractFromState due to number of parameters.
                        String payerOrganization = TransformHelper.getPayerOrganization(händelseform, stateDate, currentAvd, samverks, 
                                                                                        TransformHelper.SLL_CAREGIVER_HSA_ID, referredFromHsaId,
                                                                                        kombika, ershId, currentFile);

                        // Map the current commission information to a CareContractType and add it to the currentEvent list.
                        CareContractType currentContract = TransformHelper.getCareContractFromState(stateDate, careUnitHSAid, samverks, payerOrganization);
                        careEvent.getContracts().getContract().add(currentContract);
                    }
                }
            }

            // Last updated time
            if (updatedTime != null) {
                careEvent.setLastUpdatedTime(TransformHelper.createXMLCalendarFromDate(updatedTime));
            }
            else if (ersh.getLastUpdated() != null) {
                careEvent.setLastUpdatedTime(ersh.getLastUpdated());
            }
            else {
                fatal(String.format("Failed to set last updated time for ersh %s in file %s, all sources are null.", ershId, currentFile));
            }

            // Deleted
            careEvent.setDeleted(ersh.isMakulerad());

            // Date Period
            careEvent.setDatePeriod(of.createDatePeriodType());
            careEvent.getDatePeriod().setStart(ersh.getStartdatum().toXMLFormat().replace("-", ""));
            if (ersh.getSlutdatum() != null) {
                careEvent.getDatePeriod().setEnd(ersh.getSlutdatum().toXMLFormat().replace("-", ""));
            }

            // Emergency
            if (vårdkontakt.getAkut() != null) {
                careEvent.setEmergency(vårdkontakt.getAkut().equals("J"));
            }

            // Event Type
            if (händelseform != null) {
                String händelsetyp = vårdkontakt.getHändelsetyp();
                TransformHelper.createEventTypeStructure(ersh, careEvent, händelseform, händelsetyp);
            }

            // Fee Category
            String avgiftsklass = vårdkontakt.getAvgiftsklass();
            if (avgiftsklass == null || "006".equals(avgiftsklass)) {
                careEvent.setFeeCategory(of.createCVType());
                careEvent.getFeeCategory().setCodeSystem("SLL.CS.TAXA");
                careEvent.getFeeCategory().setCode(vårdkontakt.getPatientavgift());
            }
            else {
                fatal(String.format("Found unexpected Avgiftsklass %s on care event %s in %s.", avgiftsklass, ershId, currentFile));
            }

            // Involved Professions
            TransformHelper.createInvolvedProfessionsStructure(ersh, careEvent);

            // Diagnoses
            TransformHelper.createDiagnosisStructure(ersh, careEvent);

            // Conditions
            TransformHelper.createConditionStructure(ersh, careEvent);

            // Activities
            TransformHelper.createActivityStructure(ersh, careEvent);

            // Stay Before
            VisteFöre stayBefore = vårdkontakt.getVisteFöre();
            if (stayBefore != null) {
                careEvent.setStayBefore(of.createCVType());
                careEvent.getStayBefore().setCode(stayBefore.getKod());
                careEvent.getStayBefore().setCodeSystem("SLL.CS.IKOD");
            }

            // Stay After
            VisteEfter stayAfter = vårdkontakt.getVisteEfter();
            if (stayAfter != null) {
                careEvent.setStayAfter(of.createCVType());
                careEvent.getStayAfter().setCode(stayAfter.getKod());
                careEvent.getStayAfter().setCodeSystem("SLL.CS.UKOD");
            }
            
            // Deceased
            careEvent.setDeceased(stayAfter != null && "7".equals(stayAfter.getKod()));

        }
        catch (Exception e) {
            if (e instanceof TransformationException) {
                throw e;
            }
            throw new TransformationException(String.format("Transformation of care event %s in %s failed fatally: %s",
                                                            ershId, currentFile, e.getMessage()), e);
        }
        
        return status;
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
