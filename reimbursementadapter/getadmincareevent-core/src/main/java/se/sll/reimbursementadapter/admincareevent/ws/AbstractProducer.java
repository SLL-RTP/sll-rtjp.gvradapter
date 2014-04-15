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
package se.sll.reimbursementadapter.admincareevent.ws;

import org.apache.cxf.binding.soap.SoapFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import riv.followup.processdevelopment.reimbursement.getadministrativecareeventresponder.v1
        .GetAdministrativeCareEventResponse;
import riv.followup.processdevelopment.reimbursement.getadministrativecareeventresponder.v1
        .GetAdministrativeCareEventType;
import riv.followup.processdevelopment.reimbursement.v1.CareEventType;
import riv.followup.processdevelopment.reimbursement.v1.TimePeriodXSType;
import se.sll.ersmo.xml.indata.ERSMOIndata;
import se.sll.reimbursementadapter.admincareevent.jmx.StatusBean;
import se.sll.reimbursementadapter.exception.NotFoundException;
import se.sll.reimbursementadapter.gvr.reader.GVRFileReader;
import se.sll.reimbursementadapter.gvr.transform.ERSMOIndataToCareEventTransformer;
import se.sll.reimbursementadapter.gvr.transform.ERSMOIndataUnMarshaller;

import javax.annotation.Resource;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import java.io.Reader;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Abstract producer for the GetAdministrativeCareEvent service. Implements and isolates the actual logic for the
 * other shell producers.
 */
public class AbstractProducer {

    private static final Logger LOG = LoggerFactory.getLogger("WS-API");
    private static final String SERVICE_CONSUMER_HEADER_NAME = "x-rivta-original-serviceconsumer-hsaid";

    /** Handles all the JMX stuff. */
    @Autowired
    private StatusBean statusBean;

    /** Lists files matching a period and provides Readers for individual files. */
    @Autowired
    private GVRFileReader gvrFileReader;

    /** Reference to the JAX-WS {@link javax.xml.ws.WebServiceContext}. */
    @Resource
    private WebServiceContext webServiceContext;

    /** The configured value for the maximum number of Care Events that the RIV Service should return. */
    @Value("${pr.riv.maximumSupportedCareEvents:10000}")
    private int maximumSupportedCareEvents;

    /**
     * Creates a GetAdministrativeCareEventResponse from the provided GetAdministrativeCareEventType parameter.
     * Used by {@link se.sll.reimbursementadapter.admincareevent.ws.GetAdministrativeCareEventProducer}.
     *
     * @param parameters The incoming parameters from the RIV service.
     * @return A complete GetAdministrativeCareEventResponse.
     */
    protected GetAdministrativeCareEventResponse getAdministrativeCareEvent0(GetAdministrativeCareEventType
                                                                                     parameters) {
        GetAdministrativeCareEventResponse response = new GetAdministrativeCareEventResponse();
        response.setResultCode("OK");
        response.setResponseTimePeriod(new TimePeriodXSType());
        response.getResponseTimePeriod().setStart(parameters.getUpdatedDuringPeriod().getStart());
        response.getResponseTimePeriod().setEnd(parameters.getUpdatedDuringPeriod().getEnd());

        List<Path> pathList = null;
        try {
            pathList = gvrFileReader.getFileList(parameters.getUpdatedDuringPeriod().getStart().toGregorianCalendar().getTime(),
                    parameters.getUpdatedDuringPeriod().getEnd().toGregorianCalendar().getTime());
        } catch (Exception e) {
            LOG.error("Error when listing files in GVR directory", e);
            throw createSoapFault("Internal error when listing files in GVR directory", e);
            //response.setResultCode("ERROR");
            //response.setComment("Internal error in the service when reading files from disk: " + e.getMessage());
            //return response;
        }

        Date currentDate = null;

        for (Path currentFile : pathList) {
            currentDate = gvrFileReader.getDateFromGVRFile(currentFile);

            try (Reader fileContent = gvrFileReader.getReaderForFile(currentFile)) {
                ERSMOIndata xmlObject = ERSMOIndataUnMarshaller.unmarshalString(fileContent);

                // Transform all the Ersättningshändelse within the object to CareEventType and add them to the
                // response.
                List<CareEventType> careEventList = ERSMOIndataToCareEventTransformer.doTransform(xmlObject,
                        currentDate);

                if ((careEventList.size() + response.getCareEvent().size()) > maximumSupportedCareEvents) {
                    // Truncate response if we reached the configured limit for care events in the response.
                    if (response.getCareEvent().size() == 0) {
                        // If we have been truncated due to a overly large first file we set the end response
                        // period to the start of the request to indicate that nothing was processed.
                        response.getResponseTimePeriod().setEnd(parameters.getUpdatedDuringPeriod().getStart());
                    } else {
                        response.getResponseTimePeriod().setEnd(response.getCareEvent().get(response.getCareEvent()
                                .size() - 1).getLastUpdatedTime());
                    }
                    response.setResultCode("TRUNCATED");
                    response.setComment("Response was truncated due to hitting the maximum configured number of Care " +
                            "Events of " + maximumSupportedCareEvents);
                    return response;
                }

                response.getCareEvent().addAll(careEventList);
            } catch (Exception e) {
                LOG.error("Error when creating Reader for file: " + currentFile.getFileName(), e);
                throw createSoapFault("Internal error when creating Reader for file: " + currentFile.getFileName(), e);
                //response.setResultCode("ERROR");
                //response.setComment("Internal error in the service when reading a source file: " + e.getMessage());
                //return response;
            }
        }

        if (response.getCareEvent().size() > 0) {
            response.getResponseTimePeriod().setEnd(response.getCareEvent().get(response.getCareEvent().size() - 1)
                    .getLastUpdatedTime());
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
        LOG.error(msg, throwable);

        return createSoapFault(msg);
    }

    /**
     * Creates a soap fault.
     *
     * @param throwable the cause.
     * @return the soap fault object.
     */
    protected SoapFault createSoapFault(String msg, Throwable throwable) {
        LOG.error(msg, throwable);

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
        LOG.info(createLogMessage(headers.get(SERVICE_CONSUMER_HEADER_NAME)));
        LOG.debug("HTTP Headers {}", headers);
    }

    /**
     * Creates a LOG message.
     *
     * @param msg the message.
     * @return the LOG message.
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
            LOG.error(createLogMessage(ex.getMessage()));
        } catch (Exception exception) {
            throw createSoapFault(exception);
        } finally {
            statusBean.stop(status);
        }

        LOG.debug("stats: {}", statusBean.getPerformanceMetricsAsJSON());

        return status;
    }
}
