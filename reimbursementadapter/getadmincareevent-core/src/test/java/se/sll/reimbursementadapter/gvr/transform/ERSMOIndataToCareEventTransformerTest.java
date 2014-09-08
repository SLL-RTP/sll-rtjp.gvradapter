package se.sll.reimbursementadapter.gvr.transform;

import java.io.Reader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import riv.followup.processdevelopment.reimbursement.v1.CareEventType;
import se.sll.ersmo.xml.indata.ERSMOIndata;
import se.sll.ersmo.xml.indata.Vkhform;
import se.sll.reimbursementadapter.TestSupport;
import se.sll.reimbursementadapter.admincareevent.model.CommissionState;
import se.sll.reimbursementadapter.admincareevent.model.FacilityState;
import se.sll.reimbursementadapter.admincareevent.model.TermItemCommission;
import se.sll.reimbursementadapter.admincareevent.service.CodeServerMEKCacheManagerService;
import se.sll.reimbursementadapter.gvr.RetryBin;
import se.sll.reimbursementadapter.gvr.reader.DateFilterMethod;
import se.sll.reimbursementadapter.gvr.reader.GVRFileReader;
import se.sll.reimbursementadapter.parser.TermItem;

/**
 * Tests the ERSMOIndataToCareEventTransformer with different
 * transformation scenarios using the test files in the project.
 */
public class ERSMOIndataToCareEventTransformerTest extends TestSupport {

    /** Lists files matching a period and provides Readers for individual files. */
    @Autowired
    private GVRFileReader gvrFileReader;

    /**
     * Tests the entire transformation of a ERSMOIndata file to a List of CareEventTypes
     * @throws Exception on IO exceptions when reading files.
     */
    @Test
    public void testDoTransform() throws Exception {
        // Populate the cache necessary for the mapping to function correctly.
        final CodeServerMEKCacheManagerService instance = CodeServerMEKCacheManagerService.getInstance();
        instance.revalidate();

        // Set the GVR Filter method to work with file names for this test.
        gvrFileReader.setDateFilterMethod(DateFilterMethod.FILENAME);

        // Read a given ERSMOIndata file and marshal to an XML-object.
        Path inFile = FileSystems.getDefault().getPath(gvrFileReader.getLocalPath() + "ERSMO_2014-02-02T080000.000+0000.xml");
        Reader fileReader = gvrFileReader.getReaderForFile(inFile);
        ERSMOIndataMarshaller unMarshaller = new ERSMOIndataMarshaller();
        ERSMOIndata indata = unMarshaller.unmarshal(fileReader);

        // Transform to a list of RIV CareEventTypes.
        List<CareEventType> careEventList = new ArrayList<CareEventType>(); 
        ERSMOIndataToCareEventTransformer.doTransform(new RetryBin(), true, careEventList, indata.getErsättningshändelse(), gvrFileReader.getDateFromGVRFile(inFile), inFile);

        // Validate contents of transformed RIV objects.
        Assert.assertEquals("Number of Care Events", 1, careEventList.size());
        CareEventType careEventType = careEventList.get(0);

        Assert.assertEquals("ID#1", "12345678901234567890", careEventType.getId());

        // Source system
        Assert.assertEquals("Source System organization", "SE2321000016-39KJ", careEventType.getSourceSystem().getOrg());
        Assert.assertEquals("Source System Name", "GVR", careEventType.getSourceSystem().getId());

        // Patient
        Assert.assertEquals("Patient ID", "191212121212", careEventType.getPatient().getId().getId());
        Assert.assertEquals("Patient ID Type", "1.2.752.129.2.1.3.1", careEventType.getPatient().getId().getType());
        Assert.assertEquals("Patient Birthdate", "19121212", careEventType.getPatient().getBirthDate());
        Assert.assertEquals("Patient Gender", "2", careEventType.getPatient().getGender().getCode());
        Assert.assertEquals("Patient Gender CodeSystem", "1.2.752.129.2.2.1.1", careEventType.getPatient().getGender().getCodeSystem());
        Assert.assertEquals("Patient Residency region", "01", careEventType.getPatient().getResidence().getRegion().getCode());
        Assert.assertEquals("Patient Residency municipality", "02", careEventType.getPatient().getResidence().getMunicipality().getCode());
        Assert.assertEquals("Patient Residency parish", "03", careEventType.getPatient().getResidence().getParish().getCode());
        Assert.assertEquals("Patient Local residence", "1210151", careEventType.getPatient().getLocalResidence());

        // Emergency
        Assert.assertEquals("Emergency", true, careEventType.isEmergency());

        // Event type
        Assert.assertEquals("Event Main Type", "2", careEventType.getEventTypeMain().getCode());
        Assert.assertEquals("Event Main Type", "1.2.752.129.2.2.2.25", careEventType.getEventTypeMain().getCodeSystem());
        Assert.assertEquals("Event Sub Type", "1", careEventType.getEventTypeSub().getCode());
        Assert.assertEquals("Event Sub Type", "SLL.CS.BTYP", careEventType.getEventTypeSub().getCodeSystem());

        // Fee category
        Assert.assertEquals("Fee Category", "12", careEventType.getFeeCategory().getCode());
        Assert.assertEquals("Fee Category", "SLL.CS.TAXA", careEventType.getFeeCategory().getCodeSystem());

        // Contracts
        Assert.assertEquals("Number of contracts", 1, careEventType.getContracts().getContract().size());
        Assert.assertEquals("Contract #1 id", "SE2321000016-39KJ+9081", careEventType.getContracts().getContract().get(0).getId().getExtension());
        Assert.assertEquals("Contract #1 name", "Stockholm Spine center; Ryggkir", careEventType.getContracts().getContract().get(0).getName());
        Assert.assertEquals("Contract #1 type name", "Ryggkirurgi, vårdval", careEventType.getContracts().getContract().get(0).getContractType().getDisplayName().trim());
        Assert.assertEquals("Contract #1 type code", "615", careEventType.getContracts().getContract().get(0).getContractType().getCode());
        Assert.assertEquals("Contract #1 type providerOrg", "SE2321000016-15CQ", careEventType.getContracts().getContract().get(0).getProviderOrganization());
        Assert.assertEquals("Contract #1 type payerOrg", "SE2321000016-39KJ", careEventType.getContracts().getContract().get(0).getPayerOrganization());
        Assert.assertEquals("Contract #1 type requesterOrg", "SE2321000016-39KJ", careEventType.getContracts().getContract().get(0).getRequesterOrganization());

        // Care Unit (spine center öv)
        Assert.assertEquals("Kombika", "SE2321000016-39KJ+30216311002", careEventType.getCareUnit().getCareUnitLocalId().getExtension());
        Assert.assertEquals("Care Unit HSA Id", "SE2321000016-15CQ", careEventType.getCareUnit().getCareUnitId());

        // Updated time (taken from the filename)
        Assert.assertEquals("UpdatedTime", "2014-02-02T09:00:00.000+01:00", careEventType.getLastUpdatedTime().toXMLFormat());

        // Deleted
        Assert.assertEquals("Deleted", false, careEventType.isDeleted());

        // Date Period
        Assert.assertEquals("Date Period start", "20140202", careEventType.getDatePeriod().getStart());
        Assert.assertEquals("Date Period end", "20140202", careEventType.getDatePeriod().getEnd());

        // Involved Professions
        Assert.assertEquals("Profession count", 2, careEventType.getInvolvedProfessions().getProfession().size());
        Assert.assertEquals("Profession #1 code","01", careEventType.getInvolvedProfessions().getProfession().get(0).getCode());
        Assert.assertEquals("Profession #1 OrdnNr", 1, careEventType.getInvolvedProfessions().getProfession().get(0).getSeqNo().intValue());
        Assert.assertEquals("Profession #2 code", "02", careEventType.getInvolvedProfessions().getProfession().get(1).getCode());
        Assert.assertEquals("Profession #2 OrdnNr", 2, careEventType.getInvolvedProfessions().getProfession().get(1).getSeqNo().intValue());

        // Diagnoses
        Assert.assertEquals("Number of diagnoses", 4, careEventType.getDiagnoses().getDiagnosis().size());
        Assert.assertEquals("Diagnosis #1 code", "I050", careEventType.getDiagnoses().getDiagnosis().get(0).getCode());
        Assert.assertEquals("Diagnosis #1 codeSystem", "1.2.752.116.1.1.1.1.3", careEventType.getDiagnoses().getDiagnosis().get(0).getCodeSystem());
        Assert.assertEquals("Diagnosis #1 OrdnNr", 1, careEventType.getDiagnoses().getDiagnosis().get(0).getSeqNo().intValue());
        Assert.assertEquals("Diagnosis #2 code", "J250", careEventType.getDiagnoses().getDiagnosis().get(1).getCode());
        Assert.assertEquals("Diagnosis #2 codeSystem", "1.2.752.116.1.1.1.1.3", careEventType.getDiagnoses().getDiagnosis().get(1).getCodeSystem());
        Assert.assertEquals("Diagnosis #2 OrdnNr", 2, careEventType.getDiagnoses().getDiagnosis().get(1).getSeqNo().intValue());
        Assert.assertEquals("Diagnosis #3 code", "K570", careEventType.getDiagnoses().getDiagnosis().get(2).getCode());
        Assert.assertEquals("Diagnosis #3 codeSystem", "1.2.752.116.1.1.1.1.3", careEventType.getDiagnoses().getDiagnosis().get(2).getCodeSystem());
        Assert.assertEquals("Diagnosis #3 OrdnNr", 3, careEventType.getDiagnoses().getDiagnosis().get(2).getSeqNo().intValue());
        Assert.assertEquals("Diagnosis #4 code", "A01AB13", careEventType.getDiagnoses().getDiagnosis().get(3).getCode());
        Assert.assertEquals("Diagnosis #4 codeSystem", "1.2.752.129.2.2.3.1.1", careEventType.getDiagnoses().getDiagnosis().get(3).getCodeSystem());
        Assert.assertEquals("Diagnosis #4 OrdnNr", 4, careEventType.getDiagnoses().getDiagnosis().get(3).getSeqNo().intValue());
        
        // Conditions
        Assert.assertEquals("Number of conditions", 1, careEventType.getConditions().getCondition().size());
        Assert.assertEquals("Condition #1 code", "ASA6", careEventType.getConditions().getCondition().get(0).getCode());
        Assert.assertEquals("Condition #1 OrdnNr", 1, careEventType.getConditions().getCondition().get(0).getSeqNo().intValue());

        // Activities
        Assert.assertEquals("Number of activities", 4, careEventType.getActivities().getActivity().size());
        Assert.assertEquals("Activity #1 code", "NHP09", careEventType.getActivities().getActivity().get(0).getCode());
        Assert.assertEquals("Activity #1 codeSystem", "1.2.752.116.1.3.2.1.4", careEventType.getActivities().getActivity().get(0).getCodeSystem());
        Assert.assertEquals("Activity #1 date", "20140202", careEventType.getActivities().getActivity().get(0).getDate());
        Assert.assertEquals("Activity #1 OrdnNr", 1, careEventType.getActivities().getActivity().get(0).getSeqNo().intValue());
        Assert.assertEquals("Activity #2 code", "PE009", careEventType.getActivities().getActivity().get(1).getCode());
        Assert.assertEquals("Activity #2 codeSystem", "1.2.752.116.1.3.2.1.4", careEventType.getActivities().getActivity().get(1).getCodeSystem());
        Assert.assertEquals("Activity #2 date", "20140202", careEventType.getActivities().getActivity().get(1).getDate());
        Assert.assertEquals("Activity #2 OrdnNr", 2, careEventType.getActivities().getActivity().get(1).getSeqNo().intValue());
        Assert.assertEquals("Activity #3 code", "AQ014", careEventType.getActivities().getActivity().get(2).getCode());
        Assert.assertEquals("Activity #3 codeSystem", "1.2.752.116.1.3.2.1.4", careEventType.getActivities().getActivity().get(2).getCodeSystem());
        Assert.assertEquals("Activity #3 date", "20140202", careEventType.getActivities().getActivity().get(2).getDate());
        Assert.assertEquals("Activity #3 OrdnNr", 3, careEventType.getActivities().getActivity().get(2).getSeqNo().intValue());
        Assert.assertEquals("Activity #4 code", "A01AB13", careEventType.getActivities().getActivity().get(3).getCode());
        Assert.assertEquals("Activity #4 codeSystem", "1.2.752.129.2.2.3.1.1", careEventType.getActivities().getActivity().get(3).getCodeSystem());
        Assert.assertEquals("Activity #4 date", "20140202", careEventType.getActivities().getActivity().get(3).getDate());
        Assert.assertEquals("Activity #4 OrdnNr", 4, careEventType.getActivities().getActivity().get(3).getSeqNo().intValue());

        // Referral from (HSA-id)
        Assert.assertEquals("Referred from", "SE2321000016-1664", careEventType.getReferredFrom());

        // StayBefore
        Assert.assertEquals("StayBefore code", "1", careEventType.getStayBefore().getCode());

        // StayAfter
        Assert.assertEquals("StayAfter code", "1", careEventType.getStayAfter().getCode());

        // Deceased
        Assert.assertEquals("Deceased", false, careEventType.isDeceased());
    }
    
    @Test
    public void testDoTransformWithNonSequentialOrdnNr() throws Exception {
        // Populate the cache necessary for the mapping to function correctly.
        final CodeServerMEKCacheManagerService instance = CodeServerMEKCacheManagerService.getInstance();
        instance.revalidate();

        // Set the GVR Filter method to work with file names for this test.
        gvrFileReader.setDateFilterMethod(DateFilterMethod.FILENAME);

        // Read a given ERSMOIndata file and marshal to an XML-object.
        Path inFile = FileSystems.getDefault().getPath(gvrFileReader.getLocalPath() + "ERSMO_2014-09-04T080000.000+0000.xml");
        Reader fileReader = gvrFileReader.getReaderForFile(inFile);
        ERSMOIndataMarshaller unMarshaller = new ERSMOIndataMarshaller();
        ERSMOIndata indata = unMarshaller.unmarshal(fileReader);
        indata.getErsättningshändelse().get(0).setStartverksamhet("1234");
        indata.getErsättningshändelse().get(0).setSlutverksamhet("1234");

        // Transform to a list of RIV CareEventTypes.
        List<CareEventType> careEventList = new ArrayList<CareEventType>(); 
        ERSMOIndataToCareEventTransformer.doTransform(new RetryBin(), true, careEventList, indata.getErsättningshändelse(), gvrFileReader.getDateFromGVRFile(inFile), inFile);

        // Basically the same file as the above test except that the OrdnNr is non-sequential on some rows.
        Assert.assertEquals("Number of Care Events", 4, careEventList.size());
        
        // First event has diagnoses with non-sequential OrdnNr.
        CareEventType careEventType = careEventList.get(0);
        Assert.assertEquals("Number of diagnoses", 4, careEventType.getDiagnoses().getDiagnosis().size());
        Assert.assertEquals("Diagnosis #1 code", "J250", careEventType.getDiagnoses().getDiagnosis().get(0).getCode());
        Assert.assertEquals("Diagnosis #2 code", "I050", careEventType.getDiagnoses().getDiagnosis().get(1).getCode());
        Assert.assertEquals("Diagnosis #3 code", "K570", careEventType.getDiagnoses().getDiagnosis().get(2).getCode());
        Assert.assertEquals("Diagnosis #4 code", "A01AB13", careEventType.getDiagnoses().getDiagnosis().get(3).getCode());
        
        // Second event has activities with non-sequential OrdnNr.
        careEventType = careEventList.get(1);
        Assert.assertEquals("Number of activities", 4, careEventType.getActivities().getActivity().size());
        Assert.assertEquals("Activity #1 code", "NHP09", careEventType.getActivities().getActivity().get(0).getCode());
        Assert.assertEquals("Activity #2 code", "PE009", careEventType.getActivities().getActivity().get(1).getCode());
        Assert.assertEquals("Activity #3 code", "A01AB13", careEventType.getActivities().getActivity().get(2).getCode());
        Assert.assertEquals("Activity #4 code", "AQ014", careEventType.getActivities().getActivity().get(3).getCode());
        
        // Third event has professions, diagnoses, conditions and activities with non-sequential OrdnNr.
        careEventType = careEventList.get(2);
        
        // Professions
        Assert.assertEquals("Number of professions", 2, careEventType.getInvolvedProfessions().getProfession().size());
        Assert.assertEquals("Profession #1 code", "01", careEventType.getInvolvedProfessions().getProfession().get(0).getCode());
        Assert.assertEquals("Profession #2 code", "02", careEventType.getInvolvedProfessions().getProfession().get(1).getCode());
        
        // Diagnoses
        Assert.assertEquals("Number of diagnoses", 4, careEventType.getDiagnoses().getDiagnosis().size());
        Assert.assertEquals("Diagnosis #1 code", "I050", careEventType.getDiagnoses().getDiagnosis().get(0).getCode());
        Assert.assertEquals("Diagnosis #2 code", "K570", careEventType.getDiagnoses().getDiagnosis().get(1).getCode());
        Assert.assertEquals("Diagnosis #3 code", "J250", careEventType.getDiagnoses().getDiagnosis().get(2).getCode());
        Assert.assertEquals("Diagnosis #4 code", "A01AB13", careEventType.getDiagnoses().getDiagnosis().get(3).getCode());
        
        // Conditions
        Assert.assertEquals("Number of conditions", 3, careEventType.getConditions().getCondition().size());
        Assert.assertEquals("Condition #1 code", "ASA4", careEventType.getConditions().getCondition().get(0).getCode());
        Assert.assertEquals("Condition #2 code", "ASA5", careEventType.getConditions().getCondition().get(1).getCode());
        Assert.assertEquals("Condition #3 code", "ASA6", careEventType.getConditions().getCondition().get(2).getCode());
        
        // Activities
        Assert.assertEquals("Number of activities", 4, careEventType.getActivities().getActivity().size());
        Assert.assertEquals("Activity #1 code", "A01AB13", careEventType.getActivities().getActivity().get(0).getCode());
        Assert.assertEquals("Activity #2 code", "PE009", careEventType.getActivities().getActivity().get(1).getCode());
        Assert.assertEquals("Activity #3 code", "AQ014", careEventType.getActivities().getActivity().get(2).getCode());
        Assert.assertEquals("Activity #4 code", "NHP09", careEventType.getActivities().getActivity().get(3).getCode());
        
        // The fourth event has several diagnoses with the same OrdnNr.
        careEventType = careEventList.get(3);
        Assert.assertEquals("Number of diagnoses", 4, careEventType.getDiagnoses().getDiagnosis().size());
        Assert.assertEquals("Diagnosis #1 code", "I050", careEventType.getDiagnoses().getDiagnosis().get(0).getCode());
        Assert.assertEquals("Diagnosis #2 code", "K570", careEventType.getDiagnoses().getDiagnosis().get(1).getCode());
        Assert.assertEquals("Diagnosis #3 code", "J250", careEventType.getDiagnoses().getDiagnosis().get(2).getCode());
        Assert.assertEquals("Diagnosis #4 code", "A01AB13", careEventType.getDiagnoses().getDiagnosis().get(3).getCode());
    }

    @Test
    public void testDoTransformNonExistentKombika() throws Exception {
        // Populate the cache necessary for the mapping to function correctly.
        final CodeServerMEKCacheManagerService instance = CodeServerMEKCacheManagerService.getInstance();
        instance.revalidate();

        // Set the GVR Filter method to work with file names for this test.
        gvrFileReader.setDateFilterMethod(DateFilterMethod.FILENAME);

        // Read a given ERSMOIndata file and marshal to an XML-object.
        Path inFile = FileSystems.getDefault().getPath(gvrFileReader.getLocalPath() + "ERSMO_2014-02-02T080000.000+0000.xml");
        Reader fileReader = gvrFileReader.getReaderForFile(inFile);
        ERSMOIndataMarshaller unMarshaller = new ERSMOIndataMarshaller();
        ERSMOIndata indata = unMarshaller.unmarshal(fileReader);
        indata.getErsättningshändelse().get(0).setStartverksamhet("1234");
        indata.getErsättningshändelse().get(0).setSlutverksamhet("1234");

        // Transform to a list of RIV CareEventTypes.
        List<CareEventType> careEventList = new ArrayList<CareEventType>(); 
        ERSMOIndataToCareEventTransformer.doTransform(new RetryBin(), true, careEventList, indata.getErsättningshändelse(), gvrFileReader.getDateFromGVRFile(inFile), inFile);

        // Exactly the same file as the above test, so we only see that the local-id and contract is gone, and that the transformation doesn't freak out.
        Assert.assertEquals("Number of Care Events", 1, careEventList.size());
    }

    @Test
    public void testActivityDateBasedLookup() throws Exception {
        // Populate the cache necessary for the mapping to function correctly.
        final CodeServerMEKCacheManagerService instance = CodeServerMEKCacheManagerService.getInstance();
        instance.revalidate();

        // Set the GVR Filter method to work with file names for this test.
        gvrFileReader.setDateFilterMethod(DateFilterMethod.FILENAME);

        // Read a given ERSMOIndata file and marshal to an XML-object.
        Path inFile = FileSystems.getDefault().getPath(gvrFileReader.getLocalPath() + "ERSMO_2013-09-08T080000.000+0000.xml");
        Reader fileReader = gvrFileReader.getReaderForFile(inFile);
        ERSMOIndataMarshaller unMarshaller = new ERSMOIndataMarshaller();
        ERSMOIndata indata = unMarshaller.unmarshal(fileReader);

        // Transform to a list of RIV CareEventTypes.
        List<CareEventType> careEventList = new ArrayList<CareEventType>(); 
        ERSMOIndataToCareEventTransformer.doTransform(new RetryBin(), true, careEventList, indata.getErsättningshändelse(), gvrFileReader.getDateFromGVRFile(inFile), inFile);

        // Exactly the same file as the above test, so we only see that the local-id and contract is gone, and that the transformation doesn't freak out.
        Assert.assertEquals("Number of Care Events", 1, careEventList.size());
    }

    @Test
    public void testEndDateBasedLookup() throws Exception {
        // Populate the cache necessary for the mapping to function correctly.
        final CodeServerMEKCacheManagerService instance = CodeServerMEKCacheManagerService.getInstance();
        instance.revalidate();

        // Set the GVR Filter method to work with file names for this test.
        gvrFileReader.setDateFilterMethod(DateFilterMethod.FILENAME);

        // Read a given ERSMOIndata file and marshal to an XML-object.
        Path inFile = FileSystems.getDefault().getPath(gvrFileReader.getLocalPath() + "ERSMO_2013-09-09T080000.000+0000.xml");
        Reader fileReader = gvrFileReader.getReaderForFile(inFile);
        ERSMOIndataMarshaller unMarshaller = new ERSMOIndataMarshaller();
        ERSMOIndata indata = unMarshaller.unmarshal(fileReader);

        // Transform to a list of RIV CareEventTypes.
        List<CareEventType> careEventList = new ArrayList<CareEventType>(); 
        ERSMOIndataToCareEventTransformer.doTransform(new RetryBin(), true, careEventList, indata.getErsättningshändelse(), gvrFileReader.getDateFromGVRFile(inFile), inFile);

        // Exactly the same file as the above test, so we only see that the local-id and contract is gone, and that the transformation doesn't freak out.
        Assert.assertEquals("Number of Care Events", 1, careEventList.size());
    }

    @Test
    public void testSkippingCareEventWithFöljerMall_n() throws Exception {
        // Populate the cache necessary for the mapping to function correctly.
        final CodeServerMEKCacheManagerService instance = CodeServerMEKCacheManagerService.getInstance();
        instance.revalidate();
        
        // Set the GVR Filter method to work with file names for this test.
        gvrFileReader.setDateFilterMethod(DateFilterMethod.FILENAME);

        // Read a given ERSMOIndata file and marshal to an XML-object.
        Path inFile = FileSystems.getDefault().getPath(gvrFileReader.getLocalPath() + "ERSMO_2014-09-10T080000.000+0000.xml");
        Reader fileReader = gvrFileReader.getReaderForFile(inFile);
        ERSMOIndataMarshaller unMarshaller = new ERSMOIndataMarshaller();
        ERSMOIndata indata = unMarshaller.unmarshal(fileReader);

        // Transform to a list of RIV CareEventTypes.
        List<CareEventType> careEventList = new ArrayList<CareEventType>(); 
        ERSMOIndataToCareEventTransformer.doTransform(new RetryBin(), true, careEventList, indata.getErsättningshändelse(), gvrFileReader.getDateFromGVRFile(inFile), inFile);

        Assert.assertEquals("Number of Care Events", 0, careEventList.size());
    }

    @Test
    public void testNormalReferralHsaLookup() throws Exception {
        // Populate the cache necessary for the mapping to function correctly.
        final CodeServerMEKCacheManagerService instance = CodeServerMEKCacheManagerService.getInstance();
        instance.revalidate();
        
        // Set the GVR Filter method to work with file names for this test.
        gvrFileReader.setDateFilterMethod(DateFilterMethod.FILENAME);

        // Read a given ERSMOIndata file and marshal to an XML-object.
        Path inFile = FileSystems.getDefault().getPath(gvrFileReader.getLocalPath() + "ERSMO_2014-10-13T080000.000+0000.xml");
        Reader fileReader = gvrFileReader.getReaderForFile(inFile);
        ERSMOIndataMarshaller unMarshaller = new ERSMOIndataMarshaller();
        ERSMOIndata indata = unMarshaller.unmarshal(fileReader);

        // Transform to a list of RIV CareEventTypes.
        List<CareEventType> careEventList = new ArrayList<CareEventType>(); 
        ERSMOIndataToCareEventTransformer.doTransform(new RetryBin(), true, careEventList, indata.getErsättningshändelse(), gvrFileReader.getDateFromGVRFile(inFile), inFile);

        Assert.assertEquals("Number of Care Events", 1, careEventList.size());
        Assert.assertEquals("SE2321000016-15CQ", careEventList.get(0).getReferredFrom());
    }
    
    @Test
    public void testExpiredKombikaReferralHsaLookup() throws Exception {
        // Populate the cache necessary for the mapping to function correctly.
        final CodeServerMEKCacheManagerService instance = CodeServerMEKCacheManagerService.getInstance();
        instance.revalidate();
        
        // Set the GVR Filter method to work with file names for this test.
        gvrFileReader.setDateFilterMethod(DateFilterMethod.FILENAME);

        // Read a given ERSMOIndata file and marshal to an XML-object.
        Path inFile = FileSystems.getDefault().getPath(gvrFileReader.getLocalPath() + "ERSMO_2014-10-12T080000.000+0000.xml");
        Reader fileReader = gvrFileReader.getReaderForFile(inFile);
        ERSMOIndataMarshaller unMarshaller = new ERSMOIndataMarshaller();
        ERSMOIndata indata = unMarshaller.unmarshal(fileReader);

        // Transform to a list of RIV CareEventTypes.
        List<CareEventType> careEventList = new ArrayList<CareEventType>(); 
        ERSMOIndataToCareEventTransformer.doTransform(new RetryBin(), true, careEventList, indata.getErsättningshändelse(), gvrFileReader.getDateFromGVRFile(inFile), inFile);

        Assert.assertEquals("Number of Care Events", 1, careEventList.size());
        Assert.assertEquals("REFERRAL_HSA-643S", careEventList.get(0).getReferredFrom());
    }

    @Test
    public void testFailedKombikaReferralHsaLookup() throws Exception {
        // Populate the cache necessary for the mapping to function correctly.
        final CodeServerMEKCacheManagerService instance = CodeServerMEKCacheManagerService.getInstance();
        instance.revalidate();
        
        // Set the GVR Filter method to work with file names for this test.
        gvrFileReader.setDateFilterMethod(DateFilterMethod.FILENAME);

        // Read a given ERSMOIndata file and marshal to an XML-object.
        Path inFile = FileSystems.getDefault().getPath(gvrFileReader.getLocalPath() + "ERSMO_2014-10-14T080000.000+0000.xml");
        Reader fileReader = gvrFileReader.getReaderForFile(inFile);
        ERSMOIndataMarshaller unMarshaller = new ERSMOIndataMarshaller();
        ERSMOIndata indata = unMarshaller.unmarshal(fileReader);

        // Transform to a list of RIV CareEventTypes.
        List<CareEventType> careEventList = new ArrayList<CareEventType>(); 
        ERSMOIndataToCareEventTransformer.doTransform(new RetryBin(), true, careEventList, indata.getErsättningshändelse(), gvrFileReader.getDateFromGVRFile(inFile), inFile);

        Assert.assertEquals("Number of Care Events", 1, careEventList.size());
        Assert.assertEquals(null, careEventList.get(0).getReferredFrom());
    }

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
        final TermItem<FacilityState> avd = instance.getCurrentIndex().get(sourceFacilityId);
        FacilityState currentAvd = avd.getState(stateDate);
        final TermItemCommission<CommissionState> samverks = currentAvd.getCommissions().get(0);
        // Not really part of this test, but it never hurts.
        Assert.assertEquals("Facility ID", "9081", samverks.getId());
        Assert.assertEquals("Payer facility HSA", "SE2321000016-15CQ", TransformHelper.getPayerOrganization(Vkhform.SLUTENVÅRDSTILLFÄLLE, stateDate, currentAvd, samverks, "SE2321000016-39KJ", null, null, null, null));
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
    
}