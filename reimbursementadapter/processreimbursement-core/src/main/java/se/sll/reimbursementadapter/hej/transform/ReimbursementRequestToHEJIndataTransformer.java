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

import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Element;
import riv.followup.processdevelopment.reimbursement.processreimbursementresponder.v1.ProcessReimbursementRequestType;
import riv.followup.processdevelopment.reimbursement.v1.CVType;
import riv.followup.processdevelopment.reimbursement.v1.PatientType;
import riv.followup.processdevelopment.reimbursement.v1.ProductType;
import riv.followup.processdevelopment.reimbursement.v1.ReimbursementEventType;
import se.sll.hej.xml.indata.HEJIndata;
import se.sll.hej.xml.indata.ObjectFactory;
import se.sll.reimbursementadapter.parser.CodeServerCode;
import se.sll.reimbursementadapter.parser.TermItem;
import se.sll.reimbursementadapter.processreimbursement.model.GeographicalAreaState;
import se.sll.reimbursementadapter.processreimbursement.service.CodeServerCacheManagerService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReimbursementRequestToHEJIndataTransformer {

    public Map<String, TermItem<GeographicalAreaState>> codeServerIndex;


    public ReimbursementRequestToHEJIndataTransformer(Map<String, TermItem<GeographicalAreaState>> codeServerIndex) {
        this.codeServerIndex = codeServerIndex;
    }

    public HEJIndata doTransform(ProcessReimbursementRequestType request) {
        System.out.println("Cache test: " + codeServerIndex.get("2242363"));
        ObjectFactory of = new ObjectFactory();
        HEJIndata response = of.createHEJIndata();
        List<String> test = new ArrayList<>();
        response.setKälla(request.getSourceSystem().getName());
        response.setID(request.getBatchId());
        for (ReimbursementEventType currentReimbursementEvent : request.getReimbursementEvent()) {
            response.getErsättningshändelse().add(transformReimbursementEventToErsättningshändelse(currentReimbursementEvent));
        }
        return response;
    }

    public HEJIndata.Ersättningshändelse transformReimbursementEventToErsättningshändelse(ReimbursementEventType currentReimbursementEvent) {
        ObjectFactory of = new ObjectFactory();
        HEJIndata.Ersättningshändelse ersh = of.createHEJIndataErsättningshändelse();
        ersh.setKälla(currentReimbursementEvent.getId().getSource());
        ersh.setID(currentReimbursementEvent.getId().getValue());
        ersh.setAkut(currentReimbursementEvent.isEmergency() ? "J" : "N");
        ersh.setHändelseform(currentReimbursementEvent.getEventType().getMainType().getCode());
        ersh.setTyp(currentReimbursementEvent.getEventType().getSubType().getCode());

        ersh.setPatient(of.createHEJIndataErsättningshändelsePatient());
        ersh.getPatient().setID(currentReimbursementEvent.getPatient().getId().getId());
        PatientType.Residency residency = currentReimbursementEvent.getPatient().getResidency();
        ersh.getPatient().setLkf(residency.getRegion() + residency.getMunicipality() + residency.getParish());
        if (currentReimbursementEvent.getPatient().getAny().size() > 0) {
            Object anyObject = currentReimbursementEvent.getPatient().getAny().get(0);
            System.out.println("Any class type: " + anyObject.getClass());
            if (anyObject instanceof Element) {
                Element test = (Element) anyObject;
                System.out.println("Element name: " + test.getNodeName());
                System.out.println("Element value: " + test.getTextContent());
                ersh.getPatient().setBasområde(test.getTextContent());
            }
        }

        ersh.setKundKod("01" + "MAPPAT BETJÄNINGSOMRÅDE FRÅN KODSERVERN");

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

