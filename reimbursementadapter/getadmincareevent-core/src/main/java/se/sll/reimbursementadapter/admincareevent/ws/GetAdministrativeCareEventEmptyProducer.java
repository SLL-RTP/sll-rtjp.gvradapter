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
package se.sll.reimbursementadapter.admincareevent.ws;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;

import riv.followup.processdevelopment.reimbursement.getadministrativecareevent.v1.rivtabp21.GetAdministrativeCareEventResponderInterface;
import riv.followup.processdevelopment.reimbursement.getadministrativecareeventresponder.v1.GetAdministrativeCareEventResponse;
import riv.followup.processdevelopment.reimbursement.getadministrativecareeventresponder.v1.GetAdministrativeCareEventType;

/**
 * Empty producer for GetAdministrativeCareEvent that only returns an empty
 * response which the stand-alone Mule flow then loads with data.
 */
public class GetAdministrativeCareEventEmptyProducer implements
		GetAdministrativeCareEventResponderInterface {

	@Override
	@WebResult(name = "GetAdministrativeCareEventResponse", targetNamespace = "urn:riv:followup:processdevelopment:GetAdministrativeCareEventResponder:1", partName = "parameters")
	@WebMethod(operationName = "GetAdministrativeCareEvent", action = "urn:riv:followup:processdevelopment:GetAdministrativeCareEventResponder:1:GetAdministrativeCareEvent")
	public GetAdministrativeCareEventResponse getAdministrativeCareEvent(
			@WebParam(partName = "LogicalAddress", name = "LogicalAddress", targetNamespace = "urn:riv:itintegration:registry:1", header = true) String logicalAddress,
			@WebParam(partName = "parameters", name = "GetAdministrativeCareEvent", targetNamespace = "urn:riv:followup:processdevelopment:GetAdministrativeCareEventResponder:1") GetAdministrativeCareEventType parameters) {
		return new GetAdministrativeCareEventResponse();
	}
}