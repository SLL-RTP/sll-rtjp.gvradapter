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
package se.sll.gvradapter.admincareevent.ws;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;

import riv.followup.processdevelopment.reimbursement.getadministrativecareevent.v1.rivtabp21.GetAdministrativeCareEventResponderInterface;
import riv.followup.processdevelopment.reimbursement.getadministrativecareeventresponder.v1.GetAdministrativeCareEventResponse;
import riv.followup.processdevelopment.reimbursement.getadministrativecareeventresponder.v1.GetAdministrativeCareEventType;

/**
 * Fully implemented GetAdministrativeCareEventProducer that is used by the logic free Mule flow and the web-app.
 * It reads all the new files in the configured in directory, transforms these to the contract format and returns the response.
 * 
 * It uses lightly modified versions of the existing
 * Mule support classes for handling file reading and transformations, which is why the architecture of these are a little unorthodox.
 */
public class GetAdministrativeCareEventProducer extends AbstractProducer implements
        GetAdministrativeCareEventResponderInterface {

    private static GetAdministrativeCareEventResponse response;

	@Override
	@WebResult(name = "GetAdministrativeCareEventResponse", targetNamespace = "urn:riv:followup:processdevelopment:GetAdministrativeCareEventResponder:1", partName = "parameters")
	@WebMethod(operationName = "GetAdministrativeCareEvent", action = "urn:riv:followup:processdevelopment:GetAdministrativeCareEventResponder:1:GetAdministrativeCareEvent")
	public GetAdministrativeCareEventResponse getAdministrativeCareEvent(
			@WebParam(partName = "LogicalAddress", name = "LogicalAddress", targetNamespace = "urn:riv:itintegration:registry:1", header = true) String logicalAddress,
			@WebParam(partName = "parameters", name = "GetAdministrativeCareEvent", targetNamespace = "urn:riv:followup:processdevelopment:GetAdministrativeCareEventResponder:1") final GetAdministrativeCareEventType parameters) {
        boolean status = fulfill(new Runnable() {
            @Override
            public void run() {
                response = getAdministrativeCareEvent0(parameters);
            }
        });
        return response;
    }

}