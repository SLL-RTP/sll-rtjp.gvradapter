package se.sll.reimbursementadapter.hej.transform;

import junit.framework.Assert;

import org.junit.Test;

import riv.followup.processdevelopment.reimbursement.processreimbursementresponder.v1.ProcessReimbursementRequestType;
import riv.followup.processdevelopment.reimbursement.v1.*;
import se.sll.hej.xml.indata.HEJIndata;
import se.sll.reimbursementadapter.TestSupport;
import se.sll.reimbursementadapter.processreimbursement.service.CodeServerCacheManagerService;

public class ReimbursementRequestToHEJIndataTransformerTest extends TestSupport {

    @Test
    public void testFullTransform() throws Exception {
        CodeServerCacheManagerService instance = CodeServerCacheManagerService.getInstance();
        instance.revalidate();
        ReimbursementRequestToHEJIndataTransformer transformer = new ReimbursementRequestToHEJIndataTransformer
                (instance.getCurrentIndex());

        // Base data used for populating the request and asserting the transformed HEJIndata response.
        String batchId = "BatchId";
        String sourceSystemName = "UnitTests";
        String sourceSystemOrganization = "1.2.3.4.5.6";
        String eventId = "EVENTID";
        boolean emergency = true;
        String mainEventType = "ÖPPENVÅRD";
        String mainEventTypeCodeSystem = "1.2.3.5.6";
        String subEventType = "I02+1235";
        String subEventTypeCodeSystem = "1.2.4.4.6";
        String patientId = "191212121212";
        String patientType = "1.2.4.56.7";
        String patientBirthdate = "19121212";
        String patientGender = "2";
        String patientGenderCodeSystem = "1.2.752.129.2.2.1.1";
        String patientRegion = "01";
        String patientMunicipality = "80";
        String patientParish = "01";
        String patientLocalResidence = "1210151";
        // Som mappat i Kodservern
        String patientBetjäningsområde = "132002";
        String professionCode = "01";
        String professionCodeSystem = "1.1.2.3.4";
        String activityCode = "NHP09";
        String activityCodeSystem = "1.2.3.4.5.6.7";
        String activityDate = "20140101";
        String product1Code = "A1234";
        String product1CodeSystem = "1.2.3.4.5";
        String product1CareUnitHsaId = "SE012301231";
        String product1CareUnitLocalId = "19147021M01";
        String product1CareUnitCodeSystem = "1.2.3.4.5.6";
        String product1ContractId = "6341";
        String product1ContractCodeSytem = "1.3.4.6.8";
        String product1ContractName = "Contract";
        String product1ModelCode = "003";
        String product1ModelCodeSystem = "1.2.3.4.5.6";
        String product1FromDatum = "20140101";
        String product1TomDatum = "20140206";
        String product1FbPeriod = "201402";

        // Create new ProcessReimbursementRequestType and fill it with the above data.
        ProcessReimbursementRequestType requestType = new ProcessReimbursementRequestType();
        requestType.setBatchId(batchId);
        requestType.setSourceSystem(new SourceSystemType());
        requestType.getSourceSystem().setId(sourceSystemName);
        requestType.getSourceSystem().setOrg(sourceSystemOrganization);
        ReimbursementEventType event1 = new ReimbursementEventType();
        event1.setId(eventId);
        event1.setEmergency(emergency);
        event1.setEventTypeMain(new CVType());
        event1.getEventTypeMain().setCode(mainEventType);
        event1.getEventTypeMain().setCodeSystem(mainEventTypeCodeSystem);
        event1.setEventTypeSub(new CVType());
        event1.getEventTypeSub().setCode(subEventType);
        event1.getEventTypeSub().setCodeSystem(subEventTypeCodeSystem);

        event1.setPatient(new PatientType());
        event1.getPatient().setId(new PersonIdType());
        event1.getPatient().getId().setId(patientId);
        event1.getPatient().getId().setType(patientType);
        event1.getPatient().setBirthDate(patientBirthdate);
        event1.getPatient().setGender(new CVType());
        event1.getPatient().getGender().setCode(patientGender);
        event1.getPatient().getGender().setCodeSystem(patientGenderCodeSystem);
        event1.getPatient().setResidence(new ResidenceType());
        event1.getPatient().getResidence().setRegion(new CVType());
        event1.getPatient().getResidence().getRegion().setCode(patientRegion);
        event1.getPatient().getResidence().setMunicipality(new CVType());
        event1.getPatient().getResidence().getMunicipality().setCode(patientMunicipality);
        event1.getPatient().getResidence().setParish(new CVType());
        event1.getPatient().getResidence().getParish().setCode(patientParish);
        event1.getPatient().setLocalResidence(patientLocalResidence);

        event1.setInvolvedProfessions(new ReimbursementEventType.InvolvedProfessions());
        ProfessionType profession1 = new ProfessionType();
        profession1.setCode(professionCode);
        profession1.setCodeSystem(professionCodeSystem);
        event1.getInvolvedProfessions().getProfession().add(profession1);

        event1.setActivities(new ReimbursementEventType.Activities());
        ActivityType activity1 = new ActivityType();
        activity1.setCode(activityCode);
        activity1.setDate(activityDate);
        event1.getActivities().getActivity().add(activity1);

        // Create a new <productSet>
        ReimbursementEventType.ProductSet productSet = new ReimbursementEventType.ProductSet();

        // Create a new <product> 'product1' to be added in the product set
        ProductType product1 = new ProductType();

        // Create the Product1 <code> type
        CVType product1CodeType = new CVType();
        product1CodeType.setCode(product1Code);
        product1CodeType.setCodeSystem(product1CodeSystem);
        product1.setCode(product1CodeType);

        // Create the Product 1 <careUnit> type
        CareUnitType product1CareUnitType = new CareUnitType();
        product1CareUnitType.setCareUnitId(product1CareUnitHsaId);
        product1CareUnitType.setCareUnitLocalId(new IIType());
        product1CareUnitType.getCareUnitLocalId().setExtension(product1CareUnitLocalId);
        product1CareUnitType.getCareUnitLocalId().setRoot(product1CareUnitCodeSystem);
        product1.setCareUnit(product1CareUnitType);

        // Create the Product 1 <contract> type
        SimpleContractType product1CareContractType = new SimpleContractType();
        product1CareContractType.setId(new IIType());
        product1CareContractType.getId().setRoot(product1ContractCodeSytem);
        product1CareContractType.getId().setExtension(product1ContractId);
        product1CareContractType.setName(product1ContractName);
        product1.setContract(product1CareContractType);

        // Create the Product 1 <model> type
        product1.setModel(new CVType());
        product1.getModel().setCode(product1ModelCode);
        product1.getModel().setCodeSystem(product1ModelCodeSystem);

        // Set the product dates.
        product1.setDatePeriod(new DatePeriodType());
        product1.getDatePeriod().setStart(product1FromDatum);
        product1.getDatePeriod().setEnd(product1TomDatum);

        // Add the product1 to the productSet.
        productSet.getProduct().add(product1);

        // Add the productSet to the event1
        event1.getProductSet().add(productSet);

        requestType.getReimbursementEvent().add(event1);


        // Do the transformation
        HEJIndata indata = transformer.doTransform(requestType, 1000);


        // Asserts
        Assert.assertEquals("BatchId -> Indata ID", batchId, indata.getID());
        Assert.assertEquals("Source System name", sourceSystemName, indata.getKälla());

        HEJIndata.Ersättningshändelse ersh = indata.getErsättningshändelse().get(0);
        Assert.assertNotNull(ersh);

        Assert.assertEquals("CareEvent ID", eventId, ersh.getID());

        // Patient
        Assert.assertEquals("Patient ID", patientId, ersh.getPatient().getID());
        Assert.assertEquals("Patient region", patientRegion, ersh.getPatient().getLkf().substring(0, 2));
        Assert.assertEquals("Patient municipality", patientMunicipality, ersh.getPatient().getLkf().substring(2, 4));
        Assert.assertEquals("Patient parish", patientParish, ersh.getPatient().getLkf().substring(4, 6));
        Assert.assertEquals("Patient Geographical area", patientLocalResidence, ersh.getPatient().getBasområde());
        Assert.assertEquals("Patient medical services area", "01" + patientBetjäningsområde, ersh.getKundKod());

        // Produktomgång
        Assert.assertEquals("Produktomgång typ", "Utförare", ersh.getProduktomgång().get(0).getTyp());
        Assert.assertEquals("Produkt1 kod", product1Code, ersh.getProduktomgång().get(0).getProdukt().get(0).getKod());
        Assert.assertEquals("Produkt1 uppdrag", product1ContractId, ersh.getProduktomgång().get(0).getProdukt().get(0).getUppdrag());
        Assert.assertEquals("Produkt1 fromdatum", product1FromDatum, ersh.getProduktomgång().get(0).getProdukt().get(0).getFromDatum());
        Assert.assertEquals("Produkt1 tomdatum", product1TomDatum, ersh.getProduktomgång().get(0).getProdukt().get(0).getTomDatum());
        Assert.assertEquals("Produkt1 fbperiod", product1FbPeriod, ersh.getProduktomgång().get(0).getProdukt().get(0).getFbPeriod());

        // Yrkeskategori
        Assert.assertEquals("Yrkeskategori 1", professionCode, ersh.getYrkeskategorier().getYrkeskategori().get(0).getKod());
        Assert.assertEquals("Yrkeskategori 1 OrdNr", "1", ersh.getYrkeskategorier().getYrkeskategori().get(0).getOrdnNr());

        // Åtgärd
        Assert.assertEquals("Åtgärd 1", activityCode, ersh.getÅtgärder().getÅtgärd().get(0).getKod());
        Assert.assertEquals("Åtgärd 1 OrdNr", "1", ersh.getÅtgärder().getÅtgärd().get(0).getOrdnNr());
        Assert.assertEquals("Åtgärd 1 Datum", activityDate, ersh.getÅtgärder().getÅtgärd().get(0).getDatum());
    }

    @Test
    public void testNullRequest() throws Exception {
        CodeServerCacheManagerService instance = CodeServerCacheManagerService.getInstance();
        ReimbursementRequestToHEJIndataTransformer transformer = new ReimbursementRequestToHEJIndataTransformer
                (instance.getCurrentIndex());
        try {
            transformer.doTransform(null, 2);
            Assert.fail("No exception thrown");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }
}