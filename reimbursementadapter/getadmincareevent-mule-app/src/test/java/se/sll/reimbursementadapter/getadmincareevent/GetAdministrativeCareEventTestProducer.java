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

import javax.jws.WebParam;
import javax.jws.WebService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.mule.util.RecursiveResourceBundle;

import riv.followup.processdevelopment.reimbursement.getadministrativecareevent.v1.rivtabp21.GetAdministrativeCareEventResponderInterface;
import riv.followup.processdevelopment.reimbursement.getadministrativecareeventresponder.v1.GetAdministrativeCareEventResponse;
import riv.followup.processdevelopment.reimbursement.getadministrativecareeventresponder.v1.GetAdministrativeCareEventType;
import riv.followup.processdevelopment.reimbursement.getadministrativecareeventresponder.v1.ObjectFactory;


@WebService(serviceName = "sampleService", portName = "samplePort", targetNamespace = "urn:org.soitoolkit.refapps.sd.sample.wsdl:v1", name = "sampleService")
public class GetAdministrativeCareEventTestProducer implements GetAdministrativeCareEventResponderInterface {

	public static final String TEST_ID_OK               = "1234567890";
	public static final String TEST_ID_FAULT_INVALID_ID = "-1";
	public static final String TEST_ID_FAULT_TIMEOUT    = "0";
	
	private static final Logger log = LoggerFactory.getLogger(GetAdministrativeCareEventTestProducer.class);
    private static final RecursiveResourceBundle rb = new RecursiveResourceBundle("getadmincareevent-config");
	private static final long SERVICE_TIMOUT_MS = Long.parseLong(rb.getString("SERVICE_TIMEOUT_MS"));

    @Override
    public GetAdministrativeCareEventResponse getAdministrativeCareEvent(@WebParam(partName = "LogicalAddress", name = "LogicalAddress", targetNamespace = "urn:riv:itintegration:registry:1", header = true) String logicalAddress, @WebParam(partName = "parameters", name = "GetAdministrativeCareEvent", targetNamespace = "urn:riv:followup:processdevelopment:reimbursement:GetAdministrativeCareEventResponder:1") GetAdministrativeCareEventType parameters) {
        log.info("GetAdministrativeCareEventTestProducer received the request: {}", parameters);
        ObjectFactory of = new ObjectFactory();
        GetAdministrativeCareEventResponse response = of.createGetAdministrativeCareEventResponse();
        response.setResultCode("OK");
        response.setComment("TEST");


        return null;
    }
}


