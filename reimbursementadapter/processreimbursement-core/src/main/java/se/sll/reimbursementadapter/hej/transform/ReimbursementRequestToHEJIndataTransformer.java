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
import riv.followup.processdevelopment.reimbursement.v1.CVType;
import riv.followup.processdevelopment.reimbursement.v1.PatientType;
import riv.followup.processdevelopment.reimbursement.v1.ProductType;
import riv.followup.processdevelopment.reimbursement.v1.ReimbursementEventType;
import se.sll.hej.xml.indata.HEJIndata;
import se.sll.hej.xml.indata.ObjectFactory;
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
    private static final Logger log = LoggerFactory.getLogger(ReimbursementRequestToHEJIndataTransformer.class);

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
    public HEJIndata doTransform(ProcessReimbursementRequestType request) {
        log.info("Entering ReimbursementRequestToHEJIndataTransformer.doTransform");

        // Create and populate the base response object
        ObjectFactory of = new ObjectFactory();
        HEJIndata response = of.createHEJIndata();
        response.setKälla(request.getSourceSystem().getName());
        response.setID(request.getBatchId());

        // For each reimbursement event in the request, transform to Ersättningshändelse and add to the response list.
        for (ReimbursementEventType currentReimbursementEvent : request.getReimbursementEvent()) {
            response.getErsättningshändelse().add(transformReimbursementEventToErsättningshändelse(currentReimbursementEvent));
        }

        log.info("Exiting ReimbursementRequestToHEJIndataTransformer.doTransform");

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
        ersh.setKälla(currentReimbursementEvent.getId().getSource());
        ersh.setID(currentReimbursementEvent.getId().getValue());
        ersh.setAkut(currentReimbursementEvent.isEmergency() ? "J" : "N");
        ersh.setHändelseform(currentReimbursementEvent.getEventType().getMainType().getCode());
        ersh.setTyp(currentReimbursementEvent.getEventType().getSubType().getCode());

        // Create and populate the Patient tag.
        ersh.setPatient(of.createHEJIndataErsättningshändelsePatient());
        ersh.getPatient().setID(currentReimbursementEvent.getPatient().getId().getId());
        PatientType.Residency residency = currentReimbursementEvent.getPatient().getResidency();
        ersh.getPatient().setLkf(residency.getRegion() + residency.getMunicipality() + residency.getParish());

        // Fetch the geographical area and lookup the medical services area from it.
        // Currently done via the Extras xs:any tag.
        if (currentReimbursementEvent.getPatient().getAny().size() > 0) {
            Object anyObject = currentReimbursementEvent.getPatient().getAny().get(0);
            log.debug("Any class type: " + anyObject.getClass());
            if (anyObject instanceof Element) {
                Element anyElement = (Element) anyObject;
                log.debug("Element name: " + anyElement.getNodeName());
                log.debug("Element value: " + anyElement.getTextContent());
                if (anyElement.getNodeName().equals("Extras")) {
                    ersh.getPatient().setBasområde(anyElement.getTextContent());

                    TermItem<GeographicalAreaState> geographicalAreaStateTermItem = codeServerCache.get(anyElement.getTextContent());
                    if (geographicalAreaStateTermItem != null) {

                        final GeographicalAreaState state = geographicalAreaStateTermItem.getState(new Date());
                        if (state != null) {
                            ersh.setKundKod("01" + state.getMedicalServiceArea());
                        } else {
                            log.error("Could not find any Medical Services code matching the requested geographical area code : (" + anyElement.getTextContent() + ")." +
                                    "Please check the code server mapping for the geographical area code!");
                            // TODO: What to do in this case?
                        }
                    } else {
                        log.error("Could not lookup the Geographical Area code in the cache from the requested code (" + anyElement.getTextContent() + ")");
                        // TODO: What to do in this case?
                    }

                }
            }
        }

        // Map professions
        int professionIndex = 1;
        ersh.setYrkeskategorier(of.createHEJIndataErsättningshändelseYrkeskategorier());
        for (CVType profession : currentReimbursementEvent.getInvolvedProfessions().getProfession()) {
            HEJIndata.Ersättningshändelse.Yrkeskategorier.Yrkeskategori yk = of.createHEJIndataErsättningshändelseYrkeskategorierYrkeskategori();
            yk.setKod(profession.getCode());
            yk.setOrdnNr("" + professionIndex++);
            ersh.getYrkeskategorier().getYrkeskategori().add(yk);
        }

        // Map activities.
        int activityIndex = 1;
        ersh.setÅtgärder(of.createHEJIndataErsättningshändelseÅtgärder());
        for (CVType activity : currentReimbursementEvent.getActivities().getActivity()) {
            HEJIndata.Ersättningshändelse.Åtgärder.Åtgärd åtgärd = of.createHEJIndataErsättningshändelseÅtgärderÅtgärd();
            åtgärd.setDatum("???");
            åtgärd.setOrdnNr("" + activityIndex++);
            åtgärd.setKod(activity.getCode());
            ersh.getÅtgärder().getÅtgärd().add(åtgärd);
        }

        // Map product sets
        for (ReimbursementEventType.ProductSet productSet : currentReimbursementEvent.getProductSet()) {
            HEJIndata.Ersättningshändelse.Produktomgång prodOmgång = of.createHEJIndataErsättningshändelseProduktomgång();
            for (ProductType product : productSet.getProduct()) {
                HEJIndata.Ersättningshändelse.Produktomgång.Produkt produkt = of.createHEJIndataErsättningshändelseProduktomgångProdukt();
                produkt.setKod(product.getCode().getCode());
                produkt.setAntal("???");
                produkt.setErsVerksamhet(product.getCareUnit().getCareUnitLocalId().getCode());
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

