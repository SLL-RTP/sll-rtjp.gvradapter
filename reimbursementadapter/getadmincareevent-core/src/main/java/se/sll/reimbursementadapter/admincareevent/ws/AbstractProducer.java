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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.annotation.Resource;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
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
import se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse;
import se.sll.reimbursementadapter.exception.NotFoundException;
import se.sll.reimbursementadapter.exception.TransformationException;
import se.sll.reimbursementadapter.gvr.RetryBin;
import se.sll.reimbursementadapter.gvr.reader.GVRFileReader;
import se.sll.reimbursementadapter.gvr.transform.ERSMOIndataMarshaller;
import se.sll.reimbursementadapter.gvr.transform.ERSMOIndataToCareEventTransformer;
import se.sll.reimbursementadapter.gvr.transform.TransformHelper;

/**
 * Abstract producer for the GetAdministrativeCareEvent service. Implements and isolates the actual logic for the
 * other shell producers.
 */
public class AbstractProducer {

    private static final Logger LOG = LoggerFactory.getLogger("WS-API");
    //private static final String SERVICE_CONSUMER_HEADER_NAME = "x-rivta-original-serviceconsumer-hsaid";

    public static final String RETRY_LOCK = "SLL_REIMBURSEMENT_RETRY_LOCK";
    
    /** Handles all the JMX stuff. */
    // #245 Fix and re-add the statusBean code.
    //@Autowired
    //private StatusBean statusBean;

    /** Lists files matching a period and provides Readers for individual files. */
    @Autowired
    public GVRFileReader gvrFileReader;

    @Autowired
    public RetryBin retryBin;
    
    /** Reference to the JAX-WS {@link javax.xml.ws.WebServiceContext}. */
    @Resource
    private WebServiceContext webServiceContext;

    /**
     * The maximum number of care events that the service can read from new
     * files in a single request, since the service is required to send all 
     * care events in one source file in the same request this value can not 
     * be set too low.
     * 
     *  NOTE: The number of events in the response can exceed this number by
     *  use of old care events in the retry bin.
     */
    @Value("${pr.riv.maximumNewEvents:5000}")
    protected int maximumNewEvents;

    /**
     * Creates a GetAdministrativeCareEventResponse from the provided GetAdministrativeCareEventType parameter.
     * Used by {@link se.sll.reimbursementadapter.admincareevent.ws.GetAdministrativeCareEventProducer}.
     *
     * @param parameters The incoming parameters from the RIV service.
     * @return A complete GetAdministrativeCareEventResponse.
     */
    protected GetAdministrativeCareEventResponse getAdministrativeCareEvent0(GetAdministrativeCareEventType parameters) 
    {

        // Make sure no two threads are doing this at the same time, we are keeping state in the retry bin file.
        synchronized (RETRY_LOCK) {
            
            // Set up the incoming dates with proper timezone + DST information. (Java < 8 is not fantastic at handling this stuff)
            DateTimePeriodType requestPeriod = parameters.getUpdatedDuringPeriod();
            Date startDate = getLocalizedDate(requestPeriod.getStart());
            Date endDate = getLocalizedDate(requestPeriod.getEnd());
            LOG.info(String.format("Request received, from  %s to date %s, max new %d, indata dir %s.", 
                                   requestPeriod.getStart().normalize().toXMLFormat(), requestPeriod.getEnd().normalize().toXMLFormat(), 
                                   maximumNewEvents, gvrFileReader.localPath));

            // List all the GVR files between the start- and end dates in the configured incoming directory
            List<Path> pathList;
            try {
                pathList = gvrFileReader.getFileList(startDate, endDate);
            } catch (Exception e) {
                return errorResponse("Error when listing files in GVR directory.", e);
            }

            try {
                retryBin.load();
            }
            catch (JAXBException | SAXException | FileNotFoundException e) {
                return errorResponse(String.format("Error when loading retry bin: %s", e.getMessage()), e);
            }

            // Create the response list object.
            List<CareEventType> careEventList = new ArrayList<CareEventType>();
            Date fileUpdatedTime = null;
            String resultCode = "OK";
            String responseComment = "All known new care events translated and returned.";

            // Iterate over each file and process it. (convert to RIV format and insert into response)
            for (Path currentFile : pathList) {
                // Get a reader for the current file, read it and then Unmarshal it into a generated ERSMOIndata object.
                ERSMOIndata ersmoIndata;
                try (Reader fileContent = gvrFileReader.getReaderForFile(currentFile)) {
                    ERSMOIndataMarshaller unmarshaller = new ERSMOIndataMarshaller();
                    ersmoIndata = unmarshaller.unmarshal(fileContent);
                } catch (IOException e) {
                    return errorResponse("Error when creating Reader for file: " + currentFile.getFileName(), e);
                } 
                catch (SAXException e) {
                    return errorResponse("Error when loading schema file for ERSOMIndata", e);
                } 
                catch (JAXBException e) {
                    return errorResponse("JAXB Error when parsing " + currentFile.getFileName() + ", is the XML Invalid?", e);
                }

                fileUpdatedTime = gvrFileReader.getDateFromGVRFile(currentFile);

                List<Ersättningshändelse> ershList = ersmoIndata.getErsättningshändelse();

                // Calculate whether we should break now or not.

                if (ershList.size() + careEventList.size() > maximumNewEvents) {
                    if (careEventList.size() == 0) {
                        return errorResponse(String.format("ERSMOIndata from file (%s) is too big (%d events) for maximumNewEvents (%d), reconfigure it!",
                                                           currentFile.getFileName(), ershList.size(), maximumNewEvents), null);
                    }
                    resultCode = "TRUNCATED";
                    responseComment = String.format("Response was truncated due to hitting maximumNewEvents config at %d.", maximumNewEvents);  
                    break;
                }

                // Transform all the Ersättningshändelse within the object to CareEventType and add them to the
                // response.
                String källa = ersmoIndata.getKälla();
                if (!TransformHelper.SLL_GVR_SOURCE.equals(källa)) {
                    return errorResponse(String.format("Unexpected källa %s when parsing %s.", källa, currentFile.getFileName()), null);
                }

                try {
                    boolean addLookupFails = true;
                    ERSMOIndataToCareEventTransformer.doTransform(retryBin, addLookupFails, careEventList, ershList, fileUpdatedTime, currentFile);
                }
                catch (TransformationException | DatatypeConfigurationException e) {
                    return errorResponse(String.format("Exception when parsing %s: %s", currentFile.getFileName(), e.getMessage()), e);
                } 
            }

            if (careEventList.size() > 0) {
                retryBin.discardOld(fileUpdatedTime);

                // Add from retry bin to response. We only want to piggyback on a response with new entries because we do want to use the fileUpdateTime to not
                // send too new care events from the retry bin (in case someone requests time intervals backwards). The ersh in the retry bin itself has faked
                // fileUpdateTime of +1 ms from the original ersh.
                try {
                    boolean addLookupFails = false;
                    ERSMOIndataToCareEventTransformer.doTransform(retryBin, addLookupFails, careEventList, retryBin.getOld(fileUpdatedTime),
                                                                  null, retryBin.getCurrentFile());
                }
                catch (TransformationException | DatatypeConfigurationException e) {
                    return errorResponse(String.format("Exception when parsing %s: %s", retryBin.getCurrentFile(), e.getMessage()), e);
                }
            }       

            try {
                retryBin.acceptNewAndSave();
            }
            catch (IOException | JAXBException | SAXException e) {
                return errorResponse(String.format("Failed to save new retry bin: %s", e.getMessage()), e);
            }

            // Create the response.
            
            DateTimePeriodType responsePeriod = new DateTimePeriodType();
            responsePeriod.setStart(requestPeriod.getStart());
            XMLGregorianCalendar maxLastUpdatedTime = requestPeriod.getStart();
            for (CareEventType careEvent : careEventList) {
                XMLGregorianCalendar lastUpdatedTime = careEvent.getLastUpdatedTime();
                if (maxLastUpdatedTime == null || lastUpdatedTime.compare(maxLastUpdatedTime) == DatatypeConstants.GREATER) {
                    maxLastUpdatedTime = lastUpdatedTime;
                }
            }
            responsePeriod.setEnd(maxLastUpdatedTime);
            
            GetAdministrativeCareEventResponse response = new GetAdministrativeCareEventResponse();
            response.setResponseTimePeriod(responsePeriod);
            response.setResultCode(resultCode);
            response.setComment(responseComment);
            response.getCareEvent().addAll(careEventList);
            
            LOG.info(String.format("Responding with %d events from %s to %s.", 
                                   careEventList.size(), responsePeriod.getStart().normalize().toXMLFormat(), responsePeriod.getEnd().normalize().toXMLFormat()));

            return response;
        }
    }

    /**
     * Create an error response that is not a soap exception.
     *
     * @param comment The friendly explanation of the error.
     * @param e The exception itself.
     * @return A fully populated {GetAdministrativeCareEventResponse} object.
     */
    private GetAdministrativeCareEventResponse errorResponse(String comment, Exception e)
    {
        if (e == null) {
            LOG.error("Responding with error: " + comment);
        }
        else {
            LOG.error("Responding with error, logging cause: " + comment, e);
        }
        DateTimePeriodType responsePeriod = new DateTimePeriodType();
        try {
            responsePeriod.setStart(RetryBin.dateToXmlCal(new Date(0)));
            responsePeriod.setEnd(RetryBin.dateToXmlCal(new Date(0)));
        } catch (DatatypeConfigurationException ex) {
            throw new RuntimeException("This is a bug that we can not handle.", ex);
        }
        
        GetAdministrativeCareEventResponse response = new GetAdministrativeCareEventResponse();
        response.setResultCode("ERROR");
        response.setComment(comment);
        response.setResponseTimePeriod(responsePeriod);
        return response;
    }

    /**
     * Creates a localized Date object from the incoming {@link XMLGregorianCalendar}.
     *
     * @param sourceCalendar the source XMLGregorianCalendar to convert to the local system time zone.
     * @return A localized Date matching the incoming {@link XMLGregorianCalendar}.
     */
    private Date getLocalizedDate(XMLGregorianCalendar sourceCalendar) {
        GregorianCalendar gregorianCalendarStart = sourceCalendar.toGregorianCalendar();
        GregorianCalendar localCalendarStart = new GregorianCalendar();
        localCalendarStart.setTimeInMillis(gregorianCalendarStart.getTimeInMillis());
        return new Date(localCalendarStart.getTime().getTime());
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
    /*
    private void log(MessageContext messageContext) {
        final Map<?, ?> headers = (Map<?, ?>) messageContext.get(MessageContext.HTTP_REQUEST_HEADERS);
        LOG.info(createLogMessage(headers.get(SERVICE_CONSUMER_HEADER_NAME)));
        LOG.debug("HTTP Headers {}", headers);
    }
     */   
   

    /**
     * Creates a LOG message.
     */
    protected String createLogMessage(Object msg) {
        return String.format("%s", msg);
        //return String.format("%s - %s - \"%s\"", statusBean.getName(), statusBean.getGUID(),
        //        (msg == null) ? "NA" : msg);
    }

    /**
     * Runs a runnable in an instrumented manner.
     *
     * @param runnable the runnable to run.
     * @return the result code.
     */
    protected boolean fulfill(final Runnable runnable) {
        //final MessageContext messageContext = getMessageContext();
        //final String path = (String) messageContext.get(MessageContext.PATH_INFO);
        //statusBean.start(path);
        //log(messageContext);
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
            //statusBean.stop(status);
        }

        //LOG.debug("stats: {}", statusBean.getPerformanceMetricsAsJSON());

        return status;
    }

    public GVRFileReader getGvrFileReader() {
        return gvrFileReader;
    }
}