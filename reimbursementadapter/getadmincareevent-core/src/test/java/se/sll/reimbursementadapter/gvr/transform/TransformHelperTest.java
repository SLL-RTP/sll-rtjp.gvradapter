package se.sll.reimbursementadapter.gvr.transform;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import se.sll.ersmo.xml.indata.ERSMOIndata;
import se.sll.ersmo.xml.indata.Kon;
import se.sll.ersmo.xml.indata.Vkhform;
import se.sll.reimbursementadapter.TestSupport;
import se.sll.reimbursementadapter.admincareevent.model.CommissionState;
import se.sll.reimbursementadapter.admincareevent.model.FacilityState;
import se.sll.reimbursementadapter.admincareevent.model.TermItemCommission;
import se.sll.reimbursementadapter.admincareevent.service.CodeServerMEKCacheManagerService;
import se.sll.reimbursementadapter.gvr.reader.GVRFileReader;
import se.sll.reimbursementadapter.parser.TermItem;

/**
 * Tests the ERSMOIndataToCareEventTransformer with different
 * transformation scenarios using the test files in the project.
 */
public class TransformHelperTest extends TestSupport {

    /** Lists files matching a period and provides Readers for individual files. */
    @Autowired
    private GVRFileReader gvrFileReader;

    @Test
    public void doTestCodeTransformer() {
        Assert.assertEquals("Öppenvårdskontakt", "2", TransformHelper.mapErsmoKontaktFormToKvKontakttyp(Vkhform.ÖPPENVÅRDSKONTAKT));
        Assert.assertEquals("Slutenvårdstillfälle", "1", TransformHelper.mapErsmoKontaktFormToKvKontakttyp(Vkhform.SLUTENVÅRDSTILLFÄLLE));
        Assert.assertEquals("Hemsjukvårdskontakt", "4", TransformHelper.mapErsmoKontaktFormToKvKontakttyp(Vkhform.HEMSJUKVÅRDSKONTAKT));
    }

    @Test
    public void getPayerOrganization() {
        final CodeServerMEKCacheManagerService instance = CodeServerMEKCacheManagerService.getInstance();
        instance.revalidate(); // ??
        Date stateDate = new Date();
        String sourceFacilityId = "91605010010";
        final TermItem<FacilityState> facilityState = instance.getCurrentIndex().get(sourceFacilityId);
        final TermItemCommission<CommissionState> commissionState = facilityState.getState(stateDate).getCommissions().get(0);
        // Not really part of this test, but it never hurts.
        Assert.assertEquals("Facility ID", "9081", commissionState.getId());
        Assert.assertEquals("Payer facility HSA", "SE2321000016-15CQ", TransformHelper.getPayerOrganization(Vkhform.SLUTENVÅRDSTILLFÄLLE, stateDate, null, commissionState, "SE2321000016-39KJ", null, null, null, null));
    }

    @Test
    public void getPayerOrganizationRequester() {
        final CodeServerMEKCacheManagerService instance = CodeServerMEKCacheManagerService.getInstance();
        instance.revalidate(); // ??
        Date stateDate = new Date();
        String sourceFacilityId = "30216311003";
        final TermItem<FacilityState> facilityState = instance.getCurrentIndex().get(sourceFacilityId);
        final TermItemCommission<CommissionState> commissionState = facilityState.getState(stateDate).getCommissions().get(0);
        // Not really part of this test, but it never hurts.
        Assert.assertEquals("Facility ID", "9081", commissionState.getId());
        Assert.assertEquals("Payer facility HSA", "SE2321000016-39KJ", TransformHelper.getPayerOrganization(Vkhform.SLUTENVÅRDSTILLFÄLLE, stateDate, facilityState.getState(stateDate), commissionState, "SE2321000016-39KJ", null, null, null, null));
    }

    @Test
    public void getPotentialPayerFacilities() {
        final CodeServerMEKCacheManagerService instance = CodeServerMEKCacheManagerService.getInstance();
        instance.revalidate(); // ??
        Date stateDate = new Date();
        String sourceFacilityId = "91605010010";
        final TermItem<FacilityState> facilityState = instance.getCurrentIndex().get(sourceFacilityId);
        final TermItemCommission<CommissionState> commissionState = facilityState.getState(stateDate).getCommissions().get(0);
        // Not really part of this test, but it never hurts.
        Assert.assertEquals("Facility ID", "9081", commissionState.getId());

        Assert.assertEquals("Payer facility 1", "30216311002", TransformHelper.getPotentialPayerFacilities(stateDate, commissionState).get(0).getHSAMapping().getId());
        Assert.assertEquals("Payer facility 1", "30216311003", TransformHelper.getPotentialPayerFacilities(stateDate, commissionState).get(1).getHSAMapping().getId());
    }

    @Test
    public void createRivPatientGenderTest() {
        ERSMOIndata.Ersättningshändelse.Patient patient = new ERSMOIndata.Ersättningshändelse.Patient();
        patient.setID("191212121212");
        patient.setKön(Kon.K);
        Assert.assertEquals("Gender", "1", TransformHelper.createRivPatientFromErsättningsPatient(patient).getGender().getCode());

        patient.setKön(Kon.M);
        Assert.assertEquals("Gender", "2", TransformHelper.createRivPatientFromErsättningsPatient(patient).getGender().getCode());

        patient.setKön(null);
        Assert.assertEquals("Gender", "0", TransformHelper.createRivPatientFromErsättningsPatient(patient).getGender().getCode());
    }
}