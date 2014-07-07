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
package se.sll.reimbursementadapter.hej.transform;

import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import riv.followup.processdevelopment.reimbursement.processreimbursementresponder.v1.ProcessReimbursementRequestType;
import riv.followup.processdevelopment.reimbursement.v1.ActivityType;
import riv.followup.processdevelopment.reimbursement.v1.ProductType;
import riv.followup.processdevelopment.reimbursement.v1.ProfessionType;
import riv.followup.processdevelopment.reimbursement.v1.ReimbursementEventType;
import riv.followup.processdevelopment.reimbursement.v1.ResidenceType;
import se.sll.hej.xml.indata.HEJIndata;
import se.sll.hej.xml.indata.ObjectFactory;
import se.sll.reimbursementadapter.exception.NumberOfCareEventsExceededException;
import se.sll.reimbursementadapter.exception.TransformationException;
import se.sll.reimbursementadapter.parser.TermItem;
import se.sll.reimbursementadapter.processreimbursement.model.GeographicalAreaState;

/**
 * <p>Takes a RIV {@link ProcessReimbursementRequestType} object, probably from a WS Producer request
 * and transforms it to a {@link HEJIndata} object according to the SPECIFICATION.</p>
 *
 * <p>Uses the CodeServer cache to look up the medical services area that is connected to the
 * geographical area of the patient in the request.</p>
 */
public class ReimbursementRequestToHEJIndataTransformer {

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(ReimbursementRequestToHEJIndataTransformer.class);

    /** The code server cache. */
    public Map<String, TermItem<GeographicalAreaState>> codeServerCache;

    /**
     * Constructor that sets up the class using the provided codeServerCache parameter.
     *
     * @param codeServerCache The instantiated codeServerCache from CodeServerCacheManagerService.
     */
    public ReimbursementRequestToHEJIndataTransformer(Map<String, TermItem<GeographicalAreaState>> codeServerCache) {
        this.codeServerCache = codeServerCache;
    }

    /**
     * Handles the transformation of the entire {@link ProcessReimbursementRequestType} request to the
     * {@link HEJIndata} response.
     *
     * @param request The ProcessReimbursementRequestType that should be transformed.
     *
     * @return The fully transformed HEJIndata object.
     *
     * @throws NumberOfCareEventsExceededException when the number of written care events exceed the maximum
     * allowed number configured.
     *
     * @throws TransformationException when an error or validation failure occurs during the transformation.
     */
    public HEJIndata doTransform(ProcessReimbursementRequestType request, int maxNumberOfCareEvents)
            throws NumberOfCareEventsExceededException, TransformationException {
        LOG.info("Entering ReimbursementRequestToHEJIndataTransformer.doTransform");

        // Create and populate the base response object
        ObjectFactory of = new ObjectFactory();
        HEJIndata response = of.createHEJIndata();
        response.setKälla(request.getSourceSystem().getId());
        response.setID(request.getBatchId());

        // For each reimbursement event in the request, transform to Ersättningshändelse and add to the response list.
        for (ReimbursementEventType currentReimbursementEvent : request.getReimbursementEvent()) {
            if (response.getErsättningshändelse().size() >= maxNumberOfCareEvents) {
                throw new NumberOfCareEventsExceededException("The number of allowed care events (" + maxNumberOfCareEvents + ") has been exceeded.");
            }
            response.getErsättningshändelse().add(transformReimbursementEventToErsättningshändelse(currentReimbursementEvent));
        }

        LOG.info("Exiting ReimbursementRequestToHEJIndataTransformer.doTransform");

        return response;
    }

    /**
     * Transforms a single {@link ReimbursementEventType} object to a single {@link HEJIndata.Ersättningshändelse} object.
     *
     * @param currentReimbursementEvent The reimbursement event to transform.a
     * @return The transformed HEJIndata.Ersättningshändelse.
     * @throws TransformationException when an error or validation failure occurs during the transformation.
     */
    public HEJIndata.Ersättningshändelse transformReimbursementEventToErsättningshändelse(ReimbursementEventType currentReimbursementEvent)
            throws TransformationException {
        // Create response object.
        ObjectFactory of = new ObjectFactory();
        HEJIndata.Ersättningshändelse ersh = of.createHEJIndataErsättningshändelse();

        // Populate root variables according to the spec.
        if (currentReimbursementEvent.getId() != null) {
            ersh.setID(currentReimbursementEvent.getId());
        }

        if (currentReimbursementEvent.isEmergency() != null) {
            ersh.setAkut(currentReimbursementEvent.isEmergency() ? "J" : "N");
        }

        if (currentReimbursementEvent.getEventTypeMain() != null) {
            ersh.setHändelseform(currentReimbursementEvent.getEventTypeMain().getCode());
        }
        if (currentReimbursementEvent.getEventTypeSub() != null) {
            ersh.setHändelsetyp(currentReimbursementEvent.getEventTypeSub().getCode());
        }

        // Since we only allow Vårdkontakter in the GetAdminCareEvent logic, we can hard code this here.
        ersh.setHändelseklass("Vårdkontakt");

        // Create and populate the Patient tag.
        if (currentReimbursementEvent.getPatient() != null) {
            ersh.setPatient(of.createHEJIndataErsättningshändelsePatient());

            if (currentReimbursementEvent.getPatient().getId() != null) {
                ersh.getPatient().setID(currentReimbursementEvent.getPatient().getId().getId());
            }

            ResidenceType residency = currentReimbursementEvent.getPatient().getResidence();
            if (residency != null) {
                ersh.getPatient().setLkf(residency.getRegion().getCode() + residency.getMunicipality().getCode() + residency.getParish().getCode());
            }
        }

        // Fetch the geographical area and lookup the medical services area from it.
        if (currentReimbursementEvent.getPatient() != null) {
            String patientLocalResidence = currentReimbursementEvent.getPatient().getLocalResidence();
            // Set the Basområde from the Local Residence.
            ersh.getPatient().setBasområde(patientLocalResidence);

            // Map the Basområde to a Betjäningsområde using the Codeserver cache.
            TermItem<GeographicalAreaState> geographicalAreaStateTermItem = codeServerCache.get(patientLocalResidence);
            if (geographicalAreaStateTermItem != null) {
                // TODO: Fix lookup date for mapping! (None available in request atm)
                Date careEventDate = new Date();
                final GeographicalAreaState state = geographicalAreaStateTermItem.getState(careEventDate);
                if (state != null) {
                    ersh.setKundKod("01" + state.getMedicalServiceArea());
                } else {
                    LOG.error("Could not find any Medical Services code matching the requested geographical " +
                            "area code : (" + patientLocalResidence + ")." +
                            "Please check the code server mapping for the geographical area code!");
                    throw new TransformationException("Could not find any Medical Services code in the cache matching the requested geographical " +
                            "area code : (" + patientLocalResidence + ")." +
                            "Please check the code server mapping for the geographical area code!");
                }
            } else {
                LOG.error("Could not lookup the Geographical Area code in the cache from the requested code (" + patientLocalResidence + ")");
                throw new TransformationException("Could not lookup the Geographical Area code in the cache from the requested code (" + patientLocalResidence + ")");
            }
        }

        // Map professions
        int professionIndex = 1;
        if (currentReimbursementEvent.getInvolvedProfessions() != null) {
            ersh.setYrkeskategorier(of.createHEJIndataErsättningshändelseYrkeskategorier());
            for (ProfessionType profession : currentReimbursementEvent.getInvolvedProfessions().getProfession()) {
                HEJIndata.Ersättningshändelse.Yrkeskategorier.Yrkeskategori yk = of.createHEJIndataErsättningshändelseYrkeskategorierYrkeskategori();

                yk.setKod(profession.getCode());
                yk.setOrdnNr("" + professionIndex++);
                ersh.getYrkeskategorier().getYrkeskategori().add(yk);
            }
        }

        // Map activities.
        int activityIndex = 1;
        if (currentReimbursementEvent.getActivities() != null) {
            ersh.setÅtgärder(of.createHEJIndataErsättningshändelseÅtgärder());
            for (ActivityType activity : currentReimbursementEvent.getActivities().getActivity()) {
                HEJIndata.Ersättningshändelse.Åtgärder.Åtgärd åtgärd = of.createHEJIndataErsättningshändelseÅtgärderÅtgärd();

                åtgärd.setDatum(activity.getDate());
                åtgärd.setOrdnNr("" + activityIndex++);
                åtgärd.setKod(activity.getCode());
                ersh.getÅtgärder().getÅtgärd().add(åtgärd);
            }
        }

        // Map product sets
        for (ReimbursementEventType.ProductSet productSet : currentReimbursementEvent.getProductSet()) {
            HEJIndata.Ersättningshändelse.Produktomgång prodOmgång = of.createHEJIndataErsättningshändelseProduktomgång();
            for (ProductType product : productSet.getProduct()) {
                // Create a new Produkt instance and populate it with basic values from the current 'product' variable.
                HEJIndata.Ersättningshändelse.Produktomgång.Produkt produkt = of.createHEJIndataErsättningshändelseProduktomgångProdukt();
                produkt.setKod(product.getCode().getCode());
                produkt.setAntal("" + product.getCount());
                produkt.setErsVerksamhet(product.getCareUnit().getCareUnitLocalId().getExtension());
                produkt.setLevKod(product.getCareUnit().getCareUnitId());
                produkt.setUppdrag(product.getContract().getId().getExtension());
                produkt.setModell(product.getModel().getCode());

                // Sate the date period, and calculate the FbPerion from this.
                if (product.getDatePeriod() != null) {
                    produkt.setFromDatum(product.getDatePeriod().getStart());
                    produkt.setTomDatum(product.getDatePeriod().getEnd());

                    if (product.getDatePeriod().getEnd() != null && product.getDatePeriod().getEnd().length() > 4) {
                        produkt.setFbPeriod(product.getDatePeriod().getEnd().substring(0, 6));
                    } else {
                        produkt.setFbPeriod(product.getDatePeriod().getStart().substring(0, 6));
                    }
                }

                // Add the created Produkt to the Produktomgång.
                prodOmgång.getProdukt().add(produkt);
            }
            prodOmgång.setTyp("Utförare");
            ersh.getProduktomgång().add(prodOmgång);
        }

        return ersh;
    }
}

