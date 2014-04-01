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
import riv.followup.processdevelopment.reimbursement.processreimbursementresponder.v1.ProcessReimbursementRequestType;
import riv.followup.processdevelopment.reimbursement.processreimbursementresponder.v1.ProcessReimbursementResponse;
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
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Abstract producer for the ProcessReimbursementEvent service. Implements and isolates the actual logic for the
 * other shell producers.
 */
public class AbstractProducer {

    /** The Logger. */
    private static final Logger log = LoggerFactory.getLogger("WS-API");
    /** The service consumer header name to report via JMX. */
    private static final String SERVICE_CONSUMER_HEADER_NAME = "x-rivta-original-serviceconsumer-hsaid";

    /** The pre initialized Cache service to use in the mapping process. */
    @Autowired
    private CodeServerCacheManagerService codeServerCacheService;

    /** Handles all the JMX stuff. */
    @Autowired
    private StatusBean statusBean;

    /** Reference to the JAX-WS {@link javax.xml.ws.WebServiceContext}. */
    @Resource
    private WebServiceContext webServiceContext;

    /** The configured value for the maximum number of Care Events that the RIV Service allows. */
    //@Value("${pr.riv.maximumSupportedCareEvents:10000}")
    //private int maximumSupportedCareEvents;

    /** The path where HEJ should write its files. */
    @Value("${pr.hej.outPath:/tmp/hej/out}")
    private String hejFileOutputPath;

    /** The prefix for the created files. */
    @Value("${pr.hej.filePrefix:HEJIndata-}")
    private String hejFilePrefix;

    /** The suffix for the created files. */
    @Value("${pr.hej.fileSuffix:.xml}")
    private String hejFileSuffix;

    /** Number of retries when writing files to disk. */
    @Value("${pr.hej.io.numRetries:10}")
    private int hejNumberOfRetries;

    /** The delay between retries when writing files to disk. */
    @Value("${pr.hej.io.retryInterval:100}")
    private long hejRetryInterval;

    /**
     * Simple exception used by #fullfill().
     */
    private static class NotFoundException extends RuntimeException {
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

    /**
     * Creates a complete {@link ProcessReimbursementResponse} object from the request information taken from the
     * provided 'parameters' parameter.
     *
     * @param parameters a filled in {@link ProcessReimbursementRequestType} object with the request parameters from
     *                   the WS service.
     * @return a completely transformed {@link ProcessReimbursementResponse} object.
     */
    public ProcessReimbursementResponse processReimbursementEvent0(ProcessReimbursementRequestType parameters) {
        log.trace("Entering AbstractProducer.processReimbursementEvent0");
        ProcessReimbursementResponse response = new ProcessReimbursementResponse();
        response.setResultCode("OK");

        // Transforms the incoming ProcessReimbursementRequestType to the equivivalent HEJIndata according to the
        // specification (TODO: version?)
        ReimbursementRequestToHEJIndataTransformer hejTransformer =
                new ReimbursementRequestToHEJIndataTransformer(codeServerCacheService.getCurrentIndex());
        HEJIndata hejXml = hejTransformer.doTransform(parameters);

        // TODO catch exception from the transformer when the number of care events are met.

        // Try writing the files according to the configured values, retrying if it fails with an IOException.
        BufferedWriter bw = null;
        for (int currentTry = 0; currentTry < hejNumberOfRetries; currentTry++) {
            try {
                // Create a file according to the configured pattern.
                Path file = Files.createFile(FileSystems.getDefault().getPath(hejFileOutputPath,
                     hejFilePrefix
                     + parameters.getBatchId() + "_" + (new SimpleDateFormat("yyyy'-'MM'-'dd'T'hhmmssSSS")).format(new Date())
                     + hejFileSuffix
                ));
                // Create a new buffered writer connected to the file with the correct Charset.
                bw = Files.newBufferedWriter(file, Charset.forName("ISO-8859-1"), StandardOpenOption.WRITE);
                // Unmarshal the transformed HEJ XML directly into the created BufferedWriter.
                HEJIndataMarshaller.unmarshalString(hejXml, bw);
                // If no exception, do not retry. (could probably just return here aswell)
                break;
            } catch (IOException e) {
                log.error("IOException when writing the result file to disk.", e);
                response.setResultCode("ERROR");

                // If this is not the last try, and we have a configured hejRetryInterval that is not 0,
                // sleep for a bit.
                if ((hejNumberOfRetries - currentTry) > 1) {
                    if (hejRetryInterval > 0) {
                        try {
                            log.debug("Sleeping for " + hejRetryInterval + " milliseconds.");
                            Thread.sleep(hejRetryInterval);
                        } catch (InterruptedException e1) {
                        }
                    }
                }
            } finally {
                try {
                    if (bw != null) {
                        bw.flush();
                        bw.close();
                    }
                } catch (IOException e) {
                    log.error("IOException when closing BufferedWriter", e);
                }
            }
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
     * @return the message context.
     */
    protected MessageContext getMessageContext() {
        return webServiceContext.getMessageContext();
    }

    /**
     * Logs message context information.
     *
     * @param messageContext the context.
     */
    private void log(MessageContext messageContext) {
        final Map<?, ?> headers = (Map<?, ?>) messageContext.get(MessageContext.HTTP_REQUEST_HEADERS);
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
        return String.format("%s - %s - \"%s\"", statusBean.getName(), statusBean.getGUID(),
                (msg == null) ? "NA" : msg);
    }

    /**
     * Runs a runnable in an instrumented manner.
     *
     * @param runnable the runnable to run.
     * @return the result code.
     */
    protected boolean fulfill(final Runnable runnable) {
        final MessageContext messageContext = getMessageContext();
        final String path = (String) messageContext.get(MessageContext.PATH_INFO);
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
