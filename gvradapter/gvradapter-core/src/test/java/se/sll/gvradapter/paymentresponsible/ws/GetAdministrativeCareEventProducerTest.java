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
package se.sll.gvradapter.paymentresponsible.ws;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import riv.followup.processdevelopment.reimbursement.getadministrativecareeventresponder.v1.GetAdministrativeCareEventResponse;
import riv.followup.processdevelopment.reimbursement.getadministrativecareeventresponder.v1.GetAdministrativeCareEventType;
import riv.followup.processdevelopment.reimbursement.v1.CareEventType;
import riv.followup.processdevelopment.reimbursement.v1.TimePeriodMillisType;
import se.sll.gvradapter.admincareevent.service.CodeServerMEKCacheManagerService;
import se.sll.gvradapter.admincareevent.ws.AbstractProducer;

import static org.junit.Assert.assertFalse;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:gvradapter-core-spring-context.xml")
public class GetAdministrativeCareEventProducerTest extends AbstractProducer {

	@Test
	public void test() {
		CodeServerMEKCacheManagerService.getInstance().revalidate();
		GetAdministrativeCareEventType params = new GetAdministrativeCareEventType();
		params.setUpdatedDuringPeriod(new TimePeriodMillisType());
        params.getUpdatedDuringPeriod().setStart("20140123150600000");

        //GetAdministrativeCareEventResponse response = new GetAdministrativeCareEventResponse();
        GetAdministrativeCareEventResponse response = this.getAdministrativeCareEvent0(params);
		for (CareEventType careEvent : response.getCareEvent()) {
            if (careEvent.getCareUnit() != null) {
			    System.out.println("HSA-id: " + careEvent.getCareUnit().getCareUnitHsaId());
            }
            assertFalse(careEvent.getPatient().getBirthDate().contains("-"));
            assertFalse(careEvent.getDatePeriod().getStartDate().contains("-"));
            assertFalse(careEvent.getDatePeriod().getEndDate().contains("-"));
        }
	}

}

