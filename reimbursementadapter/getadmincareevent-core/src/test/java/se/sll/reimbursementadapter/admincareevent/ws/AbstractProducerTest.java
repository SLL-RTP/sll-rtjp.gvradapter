package se.sll.reimbursementadapter.admincareevent.ws;

import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;

import riv.followup.processdevelopment.reimbursement.getadministrativecareeventresponder.v1.GetAdministrativeCareEventResponse;
import riv.followup.processdevelopment.reimbursement.getadministrativecareeventresponder.v1.GetAdministrativeCareEventType;
import riv.followup.processdevelopment.reimbursement.v1.CareEventType;
import riv.followup.processdevelopment.reimbursement.v1.DateTimePeriodType;
import se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse;
import se.sll.reimbursementadapter.TestSupport;
import se.sll.reimbursementadapter.admincareevent.service.CodeServerMEKCacheManagerService;
import se.sll.reimbursementadapter.gvr.RetryBin;
import se.sll.reimbursementadapter.gvr.RetryBinTest;
import se.sll.reimbursementadapter.gvr.reader.GVRFileReader;

public class AbstractProducerTest extends TestSupport
{
    @Autowired
    private GVRFileReader gvrFileReader;

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void abstractProducerWithRetryBinTest() throws Exception
    {
        // Set up classes.
        
        CodeServerMEKCacheManagerService instance = CodeServerMEKCacheManagerService.getInstance();
        instance.revalidate();

        AbstractProducer producer = new AbstractProducer();
        producer.retryBin = new RetryBin();
        producer.retryBin.dir = tmp.getRoot().getAbsolutePath();
        producer.gvrFileReader = gvrFileReader;
        producer.maximumNewCareEvents = 100;
        
        // Prepare stuff in retry bin.
        Ersättningshändelse nowSuccessfulErsh666 = RetryBinTest.createMinimalErsh("666");
        nowSuccessfulErsh666.setSlutverksamhet("30216311002");
        GregorianCalendar cal666 = new GregorianCalendar(2014, 5, 10);
        cal666.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        producer.retryBin.put(RetryBinTest.createMinimalErsh("111"), new GregorianCalendar(2014, 6, 1).getTime());   // To be replaced in bin by new bad update, should also pass through.
        producer.retryBin.put(RetryBinTest.createMinimalErsh("222"), new GregorianCalendar(1988, 10, 10).getTime()); // To be removed from bin because expired.
        producer.retryBin.put(RetryBinTest.createMinimalErsh("333"), new GregorianCalendar(2014, 6, 1).getTime());   // To be kept in bin because nothing changed.
        producer.retryBin.put(RetryBinTest.createMinimalErsh("444"), new GregorianCalendar(2014, 6, 1).getTime());   // To be removed from bin by an update in new file.
        producer.retryBin.put(RetryBinTest.createMinimalErsh("555"), new GregorianCalendar(2015, 10, 10).getTime()); // To be kept in retry bin because ignored since after the file date.
        producer.retryBin.put(nowSuccessfulErsh666, cal666.getTime());  // To be removed because it "now" succeeds in all lookups.
        // 777 To be inserted in the retry bin because is can't be looked up. It should also be passed through even though it is broken.
        // 888 Just a successful care event that passes through without touching the retry bin.
        // 999 A care event present in both the first and the second new file that has a first failed lookup but should be removed from new by second.
        
        producer.retryBin.acceptNewAndSave();
        Assert.assertEquals(6, producer.retryBin.old.size());
        Assert.assertEquals(0, producer.retryBin.nev.size());
        
        // Call producer with nice interval.
        
        GetAdministrativeCareEventType parameters = new GetAdministrativeCareEventType();
        DateTimePeriodType period = new DateTimePeriodType();
        period.setStart(RetryBin.dateToXmlCal(new GregorianCalendar(2014, 6, 14).getTime()));
        period.setEnd(RetryBin.dateToXmlCal(new GregorianCalendar(2014, 6, 16).getTime()));
        parameters.setUpdatedDuringPeriod(period);
        GetAdministrativeCareEventResponse response = producer.getAdministrativeCareEvent0(parameters);
        List<CareEventType> events = response.getCareEvent();

        // Check events.
        
        Assert.assertEquals("OK", response.getResultCode());
        Assert.assertEquals(7, events.size());
        Assert.assertEquals("111", events.get(0).getId());
        Assert.assertEquals("2014-07-15T08:00:00.000Z", events.get(0).getLastUpdatedTime().normalize().toXMLFormat());
        Assert.assertEquals("444", events.get(1).getId());
        Assert.assertEquals("2014-07-15T08:00:00.000Z", events.get(1).getLastUpdatedTime().normalize().toXMLFormat());
        Assert.assertEquals("777", events.get(2).getId());
        Assert.assertEquals("2014-07-15T08:00:00.000Z", events.get(2).getLastUpdatedTime().normalize().toXMLFormat());
        Assert.assertEquals("888", events.get(3).getId());
        Assert.assertEquals("2014-07-15T08:00:00.000Z", events.get(4).getLastUpdatedTime().normalize().toXMLFormat());
        Assert.assertEquals("999", events.get(4).getId());
        Assert.assertEquals("999", events.get(5).getId());
        Assert.assertEquals("2014-07-15T09:00:00.000Z", events.get(5).getLastUpdatedTime().normalize().toXMLFormat());
        Assert.assertEquals("666", events.get(6).getId());
        Assert.assertEquals("2014-06-10T00:00:00.001Z", events.get(6).getLastUpdatedTime().normalize().toXMLFormat());
        
        // Check retry bin.
        
        Assert.assertEquals(4, producer.retryBin.old.size());
        Assert.assertEquals(0, producer.retryBin.nev.size());
        
        Assert.assertNotNull(producer.retryBin.old.get("111"));
        Assert.assertEquals("191212121244", producer.retryBin.old.get("111").getPatient().getID());
        Assert.assertNotNull(producer.retryBin.old.get("333"));
        Assert.assertNotNull(producer.retryBin.old.get("555"));
        Assert.assertNotNull(producer.retryBin.old.get("777"));
    }
}
