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
package se.sll.reimbursementadapter.getadmincareevent.ws;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import riv.followup.processdevelopment.reimbursement.getadministrativecareeventresponder.v1.GetAdministrativeCareEventResponse;
import riv.followup.processdevelopment.reimbursement.getadministrativecareeventresponder.v1.GetAdministrativeCareEventType;
import riv.followup.processdevelopment.reimbursement.v1.CareEventType;
import riv.followup.processdevelopment.reimbursement.v1.DateTimePeriodType;
import se.sll.reimbursementadapter.admincareevent.service.CodeServerMEKCacheManagerService;
import se.sll.reimbursementadapter.admincareevent.ws.AbstractProducer;
import se.sll.reimbursementadapter.gvr.reader.GVRFileReader;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:getadmincareevent-core-spring-context.xml")
public class GetAdministrativeCareEventProducerTest extends AbstractProducer {

	@Test
         public void test() {
        CodeServerMEKCacheManagerService.getInstance().revalidate();
        GetAdministrativeCareEventType params = new GetAdministrativeCareEventType();
        params.setUpdatedDuringPeriod(new DateTimePeriodType());
        try {
            params.getUpdatedDuringPeriod().setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-01T10:00:00.000"));
            params.getUpdatedDuringPeriod().setEnd(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-01T11:00:00.000"));
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        //GetAdministrativeCareEventResponse response = new GetAdministrativeCareEventResponse();
        GetAdministrativeCareEventResponse response = this.getAdministrativeCareEvent0(params);
        for (CareEventType careEvent : response.getCareEvent()) {
            if (careEvent.getCareUnit() != null) {
                System.out.println("HSA-id: " + careEvent.getCareUnit().getCareUnitId());
            }
            assertFalse(careEvent.getPatient().getBirthDate().contains("-"));

            if (careEvent.getDatePeriod() != null) {
                if (careEvent.getDatePeriod().getStart() != null) {
                    assertFalse(careEvent.getDatePeriod().getStart().contains("-"));
                }
                if (careEvent.getDatePeriod().getEnd() != null) {
                    assertFalse(careEvent.getDatePeriod().getEnd().contains("-"));
                }
            }
        }
        assertTrue(true);
    }

    @Test
    public void testTimezoneDateFilterInclusive() {
        // TODO: Set the filter method?
        CodeServerMEKCacheManagerService.getInstance().revalidate();
        GetAdministrativeCareEventType params = new GetAdministrativeCareEventType();
        params.setUpdatedDuringPeriod(new DateTimePeriodType());
        try {
            params.getUpdatedDuringPeriod().setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-01T10:00:00.000+02:00"));
            params.getUpdatedDuringPeriod().setEnd(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-03T10:00:00.000+02:00"));
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        //GetAdministrativeCareEventResponse response = new GetAdministrativeCareEventResponse();
        GetAdministrativeCareEventResponse response = this.getAdministrativeCareEvent0(params);
        Assert.assertEquals("Number of CareEvents", 3, response.getCareEvent().size());
        Assert.assertEquals("CareEvent 1 id", "2014-02-01T10:00:00.000+02:00",  response.getCareEvent().get(0).getLastUpdatedTime().toXMLFormat());
        Assert.assertEquals("CareEvent 2 id", "2014-02-02T10:00:00.000+02:00",  response.getCareEvent().get(1).getLastUpdatedTime().toXMLFormat());
        Assert.assertEquals("CareEvent 2 id", "2014-02-03T10:00:00.000+02:00", response.getCareEvent().get(2).getLastUpdatedTime().toXMLFormat());
    }

    @Test
    public void testTimezoneDateFilterStartExclusive() {
        // TODO: Set the filter method?
        CodeServerMEKCacheManagerService.getInstance().revalidate();
        GetAdministrativeCareEventType params = new GetAdministrativeCareEventType();
        params.setUpdatedDuringPeriod(new DateTimePeriodType());
        try {
            // Increment the start time by one millisecond to remove the first file from the filter.
            params.getUpdatedDuringPeriod().setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-01T10:00:00.001+02:00"));
            params.getUpdatedDuringPeriod().setEnd(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-03T10:00:00.000+02:00"));
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        //GetAdministrativeCareEventResponse response = new GetAdministrativeCareEventResponse();
        GetAdministrativeCareEventResponse response = this.getAdministrativeCareEvent0(params);
        Assert.assertEquals("Number of CareEvents", 2, response.getCareEvent().size());
        //Assert.assertEquals("CareEvent 1 id", "2014-02-01T10:00:00.000+02:00",  response.getCareEvent().get(0).getLastUpdatedTime().toXMLFormat());
        Assert.assertEquals("CareEvent 2 id", "2014-02-02T10:00:00.000+02:00",  response.getCareEvent().get(0).getLastUpdatedTime().toXMLFormat());
        Assert.assertEquals("CareEvent 2 id", "2014-02-03T10:00:00.000+02:00",  response.getCareEvent().get(1).getLastUpdatedTime().toXMLFormat());
    }

    @Test
    public void testTimezoneDateFilterEndExclusive() {
        // TODO: Set the filter method?
        CodeServerMEKCacheManagerService.getInstance().revalidate();
        GetAdministrativeCareEventType params = new GetAdministrativeCareEventType();
        params.setUpdatedDuringPeriod(new DateTimePeriodType());
        try {
            params.getUpdatedDuringPeriod().setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-01T10:00:00.000+02:00"));
            // Decrement the end time by one millisecond to remove the last file from the filter.
            params.getUpdatedDuringPeriod().setEnd(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-03T09:59:59.999+02:00"));
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        //GetAdministrativeCareEventResponse response = new GetAdministrativeCareEventResponse();
        GetAdministrativeCareEventResponse response = this.getAdministrativeCareEvent0(params);
        Assert.assertEquals("Number of CareEvents", 2, response.getCareEvent().size());
        Assert.assertEquals("CareEvent 1 id", "2014-02-01T10:00:00.000+02:00",  response.getCareEvent().get(0).getLastUpdatedTime().toXMLFormat());
        Assert.assertEquals("CareEvent 2 id", "2014-02-02T10:00:00.000+02:00",  response.getCareEvent().get(1).getLastUpdatedTime().toXMLFormat());
        //Assert.assertEquals("CareEvent 2 id", "2014-02-03T10:00:00.000+02:00",  response.getCareEvent().get(2).getLastUpdatedTime().toXMLFormat());
    }

    @Test
    public void testTimezoneDateFilterInclusiveDifferentTimeZones() {
        // TODO: Set the filter method?
        CodeServerMEKCacheManagerService.getInstance().revalidate();
        GetAdministrativeCareEventType params = new GetAdministrativeCareEventType();
        params.setUpdatedDuringPeriod(new DateTimePeriodType());
        try {
            // All times decreased by the offset, but it is still the same datetime instances as the preceding test.
            params.getUpdatedDuringPeriod().setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-01T13:00:00.000+05:00"));
            params.getUpdatedDuringPeriod().setEnd(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-03T04:00:00.000-04:00"));
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        //GetAdministrativeCareEventResponse response = new GetAdministrativeCareEventResponse();
        GetAdministrativeCareEventResponse response = this.getAdministrativeCareEvent0(params);
        Assert.assertEquals("Number of CareEvents", 3, response.getCareEvent().size());
        Assert.assertEquals("CareEvent 1 id", "2014-02-01T10:00:00.000+02:00",  response.getCareEvent().get(0).getLastUpdatedTime().toXMLFormat());
        Assert.assertEquals("CareEvent 2 id", "2014-02-02T10:00:00.000+02:00",  response.getCareEvent().get(1).getLastUpdatedTime().toXMLFormat());
        Assert.assertEquals("CareEvent 2 id", "2014-02-03T10:00:00.000+02:00",  response.getCareEvent().get(2).getLastUpdatedTime().toXMLFormat());
    }

    @Test
    public void testTimezoneDateFilterStartExclusiveDifferentTimeZones() {
        // TODO: Set the filter method?
        CodeServerMEKCacheManagerService.getInstance().revalidate();
        GetAdministrativeCareEventType params = new GetAdministrativeCareEventType();
        params.setUpdatedDuringPeriod(new DateTimePeriodType());
        try {
            // All times decreased by the offset, but it is still the same datetime instances as the preceding test.
            // Increment the start time by one millisecond to remove the first file from the filter.
            params.getUpdatedDuringPeriod().setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-01T13:00:00.001+05:00"));
            params.getUpdatedDuringPeriod().setEnd(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-03T04:00:00.000-04:00"));
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        GetAdministrativeCareEventResponse response = this.getAdministrativeCareEvent0(params);
        Assert.assertEquals("Number of CareEvents", 2, response.getCareEvent().size());
        //Assert.assertEquals("CareEvent 1 id", "2014-02-01T10:00:00.000+02:00",  response.getCareEvent().get(0).getLastUpdatedTime().toXMLFormat());
        Assert.assertEquals("CareEvent 2 id", "2014-02-02T10:00:00.000+02:00",  response.getCareEvent().get(0).getLastUpdatedTime().toXMLFormat());
        Assert.assertEquals("CareEvent 2 id", "2014-02-03T10:00:00.000+02:00",  response.getCareEvent().get(1).getLastUpdatedTime().toXMLFormat());
    }

    @Test
    public void testTimezoneDateFilterEndExclusiveDifferentTimeZones() {
        // TODO: Set the filter method?
        CodeServerMEKCacheManagerService.getInstance().revalidate();
        GetAdministrativeCareEventType params = new GetAdministrativeCareEventType();
        params.setUpdatedDuringPeriod(new DateTimePeriodType());
        try {
            // All times decreased by the offset, but it is still the same datetime instances as the preceding test.
            // Increment the start time by one millisecond to remove the first file from the filter.
            params.getUpdatedDuringPeriod().setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-01T13:00:00.000+05:00"));
            params.getUpdatedDuringPeriod().setEnd(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-03T03:59:59.999-04:00"));
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        GetAdministrativeCareEventResponse response = this.getAdministrativeCareEvent0(params);
        Assert.assertEquals("Number of CareEvents", 2, response.getCareEvent().size());
        Assert.assertEquals("CareEvent 1 id", "2014-02-01T10:00:00.000+02:00",  response.getCareEvent().get(0).getLastUpdatedTime().toXMLFormat());
        Assert.assertEquals("CareEvent 2 id", "2014-02-02T10:00:00.000+02:00",  response.getCareEvent().get(1).getLastUpdatedTime().toXMLFormat());
        //Assert.assertEquals("CareEvent 2 id", "2014-02-03T10:00:00.000+02:00",  response.getCareEvent().get(2).getLastUpdatedTime().toXMLFormat());
    }
}