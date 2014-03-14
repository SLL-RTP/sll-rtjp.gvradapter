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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import riv.followup.processdevelopment.reimbursement.processreimbursementresponder.v1.ProcessReimbursementRequestType;
import riv.followup.processdevelopment.reimbursement.processreimbursementresponder.v1.ProcessReimbursementResponse;
import riv.followup.processdevelopment.reimbursement.v1.ObjectFactory;
import riv.followup.processdevelopment.reimbursement.v1.ReimbursementEventType;
import se.sll.reimbursementadapter.processreimbursement.service.CodeServerCacheManagerService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:processreimbursement-core-spring-context.xml")
public class ProcessReimbursementProducerTest extends AbstractProducer {

    @Test
    public void testProcessReimbursement() throws Exception {
        /*CodeServerCacheManagerService instance = CodeServerCacheManagerService.getInstance();
        instance.revalidate();
        riv.followup.processdevelopment.reimbursement.processreimbursementresponder.v1.ObjectFactory rivOf = new riv.followup.processdevelopment.reimbursement.processreimbursementresponder.v1.ObjectFactory();
        ObjectFactory of = new ObjectFactory();

        ProcessReimbursementRequestType req = rivOf.createProcessReimbursementRequestType();
        req.setBatchId("1");
        req.setSourceSystem(of.createSourceType());
        req.getSourceSystem().setName("haha");
        req.getBatchId();
        req.getSourceSystem().setOrganization("1.2.3.5");
        ReimbursementEventType event = of.createReimbursementEventType();
        event.setPatient(of.createPatientType());

        ProcessReimbursementResponse response = processReimbursementEvent0(req);*/
        // Read the file..
    }
}
