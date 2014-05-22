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
import org.w3c.dom.Element;

import riv.followup.processdevelopment.reimbursement.processreimbursementresponder.v1.ProcessReimbursementRequestType;
import riv.followup.processdevelopment.reimbursement.v1.ActivityType;
import riv.followup.processdevelopment.reimbursement.v1.ProductType;
import riv.followup.processdevelopment.reimbursement.v1.ProfessionType;
import riv.followup.processdevelopment.reimbursement.v1.ReimbursementEventType;
import riv.followup.processdevelopment.reimbursement.v1.ResidenceType;
import se.sll.hej.xml.indata.HEJIndata;
import se.sll.hej.xml.indata.ObjectFactory;
import se.sll.reimbursementadapter.exception.NumberOfCareEventsExceededException;
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
     * @return The fully transformed HEJIndata object.
     */
    public HEJIndata doTransform(ProcessReimbursementRequestType request, int maxNumberOfCareEvents) throws NumberOfCareEventsExceededException {
        LOG.info("Entering ReimbursementRequestToHEJIndataTransformer.doTransform");

        // Create and populate the base response object
        ObjectFactory of = new ObjectFactory();
        HEJIndata response = of.createHEJIndata();
        response.setKälla(request.getSourceSystem().getName());
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
     * @param currentReimbursementEvent The reimbursement event to transform.
     * @return The transformed HEJIndata.Ersättningshändelse.
     */
    public HEJIndata.Ersättningshändelse transformReimbursementEventToErsättningshändelse(ReimbursementEventType currentReimbursementEvent) {
        // Create response object.
        ObjectFactory of = new ObjectFactory();
        HEJIndata.Ersättningshändelse ersh = of.createHEJIndataErsättningshändelse();

        // Populate root variables according to the spec.
        if (currentReimbursementEvent.getId() != null) {
            ersh.setKälla(currentReimbursementEvent.getId().getSource());
            ersh.setID(currentReimbursementEvent.getId().getValue());
        }

        if (currentReimbursementEvent.isEmergency() != null) {
            ersh.setAkut(currentReimbursementEvent.isEmergency() ? "J" : "N");
        }
        if (currentReimbursementEvent.getEventType() != null) {
            if (currentReimbursementEvent.getEventType().getMainType() != null) {
                ersh.setHändelseform(currentReimbursementEvent.getEventType().getMainType().getCode());
            }
            if (currentReimbursementEvent.getEventType().getSubType() != null) {
                ersh.setTyp(currentReimbursementEvent.getEventType().getSubType().getCode());
            }
        }

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
        // Currently done via the Extras xs:any tag.
        if (currentReimbursementEvent.getPatient() != null && currentReimbursementEvent.getPatient().getAny().size() > 0) {
            Object anyObject = currentReimbursementEvent.getPatient().getAny().get(0);
            LOG.debug("Any class type: " + anyObject.getClass());
            if (anyObject instanceof Element) {
                Element anyElement = (Element) anyObject;
                LOG.debug("Element name: " + anyElement.getNodeName());
                LOG.debug("Element value: " + anyElement.getTextContent());
                if (anyElement.getNodeName().equals("Extras")) {
                    ersh.getPatient().setBasområde(anyElement.getTextContent());
                    TermItem<GeographicalAreaState> geographicalAreaStateTermItem = codeServerCache.get(anyElement.getTextContent());
                    if (geographicalAreaStateTermItem != null) {
                        // TODO: Fix lookup date for mapping! (None available in request atm)
                        Date careEventDate = new Date();
                        final GeographicalAreaState state = geographicalAreaStateTermItem.getState(careEventDate);
                        if (state != null) {
                            ersh.setKundKod("01" + state.getMedicalServiceArea());
                        } else {
                            LOG.error("Could not find any Medical Services code matching the requested geographical " +
                                    "area code : (" + anyElement.getTextContent() + ")." +
                                    "Please check the code server mapping for the geographical area code!");
                            // TODO: What to do in this case?
                        }
                    } else {
                        LOG.error("Could not lookup the Geographical Area code in the cache from the requested code (" + anyElement.getTextContent() + ")");
                        // TODO: What to do in this case?
                    }

                }
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

                åtgärd.setDatum("???");
                åtgärd.setOrdnNr("" + activityIndex++);
                åtgärd.setKod(activity.getActivityCode().getCode());
                ersh.getÅtgärder().getÅtgärd().add(åtgärd);
            }
        }

        // Map product sets
        for (ReimbursementEventType.ProductSet productSet : currentReimbursementEvent.getProductSet()) {
            HEJIndata.Ersättningshändelse.Produktomgång prodOmgång = of.createHEJIndataErsättningshändelseProduktomgång();
            for (ProductType product : productSet.getProduct()) {
                HEJIndata.Ersättningshändelse.Produktomgång.Produkt produkt = of.createHEJIndataErsättningshändelseProduktomgångProdukt();
                produkt.setKod(product.getCode().getCode());
                produkt.setAntal("???");
                produkt.setErsVerksamhet(product.getCareUnit().getCareUnitLocalId().getExtension());
                //produkt.setFbPeriod("???");
                //produkt.setFromDatum("???");
                produkt.setUppdrag(product.getContract().getId().getRoot() + " (" + product.getContract().getName() + ")");
                produkt.setModell(product.getModel().getCode());

                prodOmgång.getProdukt().add(produkt);
            }

            ersh.getProduktomgång().add(prodOmgång);
        }

        return ersh;
    }
}

