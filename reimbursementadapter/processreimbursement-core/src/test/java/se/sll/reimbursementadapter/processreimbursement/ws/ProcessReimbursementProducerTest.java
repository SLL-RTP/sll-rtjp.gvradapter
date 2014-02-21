package se.sll.reimbursementadapter.processreimbursement.ws;

import org.junit.Test;
import riv.followup.processdevelopment.reimbursement.processreimbursementresponder.v1.ProcessReimbursementRequestType;
import riv.followup.processdevelopment.reimbursement.processreimbursementresponder.v1.ProcessReimbursementResponse;
import riv.followup.processdevelopment.reimbursement.v1.ObjectFactory;
import riv.followup.processdevelopment.reimbursement.v1.ReimbursementEventType;

/**
 * Created by erja on 2014-02-21.
 */
public class ProcessReimbursementProducerTest {
    @Test
    public void testProcessReimbursement() throws Exception {
        riv.followup.processdevelopment.reimbursement.processreimbursementresponder.v1.ObjectFactory rivOf = new riv.followup.processdevelopment.reimbursement.processreimbursementresponder.v1.ObjectFactory();
        ObjectFactory of = new ObjectFactory();
        ProcessReimbursementProducer test = new ProcessReimbursementProducer();

        ProcessReimbursementRequestType req = rivOf.createProcessReimbursementRequestType();
        req.setBatchId("1");
        req.setSourceSystem(of.createSourceType());
        req.getSourceSystem().setName("haha");
        req.getSourceSystem().setOrganization("1.2.3.5");
        ReimbursementEventType event = of.createReimbursementEventType();
        event.setPatient(of.createPatientType());
        
        ProcessReimbursementResponse response = test.processReimbursement("SE01203123", req);


    }
}
