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

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import riv.followup.processdevelopment.reimbursement.getadministrativecareeventresponder.v1.GetAdministrativeCareEventResponse;
import riv.followup.processdevelopment.reimbursement.getadministrativecareeventresponder.v1.GetAdministrativeCareEventType;
import riv.followup.processdevelopment.reimbursement.v1.DateTimePeriodType;
import se.sll.reimbursementadapter.admincareevent.service.CodeServerMEKCacheManagerService;
import se.sll.reimbursementadapter.admincareevent.ws.AbstractProducer;
import se.sll.reimbursementadapter.gvr.reader.DateFilterMethod;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:getadmincareevent-core-spring-context.xml")
public class GetAdministrativeCareEventProducerTest extends AbstractProducer {

    @Test
    public void testTimezoneDateFilterInclusive() {
        this.getGvrFileReader().setDateFilterMethod(DateFilterMethod.FILENAME);
        CodeServerMEKCacheManagerService.getInstance().revalidate();
        GetAdministrativeCareEventType params = new GetAdministrativeCareEventType();
        params.setUpdatedDuringPeriod(new DateTimePeriodType());
        try {
            params.getUpdatedDuringPeriod().setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-01T08:00:00.000+00:00"));
            params.getUpdatedDuringPeriod().setEnd(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-03T08:00:00.000+00:00"));
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        //GetAdministrativeCareEventResponse response = new GetAdministrativeCareEventResponse();
        GetAdministrativeCareEventResponse response = this.getAdministrativeCareEvent0(params);
        Assert.assertEquals("Number of CareEvents", 3, response.getCareEvent().size());
        // Datumen är inte under sommartid, därför kommer svaren inte vara i sommartid heller.
        Assert.assertEquals("CareEvent 1 id", "2014-02-01T09:00:00.000+01:00", response.getCareEvent().get(0).getLastUpdatedTime().toXMLFormat());
        Assert.assertEquals("CareEvent 2 id", "2014-02-02T09:00:00.000+01:00", response.getCareEvent().get(1).getLastUpdatedTime().toXMLFormat());
        Assert.assertEquals("CareEvent 2 id", "2014-02-03T09:00:00.000+01:00", response.getCareEvent().get(2).getLastUpdatedTime().toXMLFormat());
    }

    @Test
    public void testTimezoneDateFilterStartExclusive() {
        this.getGvrFileReader().setDateFilterMethod(DateFilterMethod.FILENAME);
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
        Assert.assertEquals("CareEvent 2 id", "2014-02-02T09:00:00.000+01:00",  response.getCareEvent().get(0).getLastUpdatedTime().toXMLFormat());
        Assert.assertEquals("CareEvent 2 id", "2014-02-03T09:00:00.000+01:00",  response.getCareEvent().get(1).getLastUpdatedTime().toXMLFormat());
    }

    @Test
    public void testTimezoneDateFilterEndExclusive() {
        this.getGvrFileReader().setDateFilterMethod(DateFilterMethod.FILENAME);
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
        Assert.assertEquals("CareEvent 1 id", "2014-02-01T09:00:00.000+01:00",  response.getCareEvent().get(0).getLastUpdatedTime().toXMLFormat());
        Assert.assertEquals("CareEvent 2 id", "2014-02-02T09:00:00.000+01:00",  response.getCareEvent().get(1).getLastUpdatedTime().toXMLFormat());
        //Assert.assertEquals("CareEvent 2 id", "2014-02-03T10:00:00.000+02:00",  response.getCareEvent().get(2).getLastUpdatedTime().toXMLFormat());
    }

    @Test
    public void testTimezoneDateFilterInclusiveMetadata() throws Exception {
        this.getGvrFileReader().setDateFilterMethod(DateFilterMethod.METADATA);
        CodeServerMEKCacheManagerService.getInstance().revalidate();

        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmssSSSZ");
        Date expectedDate = sf.parse("20140201080000000+0000");
        Date expectedDate2    = sf.parse("20140202080000000+0000");
        String localPath = this.getGvrFileReader().getLocalPath();
        Path entry = FileSystems.getDefault().getPath(localPath + "ERSMO_2014-02-01T080000.000+0000.xml");
        Files.setLastModifiedTime(entry, FileTime.fromMillis(expectedDate.getTime()));
        entry = FileSystems.getDefault().getPath(localPath + "ERSMO_2014-02-02T080000.000+0000.xml");
        Files.setLastModifiedTime(entry, FileTime.fromMillis(expectedDate2.getTime()));

        GetAdministrativeCareEventType params = new GetAdministrativeCareEventType();
        params.setUpdatedDuringPeriod(new DateTimePeriodType());
        try {
            // All times decreased by the offset, but it is still the same datetime instances as the preceding test.
            params.getUpdatedDuringPeriod().setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-01T10:00:00.000+02:00"));
            params.getUpdatedDuringPeriod().setEnd(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-02T10:00:00.000+02:00"));
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        //GetAdministrativeCareEventResponse response = new GetAdministrativeCareEventResponse();
        GetAdministrativeCareEventResponse response = this.getAdministrativeCareEvent0(params);
        Assert.assertEquals("Number of CareEvents", 2, response.getCareEvent().size());
        Assert.assertEquals("CareEvent 1 id", "2014-02-01T09:00:00.000+01:00",  response.getCareEvent().get(0).getLastUpdatedTime().toXMLFormat());
        Assert.assertEquals("CareEvent 2 id", "2014-02-02T09:00:00.000+01:00",  response.getCareEvent().get(1).getLastUpdatedTime().toXMLFormat());
    }

    @Test
    public void testTimezoneDateFilterStartExclusiveMetadata() throws Exception {
        this.getGvrFileReader().setDateFilterMethod(DateFilterMethod.METADATA);
        CodeServerMEKCacheManagerService.getInstance().revalidate();

        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmssSSSZ");
        Date expectedDate = sf.parse("20140201080000000+0000");
        Date expectedDate2    = sf.parse("20140202080000000+0000");
        String localPath = this.getGvrFileReader().getLocalPath();
        Path entry = FileSystems.getDefault().getPath(localPath + "ERSMO_2014-02-01T080000.000+0000.xml");
        Files.setLastModifiedTime(entry, FileTime.fromMillis(expectedDate.getTime()));
        entry = FileSystems.getDefault().getPath(localPath + "ERSMO_2014-02-02T080000.000+0000.xml");
        Files.setLastModifiedTime(entry, FileTime.fromMillis(expectedDate2.getTime()));

        GetAdministrativeCareEventType params = new GetAdministrativeCareEventType();
        params.setUpdatedDuringPeriod(new DateTimePeriodType());
        try {
            // All times decreased by the offset, but it is still the same datetime instances as the preceding test.
            params.getUpdatedDuringPeriod().setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-01T10:00:00.001+02:00"));
            params.getUpdatedDuringPeriod().setEnd(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-02T10:00:00.000+02:00"));
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        //GetAdministrativeCareEventResponse response = new GetAdministrativeCareEventResponse();
        GetAdministrativeCareEventResponse response = this.getAdministrativeCareEvent0(params);
        Assert.assertEquals("Number of CareEvents", 1, response.getCareEvent().size());
        Assert.assertEquals("CareEvent 1 id", "2014-02-02T09:00:00.000+01:00",  response.getCareEvent().get(0).getLastUpdatedTime().toXMLFormat());
    }

    @Test
    public void testTimezoneDateFilterEndExclusiveMetadata() throws Exception {
        this.getGvrFileReader().setDateFilterMethod(DateFilterMethod.METADATA);
        CodeServerMEKCacheManagerService.getInstance().revalidate();
        for (String test : CodeServerMEKCacheManagerService.getInstance().getCurrentIndex().keySet()) {
            if (test.startsWith("191370")) {
                System.out.println("Kombika: " + test);

            }
        }
        System.out.println("Testget: " + CodeServerMEKCacheManagerService.getInstance().getCurrentIndex().get("19137011000"));
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmssSSSZ");
        Date expectedDate = sf.parse("20140201080000000+0000");
        Date expectedDate2    = sf.parse("20140202080000000+0000");
        String localPath = this.getGvrFileReader().getLocalPath();
        Path entry = FileSystems.getDefault().getPath(localPath + "ERSMO_2014-02-01T080000.000+0000.xml");
        Files.setLastModifiedTime(entry, FileTime.fromMillis(expectedDate.getTime()));
        entry = FileSystems.getDefault().getPath(localPath + "ERSMO_2014-02-02T080000.000+0000.xml");
        Files.setLastModifiedTime(entry, FileTime.fromMillis(expectedDate2.getTime()));

        GetAdministrativeCareEventType params = new GetAdministrativeCareEventType();
        params.setUpdatedDuringPeriod(new DateTimePeriodType());
        try {
            // All times decreased by the offset, but it is still the same datetime instances as the preceding test.
            params.getUpdatedDuringPeriod().setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-01T10:00:00.000+02:00"));
            params.getUpdatedDuringPeriod().setEnd(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-02T09:59:59.999+02:00"));
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        //GetAdministrativeCareEventResponse response = new GetAdministrativeCareEventResponse();
        GetAdministrativeCareEventResponse response = this.getAdministrativeCareEvent0(params);
        Assert.assertEquals("Number of CareEvents", 1, response.getCareEvent().size());
        Assert.assertEquals("CareEvent 1 id", "2014-02-01T09:00:00.000+01:00",  response.getCareEvent().get(0).getLastUpdatedTime().toXMLFormat());
        //Assert.assertEquals("CareEvent 2 id", "2014-02-02T10:00:00.000+02:00",  response.getCareEvent().get(1).getLastUpdatedTime().toXMLFormat());
    }

    @Test
    public void testTimezoneDateFilterInclusiveDifferentTimeZones() {
        this.getGvrFileReader().setDateFilterMethod(DateFilterMethod.FILENAME);
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
        Assert.assertEquals("CareEvent 1 id", "2014-02-01T09:00:00.000+01:00",  response.getCareEvent().get(0).getLastUpdatedTime().toXMLFormat());
        Assert.assertEquals("CareEvent 2 id", "2014-02-02T09:00:00.000+01:00",  response.getCareEvent().get(1).getLastUpdatedTime().toXMLFormat());
        Assert.assertEquals("CareEvent 2 id", "2014-02-03T09:00:00.000+01:00",  response.getCareEvent().get(2).getLastUpdatedTime().toXMLFormat());
    }

    @Test
    public void testTimezoneDateFilterInclusiveDST() {
        this.getGvrFileReader().setDateFilterMethod(DateFilterMethod.FILENAME);
        CodeServerMEKCacheManagerService.getInstance().revalidate();
        GetAdministrativeCareEventType params = new GetAdministrativeCareEventType();
        params.setUpdatedDuringPeriod(new DateTimePeriodType());
        try {
            // All times decreased by the offset, but it is still the same datetime instances as the preceding test.
            params.getUpdatedDuringPeriod().setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-07-01T08:00:00.000+00:00"));
            params.getUpdatedDuringPeriod().setEnd(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-07-01T08:00:00.000+00:00"));
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        //GetAdministrativeCareEventResponse response = new GetAdministrativeCareEventResponse();
        GetAdministrativeCareEventResponse response = this.getAdministrativeCareEvent0(params);
        Assert.assertEquals("Number of CareEvents", 1, response.getCareEvent().size());
        Assert.assertEquals("CareEvent 1 id", "2014-07-01T10:00:00.000+02:00",  response.getCareEvent().get(0).getLastUpdatedTime().toXMLFormat());
    }

    @Test
    public void testTimezoneDateFilterStartExclusiveDifferentTimeZones() {
        this.getGvrFileReader().setDateFilterMethod(DateFilterMethod.FILENAME);
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
        Assert.assertEquals("CareEvent 2 id", "2014-02-02T09:00:00.000+01:00",  response.getCareEvent().get(0).getLastUpdatedTime().toXMLFormat());
        Assert.assertEquals("CareEvent 2 id", "2014-02-03T09:00:00.000+01:00",  response.getCareEvent().get(1).getLastUpdatedTime().toXMLFormat());
    }

    @Test
    public void testTimezoneDateFilterEndExclusiveDifferentTimeZones() {
        this.getGvrFileReader().setDateFilterMethod(DateFilterMethod.FILENAME);
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
        Assert.assertEquals("CareEvent 1 id", "2014-02-01T09:00:00.000+01:00",  response.getCareEvent().get(0).getLastUpdatedTime().toXMLFormat());
        Assert.assertEquals("CareEvent 2 id", "2014-02-02T09:00:00.000+01:00",  response.getCareEvent().get(1).getLastUpdatedTime().toXMLFormat());
        //Assert.assertEquals("CareEvent 2 id", "2014-02-03T10:00:00.000+02:00",  response.getCareEvent().get(2).getLastUpdatedTime().toXMLFormat());
    }

    @Test
    public void testTimezoneDateFilterInclusiveDifferentTimeZonesMetadata() throws Exception {
        this.getGvrFileReader().setDateFilterMethod(DateFilterMethod.METADATA);
        CodeServerMEKCacheManagerService.getInstance().revalidate();

        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmssSSSZ");
        Date expectedDate = sf.parse("20140201080000000+0000");
        Date expectedDate2    = sf.parse("20140202080000000+0000");
        String localPath = this.getGvrFileReader().getLocalPath();
        Path entry = FileSystems.getDefault().getPath(localPath + "ERSMO_2014-02-01T080000.000+0000.xml");
        Files.setLastModifiedTime(entry, FileTime.fromMillis(expectedDate.getTime()));
        entry = FileSystems.getDefault().getPath(localPath + "ERSMO_2014-02-02T080000.000+0000.xml");
        Files.setLastModifiedTime(entry, FileTime.fromMillis(expectedDate2.getTime()));

        GetAdministrativeCareEventType params = new GetAdministrativeCareEventType();
        params.setUpdatedDuringPeriod(new DateTimePeriodType());
        try {
            // All times decreased by the offset, but it is still the same datetime instances as the preceding test.
            params.getUpdatedDuringPeriod().setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-01T13:00:00.000+05:00"));
            params.getUpdatedDuringPeriod().setEnd(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-02T04:00:00.000-04:00"));
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        //GetAdministrativeCareEventResponse response = new GetAdministrativeCareEventResponse();
        GetAdministrativeCareEventResponse response = this.getAdministrativeCareEvent0(params);
        Assert.assertEquals("Number of CareEvents", 2, response.getCareEvent().size());
        Assert.assertEquals("CareEvent 1 id", "2014-02-01T09:00:00.000+01:00",  response.getCareEvent().get(0).getLastUpdatedTime().toXMLFormat());
        Assert.assertEquals("CareEvent 2 id", "2014-02-02T09:00:00.000+01:00",  response.getCareEvent().get(1).getLastUpdatedTime().toXMLFormat());
    }

    @Test
    public void testTimezoneDateFilterStartExclusiveDifferentTimeZonesMetadata() throws Exception {
        this.getGvrFileReader().setDateFilterMethod(DateFilterMethod.METADATA);
        CodeServerMEKCacheManagerService.getInstance().revalidate();

        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmssSSSZ");
        Date expectedDate = sf.parse("20140201080000000+0000");
        Date expectedDate2    = sf.parse("20140202080000000+0000");
        String localPath = this.getGvrFileReader().getLocalPath();
        Path entry = FileSystems.getDefault().getPath(localPath + "ERSMO_2014-02-01T080000.000+0000.xml");
        Files.setLastModifiedTime(entry, FileTime.fromMillis(expectedDate.getTime()));
        entry = FileSystems.getDefault().getPath(localPath + "ERSMO_2014-02-02T080000.000+0000.xml");
        Files.setLastModifiedTime(entry, FileTime.fromMillis(expectedDate2.getTime()));

        GetAdministrativeCareEventType params = new GetAdministrativeCareEventType();
        params.setUpdatedDuringPeriod(new DateTimePeriodType());
        try {
            // All times decreased by the offset, but it is still the same datetime instances as the preceding test.
            params.getUpdatedDuringPeriod().setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-01T13:00:00.001+05:00"));
            params.getUpdatedDuringPeriod().setEnd(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-02T04:00:00.000-04:00"));
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        //GetAdministrativeCareEventResponse response = new GetAdministrativeCareEventResponse();
        GetAdministrativeCareEventResponse response = this.getAdministrativeCareEvent0(params);
        Assert.assertEquals("Number of CareEvents", 1, response.getCareEvent().size());
        Assert.assertEquals("CareEvent 1 id", "2014-02-02T09:00:00.000+01:00",  response.getCareEvent().get(0).getLastUpdatedTime().toXMLFormat());
    }

    @Test
    public void testTimezoneDateFilterEndExclusiveDifferentTimeZonesMetadata() throws Exception {
        this.getGvrFileReader().setDateFilterMethod(DateFilterMethod.METADATA);
        CodeServerMEKCacheManagerService.getInstance().revalidate();

        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmssSSSZ");
        Date expectedDate = sf.parse("20140201080000000+0000");
        Date expectedDate2    = sf.parse("20140202080000000+0000");
        String localPath = this.getGvrFileReader().getLocalPath();
        Path entry = FileSystems.getDefault().getPath(localPath + "ERSMO_2014-02-01T080000.000+0000.xml");
        Files.setLastModifiedTime(entry, FileTime.fromMillis(expectedDate.getTime()));
        entry = FileSystems.getDefault().getPath(localPath + "ERSMO_2014-02-02T080000.000+0000.xml");
        Files.setLastModifiedTime(entry, FileTime.fromMillis(expectedDate2.getTime()));

        GetAdministrativeCareEventType params = new GetAdministrativeCareEventType();
        params.setUpdatedDuringPeriod(new DateTimePeriodType());
        try {
            // All times decreased by the offset, but it is still the same datetime instances as the preceding test.
            params.getUpdatedDuringPeriod().setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-01T13:00:00.000+05:00"));
            params.getUpdatedDuringPeriod().setEnd(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-02T03:59:59.999-04:00"));
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        //GetAdministrativeCareEventResponse response = new GetAdministrativeCareEventResponse();
        GetAdministrativeCareEventResponse response = this.getAdministrativeCareEvent0(params);
        Assert.assertEquals("Number of CareEvents", 1, response.getCareEvent().size());
        Assert.assertEquals("CareEvent 1 id", "2014-02-01T09:00:00.000+01:00",  response.getCareEvent().get(0).getLastUpdatedTime().toXMLFormat());
        //Assert.assertEquals("CareEvent 2 id", "2014-02-02T10:00:00.000+02:00",  response.getCareEvent().get(1).getLastUpdatedTime().toXMLFormat());
    }

    @Test
    public void testTruncationPartResponses() {
        this.maximumSupportedCareEvents = 8;
        this.getGvrFileReader().setDateFilterMethod(DateFilterMethod.FILENAME);
        CodeServerMEKCacheManagerService.getInstance().revalidate();
        GetAdministrativeCareEventType params = new GetAdministrativeCareEventType();
        params.setUpdatedDuringPeriod(new DateTimePeriodType());
        try {
            params.getUpdatedDuringPeriod().setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-04T10:00:00.000+02:00"));
            params.getUpdatedDuringPeriod().setEnd(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-05T10:00:00.000+02:00"));
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        GetAdministrativeCareEventResponse response = this.getAdministrativeCareEvent0(params);
        Assert.assertEquals("Result Code", "TRUNCATED", response.getResultCode());
        Assert.assertEquals("Result Comment", "Response was truncated due to hitting the maximum configured number of Care Events of 8", response.getComment());
        Assert.assertEquals("Number of CareEvents", 5, response.getCareEvent().size());
        Assert.assertEquals("StartDate 1 id", "2014-02-04T10:00:00.000+02:00",  response.getResponseTimePeriod().getStart().toXMLFormat());
        Assert.assertEquals("CareEvent 2 id", "2014-02-04T09:00:00.000+01:00",  response.getResponseTimePeriod().getEnd().toXMLFormat());
        //Assert.assertEquals("CareEvent 2 id", "2014-02-03T10:00:00.000+02:00",  response.getCareEvent().get(2).getLastUpdatedTime().toXMLFormat());
    }

    @Test
    public void testTruncationNoResponses() {
        this.maximumSupportedCareEvents = 3;
        this.getGvrFileReader().setDateFilterMethod(DateFilterMethod.FILENAME);
        CodeServerMEKCacheManagerService.getInstance().revalidate();
        GetAdministrativeCareEventType params = new GetAdministrativeCareEventType();
        params.setUpdatedDuringPeriod(new DateTimePeriodType());
        try {
            params.getUpdatedDuringPeriod().setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-04T10:00:00.000+02:00"));
            params.getUpdatedDuringPeriod().setEnd(DatatypeFactory.newInstance().newXMLGregorianCalendar("2014-02-05T10:00:00.000+02:00"));
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        GetAdministrativeCareEventResponse response = this.getAdministrativeCareEvent0(params);
        Assert.assertEquals("Result Code", "ERROR", response.getResultCode());
        Assert.assertEquals("Result Comment", "Response was truncated at 0 due to hitting the maximum configured number of Care Events of 3 in the first input file.", response.getComment());
        Assert.assertEquals("Number of CareEvents", 0, response.getCareEvent().size());
        Assert.assertEquals("StartDate 1 id", "2014-02-04T10:00:00.000+02:00",  response.getResponseTimePeriod().getStart().toXMLFormat());
        Assert.assertEquals("CareEvent 2 id", "2014-02-04T10:00:00.000+02:00",  response.getResponseTimePeriod().getEnd().toXMLFormat());
        //Assert.assertEquals("CareEvent 2 id", "2014-02-03T10:00:00.000+02:00",  response.getCareEvent().get(2).getLastUpdatedTime().toXMLFormat());
    }
}