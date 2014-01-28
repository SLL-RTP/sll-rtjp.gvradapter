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

import java.nio.file.Path;
import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;

import org.apache.cxf.interceptor.Fault;

import riv.followup.processdevelopment.getadministrativecareevent._1.rivtabp21.GetAdministrativeCareEventResponderInterface;
import riv.followup.processdevelopment.getadministrativecareeventresponder._1.GetAdministrativeCareEventResponse;
import riv.followup.processdevelopment.getadministrativecareeventresponder._1.GetAdministrativeCareEventType;
import riv.followup.processdevelopment.v1.CareEventType;
import se.sll.ersmo.xml.indata.ERSMOIndata;
import se.sll.gvradapter.admincareevent.service.GVRFileService;
import se.sll.gvradapter.gvr.reader.GVRFileReader;
import se.sll.gvradapter.gvr.transform.ERSMOIndataToReimbursementEventTransformer;
import se.sll.gvradapter.gvr.transform.ERSMOIndataUnMarshaller;

/**
 * Fully implemented GetAdministrativeCareEventProducer that is used by the logic free Mule flow. It reads all the new files in the configured in directory,
 * transforms these to the contract format and returns the response.
 * 
 * It uses lightly modified versions of the existing
 * support classes for handling file reading and transformations, which is why the architecture of these are a little unorthodox.
 */
public class GetAdministrativeCareEventProducer implements
		GetAdministrativeCareEventResponderInterface {

	@Override
	@WebResult(name = "GetAdministrativeCareEventResponse", targetNamespace = "urn:riv:followup:processdevelopment:GetAdministrativeCareEventResponder:1", partName = "parameters")
	@WebMethod(operationName = "GetAdministrativeCareEvent", action = "urn:riv:followup:processdevelopment:GetAdministrativeCareEventResponder:1:GetAdministrativeCareEvent")
	public GetAdministrativeCareEventResponse getAdministrativeCareEvent(
			@WebParam(partName = "LogicalAddress", name = "LogicalAddress", targetNamespace = "urn:riv:itintegration:registry:1", header = true) String logicalAddress,
			@WebParam(partName = "parameters", name = "GetAdministrativeCareEvent", targetNamespace = "urn:riv:followup:processdevelopment:GetAdministrativeCareEventResponder:1") GetAdministrativeCareEventType parameters) {
		GetAdministrativeCareEventResponse response = new GetAdministrativeCareEventResponse();

		for (Path currentFile : GVRFileReader.getFileList(parameters.getDate())) {
			// (TODO: Convert to stream instead of a String response)
			String fileContent = GVRFileReader.readFile(currentFile);
			
			// Unmarshal the incoming file content to an ERSMOIndata.
			ERSMOIndata xmlObject;
			try {
				xmlObject = ERSMOIndataUnMarshaller.unmarshalString(fileContent);
			} catch (Exception e) {
				throw new Fault(e);
			}
			
			// Transform all the Ersättningshändelse within the object to CareEventType and add them to the response.
			List<CareEventType> careEventList = ERSMOIndataToReimbursementEventTransformer.doTransform(xmlObject);
			response.getCareEvent().addAll(careEventList);
		}

		return response;
	}

}
