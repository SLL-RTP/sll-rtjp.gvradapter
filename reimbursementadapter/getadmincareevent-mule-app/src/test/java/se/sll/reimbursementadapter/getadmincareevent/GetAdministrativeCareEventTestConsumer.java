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
package se.sll.reimbursementadapter.getadmincareevent;

import static se.sll.reimbursementadapter.GvrAdapterMuleServer.getAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import riv.followup.processdevelopment.reimbursement.getadministrativecareevent.v1.rivtabp21.GetAdministrativeCareEventResponderInterface;
import riv.followup.processdevelopment.reimbursement.getadministrativecareeventresponder.v1.GetAdministrativeCareEventResponse;
import riv.followup.processdevelopment.reimbursement.getadministrativecareeventresponder.v1.GetAdministrativeCareEventType;
import riv.followup.processdevelopment.reimbursement.getadministrativecareeventresponder.v1.ObjectFactory;
import riv.followup.processdevelopment.reimbursement.v1.TimePeriodMillisType;
import se.sll.reimbursementadapter.AbstractTestConsumer;

public class GetAdministrativeCareEventTestConsumer extends AbstractTestConsumer<GetAdministrativeCareEventResponderInterface> {

    private static final Logger log = LoggerFactory.getLogger(GetAdministrativeCareEventTestConsumer.class);

    //private static final RecursiveResourceBundle rb = new RecursiveResourceBundle("codeserveradapter-config");

    public GetAdministrativeCareEventTestConsumer(String serviceAddress) {
        super(GetAdministrativeCareEventResponderInterface.class, serviceAddress);
    }

    public static void main(String[] args) {
        String serviceAddress = getAddress("pr.ws.inboundURL");
        GetAdministrativeCareEventTestConsumer consumer = new GetAdministrativeCareEventTestConsumer(serviceAddress);
        consumer.callService("20140201095959999", "20140405095959999");
        //ListPaymentResponsibleDataResponseType response = consumer.callService("1234");
        //log.info("Returned value = " + response.getPaymentResponsibleData().getHsaId());
    }

    public GetAdministrativeCareEventResponse callService(String startDate, String endDate) {
        log.debug("Calling sample-soap-service with startdate = {} and enddate = {}", startDate, endDate);
        ObjectFactory of = new ObjectFactory();


        GetAdministrativeCareEventType request = of.createGetAdministrativeCareEventType();
        TimePeriodMillisType ts = new TimePeriodMillisType();
        ts.setStart("20140201095959999");
        ts.setEnd("20140405095959999");
        request.setUpdatedDuringPeriod(ts);
        final GetAdministrativeCareEventResponse response = _service.getAdministrativeCareEvent("1234", request);

        return response;
    }

}