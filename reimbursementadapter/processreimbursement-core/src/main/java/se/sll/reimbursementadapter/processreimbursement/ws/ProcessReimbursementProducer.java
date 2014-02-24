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

import riv.followup.processdevelopment.reimbursement.processreimbursement.v1.rivtabp21.ProcessReimbursementResponderInterface;
import riv.followup.processdevelopment.reimbursement.processreimbursementresponder.v1.ProcessReimbursementRequestType;
import riv.followup.processdevelopment.reimbursement.processreimbursementresponder.v1.ProcessReimbursementResponse;
import se.sll.hej.xml.indata.HEJIndata;
import se.sll.hej.xml.indata.ObjectFactory;
import se.sll.reimbursementadapter.hej.transform.HEJIndataMarshaller;
import se.sll.reimbursementadapter.hej.transform.ReimbursementRequestToHEJIndataTransformer;

import javax.jws.WebParam;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ProcessReimbursementProducer implements ProcessReimbursementResponderInterface {

    @Override
    public ProcessReimbursementResponse processReimbursement(@WebParam(partName = "LogicalAddress", name = "LogicalAddress",
            targetNamespace = "urn:riv:itintegration:registry:1", header = true) String logicalAddress,
                                                             @WebParam(partName = "parameters", name = "ProcessReimbursementRequest",
            targetNamespace = "urn:riv:followup:processdevelopment:reimbursement:ProcessReimbursementResponder:1") ProcessReimbursementRequestType parameters) {
        ProcessReimbursementResponse response = new ProcessReimbursementResponse();
        response.setComment("Aha!");
        response.setResultCode("OK");

        HEJIndata hejXml = ReimbursementRequestToHEJIndataTransformer.doTransform(parameters);

        try {
            Path file = Files.createFile(FileSystems.getDefault().getPath("/tmp", "hej", "out", "Ersättningshändelse_"
                            + parameters.getBatchId() + "_"
                            + (new SimpleDateFormat("yyyy'-'MM'-'dd'T'hhmmssSSS")).format(new Date()) + ".xml"));
            BufferedWriter bw = Files.newBufferedWriter(file, Charset.defaultCharset(), StandardOpenOption.WRITE);
            HEJIndataMarshaller.unmarshalString(hejXml, bw);
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }

    public static void main(String[] args) {
        try {
            HEJIndata indata = new HEJIndata();
            indata.setID("testid");
            indata.setKälla("Test");
            HEJIndata.Ersättningshändelse test = new HEJIndata.Ersättningshändelse();
            test.setID("123");
            test.setKälla("ERA");
            test.setHändelseform("test");
            test.setKundKod("sets");
            indata.getErsättningshändelse().add(test);
            Path file = Files.createFile(FileSystems.getDefault().getPath("/tmp", "hej", "out", "Ersättningshändelse_" + "01" + "_" + (new SimpleDateFormat("yyyy'-'MM'-'dd'T'hhmmssSSS")).format(new Date()) + ".xml"));
            BufferedWriter bw = Files.newBufferedWriter(file, Charset.defaultCharset(), StandardOpenOption.WRITE);
            HEJIndataMarshaller.unmarshalString(indata, bw);
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
