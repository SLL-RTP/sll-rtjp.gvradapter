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

import junit.framework.Assert;
import org.apache.cxf.binding.soap.SoapFault;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import riv.followup.processdevelopment.reimbursement.processreimbursementresponder.v1.*;
import riv.followup.processdevelopment.reimbursement.v1.*;
import se.sll.hej.xml.indata.HEJIndata;
import se.sll.reimbursementadapter.hej.transform.HEJIndataUnMarshaller;
import se.sll.reimbursementadapter.processreimbursement.service.CodeServerCacheManagerService;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;

/**
 * Tests the ProcessReimbursementProducer via AbstractProducer.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:processreimbursement-core-spring-context.xml")
public class ProcessReimbursementProducerTest extends AbstractProducer {

    // Base data used for populating the request and asserting the transformed HEJIndata response.
    private String batchId = "BatchId";
    private String sourceSystemName = "UnitTests";
    private String eventSystemName = "GVR";
    private String sourceSystemOrganization = "1.2.3.4.5.6";
    private String eventId = "EVENTID";
    private boolean emergency = true;
    private String mainEventType = "ÖPPENVÅRD";
    private String mainEventTypeCodeSystem = "1.2.3.5.6";
    private String subEventType = "I02+1235";
    private String subEventTypeCodeSystem = "1.2.4.4.6";
    private String patientId = "191212121212";
    private String patientType = "1.2.4.56.7";
    private String patientBirthdate = "19121212";
    private String patientGender = "2";
    private String patientGenderCodeSystem = "1.2.752.129.2.2.1.1";
    private String patientRegion = "01";
    private String patientMunicipality = "80";
    private String patientParish = "01";
    private String patientLocalResidence = "1210151";

    // Som mappat i Kodservern
    private String patientBetjäningsområde = "132002";
    private String professionCode = "01";
    private String professionCodeSystem = "1.1.2.3.4";
    private String activityCode = "NHP09";
    private String activityCodeSystem = "1.2.3.4.5.6.7";
    private String activityDate = "20140101";
    private String product1Code = "A1234";
    private String product1CodeSystem = "1.2.3.4.5";
    private String product1CareUnitHsaId = "SE012301231";
    private String product1CareUnitLocalId = "19147021M01";
    private String product1CareUnitCodeSystem = "1.2.3.4.5.6";
    private String product1ContractId = "6341";
    private String product1ContractCodeSytem = "1.3.4.6.8";
    private String product1ContractName = "Contract";
    private String product1ModelCode = "003";
    private String product1ModelCodeSystem = "1.2.3.4.5.6";
    private String product1FromDatum = "20140101";
    private String product1TomDatum = "20140206";
    private String product1FbPeriod = "201402";

    /**
     * Does a full RIV call to the service and reads the written file on disk and
     * verifies that the contents seems to be correct (does not test the entire transformation).
     *
     * @throws Exception e
     */
    @Test
    public void testProcessReimbursement() throws Exception {
        CodeServerCacheManagerService instance = CodeServerCacheManagerService.getInstance();
        instance.revalidate();

        ProcessReimbursementResponse response = processReimbursementEvent0(createRequestType(1));

        // Read the file that the service wrote to disk.
        BufferedReader bufferedReader = Files.newBufferedReader(getLastWrittenFile(), Charset.forName("ISO-8859-1"));

        // Unmarshall the file to a HEJIndata object.
        HEJIndataUnMarshaller unMarshaller = new HEJIndataUnMarshaller();
        HEJIndata xmlObject = unMarshaller.unmarshalString(bufferedReader);

        // Assertions (just do a few selected tests from the beginning and the end, the transformation itself
        // is already tested in those respective tests).
        Assert.assertEquals("Number of care events", 1, xmlObject.getErsättningshändelse().size());
        Assert.assertEquals("Patient ID", patientId, xmlObject.getErsättningshändelse().get(0).getPatient().getID());
        Assert.assertEquals("Patient LKF", patientRegion + patientMunicipality + patientParish, xmlObject.getErsättningshändelse().get(0).getPatient().getLkf());
        Assert.assertEquals("Source system name", sourceSystemName, xmlObject.getKälla());
        Assert.assertEquals("Batch id", batchId, xmlObject.getID());

        bufferedReader.close();
    }

    /**
     * Does a full RIV call to the service and reads the written file on disk and
     * verifies that the contents seems to be correct (does not test the entire transformation).
     *
     * @throws Exception e
     */
    @Test
    public void testProcessReimbursementMultipleEvents() throws Exception {
        int numberOfCareEvents = 5;
        CodeServerCacheManagerService instance = CodeServerCacheManagerService.getInstance();
        instance.revalidate();

        ProcessReimbursementResponse response = processReimbursementEvent0(createRequestType(numberOfCareEvents));

        // Read the file that the service wrote to disk.
        BufferedReader bufferedReader = Files.newBufferedReader(getLastWrittenFile(), Charset.forName("ISO-8859-1"));

        // Unmarshall the file to a HEJIndata object.
        HEJIndataUnMarshaller unMarshaller = new HEJIndataUnMarshaller();
        HEJIndata xmlObject = unMarshaller.unmarshalString(bufferedReader);

        // Assertions (just do a few selected tests from the beginning and the end, the transformation itself
        // is already tested in those respective tests).
        Assert.assertEquals("Number of care events", numberOfCareEvents, xmlObject.getErsättningshändelse().size());

        bufferedReader.close();
    }

    /**
     * Empty request, checking that the method returns a SoapFault as expected.s
     * @throws Exception e
     */
    @Test
    public void testProcessReimbursementEmptyRequest() throws Exception {
        CodeServerCacheManagerService instance = CodeServerCacheManagerService.getInstance();
        instance.revalidate();

        //ProcessReimbursementRequestType req = createRequestType();

        boolean soapException = false;
        try {
            ProcessReimbursementResponse response = processReimbursementEvent0(new ProcessReimbursementRequestType());
        } catch (SoapFault e) {
            e.printStackTrace();
            soapException = true;
        }

        if (!soapException) { Assert.fail("No SOAP validation exception thrown with an invalid outbound HEJIndata-XML"); };
    }

    /**
     * Request with empty sub element, checking that the method returns a SoapFault as expected.s
     * @throws Exception e
     */
    @Test
    public void testProcessReimbursementEmptyPatientRequest() throws Exception {
        CodeServerCacheManagerService instance = CodeServerCacheManagerService.getInstance();
        instance.revalidate();

        ProcessReimbursementRequestType req = createRequestType(1);
        // Null out the patient tag.
        req.getReimbursementEvent().get(0).setPatient(null);

        boolean soapException = false;
        try {
            ProcessReimbursementResponse response = processReimbursementEvent0(req);
        } catch (SoapFault e) {
            e.printStackTrace();
            soapException = true;
        }

        if (!soapException) { Assert.fail("No SOAP validation exception thrown with an invalid outbound HEJIndata-XML"); };
    }

    /**
     * Does a schematically invalid RIV call to the service and checks for exceptions.
     * @throws Exception e
     */
    @Test
    public void testProcessReimbursementSchemaValidationFail() throws Exception {
        CodeServerCacheManagerService instance = CodeServerCacheManagerService.getInstance();
        instance.revalidate();

        ProcessReimbursementRequestType req = createRequestType(1);
        // we poison the data a bit to trigger a validaton error.
        req.getReimbursementEvent().get(0).setPatient(null);

        boolean exception = false;
        try {
            ProcessReimbursementResponse response = processReimbursementEvent0(req);
        } catch (SoapFault e) {
            exception = true;
        }

        if (!exception) { Assert.fail("No SOAP validation exception thrown with an invalid outbound HEJIndata-XML"); };
    }

    private ProcessReimbursementRequestType createRequestType(int numberOfEvents) {
        riv.followup.processdevelopment.reimbursement.processreimbursementresponder.v1.ObjectFactory rivOf
                = new riv.followup.processdevelopment.reimbursement.processreimbursementresponder.v1.ObjectFactory();
        ProcessReimbursementRequestType requestType = rivOf.createProcessReimbursementRequestType();

        requestType.setBatchId(batchId);
        requestType.setSourceSystem(new SourceSystemType());
        requestType.getSourceSystem().setId(sourceSystemName);
        requestType.getSourceSystem().setOrg(sourceSystemOrganization);
        for (int x = 0; x < numberOfEvents; x++) {
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
        }
        return requestType;
    }
}
