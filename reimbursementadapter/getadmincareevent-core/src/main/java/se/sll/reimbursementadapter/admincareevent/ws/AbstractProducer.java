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

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.*;

import javax.annotation.Resource;
import javax.xml.bind.JAXBException;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.apache.cxf.binding.soap.SoapFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.xml.sax.SAXException;
import riv.followup.processdevelopment.reimbursement.getadministrativecareeventresponder.v1.GetAdministrativeCareEventResponse;
import riv.followup.processdevelopment.reimbursement.getadministrativecareeventresponder.v1.GetAdministrativeCareEventType;
import riv.followup.processdevelopment.reimbursement.v1.CareEventType;
import riv.followup.processdevelopment.reimbursement.v1.DateTimePeriodType;
import se.sll.ersmo.xml.indata.ERSMOIndata;
import se.sll.reimbursementadapter.admincareevent.jmx.StatusBean;
import se.sll.reimbursementadapter.exception.NotFoundException;
import se.sll.reimbursementadapter.gvr.reader.GVRFileReader;
import se.sll.reimbursementadapter.gvr.transform.ERSMOIndataToCareEventTransformer;
import se.sll.reimbursementadapter.gvr.transform.ERSMOIndataUnMarshaller;
import se.sll.reimbursementadapter.exception.TransformationException;

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
    protected int maximumSupportedCareEvents;

    /**
     * Creates a GetAdministrativeCareEventResponse from the provided GetAdministrativeCareEventType parameter.
     * Used by {@link se.sll.reimbursementadapter.admincareevent.ws.GetAdministrativeCareEventProducer}.
     *
     * @param parameters The incoming parameters from the RIV service.
     * @return A complete GetAdministrativeCareEventResponse.
     */
    protected GetAdministrativeCareEventResponse getAdministrativeCareEvent0(GetAdministrativeCareEventType
                                                                                     parameters) {
        // Set up the incoming dates with proper timezone + DST information. (Java < 8 is not fantastic at handling this stuff)
        GregorianCalendar gregorianCalendarStart = parameters.getUpdatedDuringPeriod().getStart().toGregorianCalendar();
        GregorianCalendar gregorianCalendarEnd = parameters.getUpdatedDuringPeriod().getEnd().toGregorianCalendar();

        GregorianCalendar localCalendarStart = new GregorianCalendar();
        localCalendarStart.setTimeInMillis(gregorianCalendarStart.getTimeInMillis());
        GregorianCalendar localCalendarEnd = new GregorianCalendar();
        localCalendarEnd.setTimeInMillis(gregorianCalendarEnd.getTimeInMillis());

        Date startDate = new Date(localCalendarStart.getTime().getTime() + localCalendarStart.getTimeZone().getDSTSavings());
        Date endDate = new Date(localCalendarEnd.getTime().getTime() + localCalendarEnd.getTimeZone().getDSTSavings());

        LOG.info(String.format("Request recieved, from date: %s to date: %s", startDate, endDate));

        // Start setting up the response
        GetAdministrativeCareEventResponse response = new GetAdministrativeCareEventResponse();
        response.setResultCode("OK");
        response.setResponseTimePeriod(new DateTimePeriodType());
        response.getResponseTimePeriod().setStart(parameters.getUpdatedDuringPeriod().getStart());
        if (parameters.getUpdatedDuringPeriod() != null) {
            response.getResponseTimePeriod().setEnd(parameters.getUpdatedDuringPeriod().getEnd());
        }

        // List all the GVR files between the start- and end dates in the configured incoming directory
        List<Path> pathList;
        try {
            pathList = gvrFileReader.getFileList(startDate, endDate);
        } catch (Exception e) {
            LOG.error("Error when listing files in GVR directory", e);
            throw createSoapFault("Internal error when listing files in GVR directory", e);
        }

        // Iterate over each file and process it. (convert to RIV format and insert into response)
        Date currentDate;
        for (Path currentFile : pathList) {
            currentDate = gvrFileReader.getDateFromGVRFile(currentFile);

            // Get a reader for the current file, read it and then Unmarshall it into a generated ERSMOIndata object.
            ERSMOIndata xmlObject;
            try (Reader fileContent = gvrFileReader.getReaderForFile(currentFile)) {
                ERSMOIndataUnMarshaller unmarshaller = new ERSMOIndataUnMarshaller();
                xmlObject = unmarshaller.unmarshalString(fileContent);
            } catch (IOException e) {
                LOG.error("Error when creating Reader for file: " + currentFile.getFileName(), e);
                throw createSoapFault("Internal error when creating Reader for file: " + currentFile.getFileName(), e);
            } catch (SAXException e) {
                LOG.error("Error when loading schema file for ERSOMIndata", e);
                throw createSoapFault("Internal error when loading schema file for ERSOMIndata", e);
            } catch (JAXBException e) {
                LOG.error("JAXB Error when parsing " + currentFile.getFileName() + ", is the XML Invalid?", e);
                throw createSoapFault("JAXB Error when parsing the source file " + currentFile.getFileName() + ", is the XML invalid?", e);
            }

            // Transform all the Ersättningshändelse within the object to CareEventType and add them to the
            // response.
            List<CareEventType> careEventList;
            try {
                careEventList = ERSMOIndataToCareEventTransformer.doTransform(xmlObject, currentDate, currentFile);
            } catch (TransformationException e) {
                LOG.error(String.format("TransformationException when parsing %s: %s", currentFile.getFileName(), e.getMessage()), e);
                throw createSoapFault(String.format("Internal transformation error when parsing %s, Cause: %s", currentFile.getFileName(), e.getMessage()), e);
            }

            // If the current size of the response list plus the list that we want to add now is larger
            // than the maximumSupportedCareEvents, we need to truncate the response.
            if ((careEventList.size() + response.getCareEvent().size()) > maximumSupportedCareEvents) {
                // Truncate response if we reached the configured limit for care events in the response.
                if (response.getCareEvent().size() == 0) {
                    // If we have been truncated due to a overly large first file we set the end response
                    // period to the start of the request to indicate that nothing was processed.
                    // This is such a special case that we return an ERROR code.
                    response.getResponseTimePeriod().setEnd(parameters.getUpdatedDuringPeriod().getStart());
                    response.setResultCode("ERROR");
                    response.setComment("Response was truncated at 0 due to hitting the maximum configured number of Care " +
                            "Events of " + maximumSupportedCareEvents + " in the first input file.");
                } else {
                    response.getResponseTimePeriod().setEnd(response.getCareEvent().get(response.getCareEvent()
                            .size() - 1).getLastUpdatedTime());
                    response.setResultCode("TRUNCATED");
                    response.setComment("Response was truncated due to hitting the maximum configured number of Care " +
                            "Events of " + maximumSupportedCareEvents);
                }

                return response;
            }

            response.getCareEvent().addAll(careEventList);
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

    public GVRFileReader getGvrFileReader() {
        return gvrFileReader;
    }
}