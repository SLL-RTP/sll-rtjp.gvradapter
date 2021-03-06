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
package se.sll.reimbursementadapter.processreimbursement.ws;

import javax.jws.WebParam;

import riv.followup.processdevelopment.reimbursement.processreimbursement.v1.rivtabp21.ProcessReimbursementResponderInterface;
import riv.followup.processdevelopment.reimbursement.processreimbursementresponder.v1.ProcessReimbursementRequestType;
import riv.followup.processdevelopment.reimbursement.processreimbursementresponder.v1.ProcessReimbursementResponse;

/**
 * Standard producer implementation for {@link ProcessReimbursementResponderInterface}.
 */
public class ProcessReimbursementProducer extends AbstractProducer implements ProcessReimbursementResponderInterface {

    private static ProcessReimbursementResponse response;

    @Override
    public ProcessReimbursementResponse processReimbursement(@WebParam(partName = "LogicalAddress", name = "LogicalAddress",
            targetNamespace = "urn:riv:itintegration:registry:1", header = true) String logicalAddress,
                                                             @WebParam(partName = "parameters", name = "ProcessReimbursementRequest",
            targetNamespace = "urn:riv:followup:processdevelopment:reimbursement:ProcessReimbursementResponder:1") final ProcessReimbursementRequestType parameters) {
        boolean status = fulfill(new Runnable() {
            @Override
            public void run() {
                response = processReimbursementEvent0(parameters);
            }
        });
        return response;
    }
}
