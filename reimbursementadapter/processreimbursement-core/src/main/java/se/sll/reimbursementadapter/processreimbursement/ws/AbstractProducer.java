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

import org.apache.cxf.binding.soap.SoapFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import riv.followup.processdevelopment.reimbursement.getadministrativecareeventresponder.v1.GetAdministrativeCareEventResponse;
import riv.followup.processdevelopment.reimbursement.getadministrativecareeventresponder.v1.GetAdministrativeCareEventType;
import riv.followup.processdevelopment.reimbursement.processreimbursementresponder.v1.ProcessReimbursementRequestType;
import riv.followup.processdevelopment.reimbursement.processreimbursementresponder.v1.ProcessReimbursementResponse;
import riv.followup.processdevelopment.reimbursement.v1.CareEventType;
import riv.followup.processdevelopment.reimbursement.v1.TimePeriodMillisType;
import se.sll.ersmo.xml.indata.ERSMOIndata;
import se.sll.hej.xml.indata.HEJIndata;
import se.sll.reimbursementadapter.hej.transform.HEJIndataMarshaller;
import se.sll.reimbursementadapter.hej.transform.ReimbursementRequestToHEJIndataTransformer;
import se.sll.reimbursementadapter.processreimbursement.jmx.StatusBean;
import se.sll.reimbursementadapter.processreimbursement.service.CodeServerCacheManagerService;

import javax.annotation.Resource;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class AbstractProducer {

    private static final Logger log = LoggerFactory.getLogger("WS-API");
    private static final String SERVICE_CONSUMER_HEADER_NAME = "x-rivta-original-serviceconsumer-hsaid";

    @Autowired
    private CodeServerCacheManagerService codeServerCacheService;

    /** Handles all the JMX stuff. */
    @Autowired
    private StatusBean statusBean;

    /** Lists files matching a period and provides Readers for individual files. */
    //@Autowired
    //private GVRFileReader gvrFileReader;

    /** Reference to the JAX-WS {@link javax.xml.ws.WebServiceContext}. */
    @Resource
    @SuppressWarnings("unused")
    private WebServiceContext webServiceContext;

    /** The configured value for the maximum number of Care Events that the RIV Service should return. */
    @Value("${pr.gvr.maximumSupportedCareEvents:10000}")
    private int maximumSupportedCareEvents;

    //
    static class NotFoundException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        /**
         * Creates an exception.
         *
         * @param message the user message in plain text.
         */
        protected NotFoundException(String message) {
            super(message);
        }

    }

    public ProcessReimbursementResponse processReimbursementEvent0(ProcessReimbursementRequestType parameters) {
        ProcessReimbursementResponse response = new ProcessReimbursementResponse();
        // TODO: Fixa någon gång :)
        response.setComment("Aha!");
        response.setResultCode("OK");

        // Transformera inkommande ProcessReimbursementRequestType till motsvarande HEJIndata enligt specifikation.
        ReimbursementRequestToHEJIndataTransformer hejTransformer = new ReimbursementRequestToHEJIndataTransformer(codeServerCacheService.getCurrentIndex());
        //System.out.println("Status bean?" + statusBean.getGUID().toString());
        HEJIndata hejXml = hejTransformer.doTransform(parameters);

        try {
            Path file = Files.createFile(FileSystems.getDefault().getPath("/tmp", "hej", "out", "Ersättningshändelse_"
                    + parameters.getBatchId() + "_"
                    + (new SimpleDateFormat("yyyy'-'MM'-'dd'T'hhmmssSSS")).format(new Date()) + ".xml"));
            BufferedWriter bw = Files.newBufferedWriter(file, Charset.forName("ISO-8859-1"), StandardOpenOption.WRITE);
            HEJIndataMarshaller.unmarshalString(hejXml, bw);
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }

    /**
     * Creates a soap fault.
     *
     * @param throwable the cause.
     * @return the soap fault object.
     */
    protected SoapFault createSoapFault(Throwable throwable) {
        final String msg = createLogMessage(throwable.toString());
        log.error(msg, throwable);

        return createSoapFault(msg);
    }

    /**
     * Creates a soap fault.
     *
     * @param throwable the cause.
     * @return the soap fault object.
     */
    protected SoapFault createSoapFault(String msg, Throwable throwable) {
        log.error(msg, throwable);

        return createSoapFault(msg);
    }

    /**
     * Creates a soap fault.
     *
     * @param msg the message.
     * @return the soap fault object.
     */
    protected SoapFault createSoapFault(final String msg) {
        return new SoapFault(msg, SoapFault.FAULT_CODE_SERVER);
    }

    /**
     * Returns the actual message context.
     *
     *
     * @return the message context.
     */
    protected MessageContext getMessageContext() {
        return webServiceContext.getMessageContext();
    }

    /**
     *
     * Logs message context information.
     *
     * @param messageContext the context.
     */
    private void log(MessageContext messageContext) {
        final Map<?, ?> headers = (Map<?, ?>)messageContext.get(MessageContext.HTTP_REQUEST_HEADERS);
        log.info(createLogMessage(headers.get(SERVICE_CONSUMER_HEADER_NAME)));
        log.debug("HTTP Headers {}", headers);
    }

    /**
     * Creates a log message.
     *
     * @param msg the message.
     * @return the log message.
     */
    protected String createLogMessage(Object msg) {
        return String.format("%s - %s - \"%s\"", statusBean.getName(), statusBean.getGUID(), (msg == null) ? "NA" : msg);
    }

    /**
     * Runs a runnable in an instrumented manner.
     *
     * @param runnable the runnable to run.
     * @return the result code.
     */
    protected boolean fulfill(final Runnable runnable) {
        final MessageContext messageContext = getMessageContext();
        final String path = (String)messageContext.get(MessageContext.PATH_INFO);
        statusBean.start(path);
        log(messageContext);
        boolean status = false;
        try {
            runnable.run();
            status = true;
        } catch (NotFoundException ex) {
            status = false;
            log.error(createLogMessage(ex.getMessage()));
        } catch (Throwable throwable) {
            throw createSoapFault(throwable);
        } finally {
            statusBean.stop(status);
        }

        log.debug("stats: {}", statusBean.getPerformanceMetricsAsJSON());

        return status;
    }
}
