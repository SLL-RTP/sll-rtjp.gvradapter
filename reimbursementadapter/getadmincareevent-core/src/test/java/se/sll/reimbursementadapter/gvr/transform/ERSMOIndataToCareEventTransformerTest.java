package se.sll.reimbursementadapter.gvr.transform;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import riv.followup.processdevelopment.reimbursement.v1.CareEventType;
import riv.followup.processdevelopment.reimbursement.v1.GenderType;
import se.sll.ersmo.xml.indata.ERSMOIndata;
import se.sll.reimbursementadapter.TestSupport;
import se.sll.reimbursementadapter.admincareevent.service.CodeServerMEKCacheManagerService;
import se.sll.reimbursementadapter.gvr.reader.DateFilterMethod;
import se.sll.reimbursementadapter.gvr.reader.GVRFileReader;

import java.io.Reader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;

/**
 * Tests the ERSMOIndataToCareEventTransformer with different
 * transformation scenarios using the test filles in the project.
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
        Path inFile = FileSystems.getDefault().getPath(gvrFileReader.getLocalPath() + "Vardkontakt_2014-02-02T100000.xml");
        Reader fileReader = gvrFileReader.getReaderForFile(inFile);
        ERSMOIndata indata = ERSMOIndataUnMarshaller.unmarshalString(fileReader);

        // Transform to a list of RIV CareEventTypes.
        List<CareEventType> careEventList = ERSMOIndataToCareEventTransformer.doTransform(indata, gvrFileReader.getDateFromGVRFile(inFile));

        // Validate contents of transformed RIV objects.
        Assert.assertEquals("Number of Care Events", 1, careEventList.size());
        CareEventType careEventType = careEventList.get(0);

        Assert.assertEquals("ID#1", "12345678901234567890", careEventType.getId());

        // Source system
        Assert.assertEquals("Source System organization", "1.2.752.97.??", careEventType.getSourceSystem().getOrganization());
        Assert.assertEquals("Source System Name", "GVR", careEventType.getSourceSystem().getName());

        // Patient
        Assert.assertEquals("Patient ID", "191212121212", careEventType.getPatient().getId().getId());
        Assert.assertEquals("Patient ID Type", "1.2.752.129.2.1.3.1", careEventType.getPatient().getId().getType());
        Assert.assertEquals("Patient Birthdate", "19121212", careEventType.getPatient().getBirthDate());
        Assert.assertEquals("Patient Gender", GenderType.M, careEventType.getPatient().getGender());
        Assert.assertEquals("Patient Residency region", "01", careEventType.getPatient().getResidency().getRegion());
        Assert.assertEquals("Patient Residency municipality", "02", careEventType.getPatient().getResidency().getMunicipality());
        Assert.assertEquals("Patient Residency parish", "03", careEventType.getPatient().getResidency().getParish());

        // Emergency
        Assert.assertEquals("Emergency", true, careEventType.isEmergency());

        // Event type
        Assert.assertEquals("Event Main Type", "ÖPPENVÅRDSKONTAKT", careEventType.getEventType().getMainType().getOriginalText());
        Assert.assertEquals("Event Sub Type", "1+013", careEventType.getEventType().getSubType().getOriginalText());

        // Fee category
        Assert.assertEquals("Fee Category", "006", careEventType.getFeeCategory().getOriginalText());

        // Contracts
        Assert.assertEquals("Number of contracts", 1, careEventType.getContracts().getContract().size());
        Assert.assertEquals("Contract #1 id", "9081", careEventType.getContracts().getContract().get(0).getId().getExtension());
        Assert.assertEquals("Contract #1 type name", "Ryggkirurgi, vårdval", careEventType.getContracts().getContract().get(0).getContractType().getDisplayName().trim());
        Assert.assertEquals("Contract #1 type code", "615", careEventType.getContracts().getContract().get(0).getContractType().getOriginalText());
        Assert.assertEquals("Contract #1 type providerOrg", "30216311002", careEventType.getContracts().getContract().get(0).getProviderOrganization());

        // Care Unit (spine center öv)
        Assert.assertEquals("Kombika", "30216311002", careEventType.getCareUnit().getCareUnitLocalId().getCode());
        Assert.assertEquals("Care Unit HSA Id", "SE2321000016-15CQ", careEventType.getCareUnit().getCareUnitHsaId());

        // Updated time (taken from the filename)
        Assert.assertEquals("UpdatedTime", "20140202100000000", careEventType.getLastUpdatedTime());

        // Deleted
        Assert.assertEquals("Deleted", false, careEventType.isDeleted());

        // Date Period
        Assert.assertEquals("Date Period start", "20140202", careEventType.getDatePeriod().getStartDate());
        Assert.assertEquals("Time Period start", null, careEventType.getDatePeriod().getStartTime());
        Assert.assertEquals("Date Period end", "20140203", careEventType.getDatePeriod().getEndDate());
        Assert.assertEquals("Time Period end", null, careEventType.getDatePeriod().getEndTime());

        // Involved Professions
        Assert.assertEquals("Profession count", 2, careEventType.getInvolvedProfessions().getProfession().size());
        Assert.assertEquals("Profession #1 code","01", careEventType.getInvolvedProfessions().getProfession().get(0).getCode());
        Assert.assertEquals("Profession 21 code", "02", careEventType.getInvolvedProfessions().getProfession().get(1).getCode());

        // Diagnoses
        Assert.assertEquals("Number of diagnoses", 3, careEventType.getDiagnoses().getDiagnosis().size());
        Assert.assertEquals("Diagnosis #1 code", "I050", careEventType.getDiagnoses().getDiagnosis().get(0).getCode());
        Assert.assertEquals("Diagnosis #1 codeSystem", "1.2.752.116.1.1.1.1.3", careEventType.getDiagnoses().getDiagnosis().get(0).getCodeSystem());
        Assert.assertEquals("Diagnosis #2 code", "J250", careEventType.getDiagnoses().getDiagnosis().get(1).getCode());
        Assert.assertEquals("Diagnosis #2 codeSystem", "1.2.752.116.1.1.1.1.3", careEventType.getDiagnoses().getDiagnosis().get(1).getCodeSystem());
        Assert.assertEquals("Diagnosis #3 code", "K570", careEventType.getDiagnoses().getDiagnosis().get(2).getCode());
        Assert.assertEquals("Diagnosis #3 codeSystem", "1.2.752.116.1.1.1.1.3", careEventType.getDiagnoses().getDiagnosis().get(2).getCodeSystem());

        // Conditions
        Assert.assertEquals("Number of conditions", 1, careEventType.getConditions().getCondition().size());
        Assert.assertEquals("Condition #1 code", "ASA6", careEventType.getConditions().getCondition().get(0).getCode());

        // Activities
        Assert.assertEquals("Number of activities", 4, careEventType.getActivities().getActivity().size());
        Assert.assertEquals("Activity #1 code", "NHP09", careEventType.getActivities().getActivity().get(0).getCode());
        Assert.assertEquals("Activity #1 codeSystem", "1.2.752.116.1.3.2.1.4", careEventType.getActivities().getActivity().get(0).getCodeSystem());
        Assert.assertEquals("Activity #2 code", "PE009", careEventType.getActivities().getActivity().get(1).getCode());
        Assert.assertEquals("Activity #2 codeSystem", "1.2.752.116.1.3.2.1.4", careEventType.getActivities().getActivity().get(1).getCodeSystem());
        Assert.assertEquals("Activity #3 code", "AQ014", careEventType.getActivities().getActivity().get(2).getCode());
        Assert.assertEquals("Activity #3 codeSystem", "1.2.752.116.1.3.2.1.4", careEventType.getActivities().getActivity().get(2).getCodeSystem());
        Assert.assertEquals("Activity #4 code", "A01AB13", careEventType.getActivities().getActivity().get(3).getCode());
        Assert.assertEquals("Activity #4 codeSystem", "1.2.752.129.2.2.3.1.1", careEventType.getActivities().getActivity().get(3).getCodeSystem());

        // Referred from
        Assert.assertEquals("Referred from", "1.2.752.97.??:17101011M04", careEventType.getReferredFrom());

        // StayBefore
        Assert.assertEquals("StayBefore code", "1", careEventType.getStayBefore().getCode());

        // StayAfter
        Assert.assertEquals("StayAFter code", "1", careEventType.getStayAfter().getCode());

        // Deceased
        Assert.assertEquals("Deceased", false, careEventType.isDeceased());

    }
}