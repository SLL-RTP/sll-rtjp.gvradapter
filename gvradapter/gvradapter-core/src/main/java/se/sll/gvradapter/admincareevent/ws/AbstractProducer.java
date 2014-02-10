package se.sll.gvradapter.admincareevent.ws;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.interceptor.Fault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import riv.followup.processdevelopment.getadministrativecareeventresponder._1.GetAdministrativeCareEventType;
import riv.followup.processdevelopment.v1.CareEventType;
import se.sll.ersmo.xml.indata.ERSMOIndata;
import se.sll.gvradapter.admincareevent.service.CodeServerMEKCacheManagerService;
import se.sll.gvradapter.gvr.reader.GVRFileReader;
import se.sll.gvradapter.gvr.transform.ERSMOIndataToReimbursementEventTransformer;
import se.sll.gvradapter.gvr.transform.ERSMOIndataUnMarshaller;
import se.sll.gvradapter.jmx.StatusBean;

import javax.annotation.Resource;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by erja on 2014-02-10.
 */
public class AbstractProducer {

    private static final Logger log = LoggerFactory.getLogger("WS-API");
    private static final String SERVICE_CONSUMER_HEADER_NAME = "x-rivta-original-serviceconsumer-hsaid";

    @Autowired
    private StatusBean statusBean;

    @Autowired
    private CodeServerMEKCacheManagerService codeServerMEKCacheManagerService;

    @Autowired
    private GVRFileReader gvrFileReader;

    @Resource
    private WebServiceContext webServiceContext;

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

    public List<CareEventType> getAdministrativeCareEvent0(GetAdministrativeCareEventType parameters) {
        List<CareEventType> responseList = new ArrayList<CareEventType>();
        for (Path currentFile : gvrFileReader.getFileList(parameters.getDate())) {
            // (TODO: Convert to stream instead of a String response)
            String fileContent = gvrFileReader.readFile(currentFile);

            // Unmarshal the incoming file content to an ERSMOIndata.
            ERSMOIndata xmlObject;
            try {
                xmlObject = ERSMOIndataUnMarshaller.unmarshalString(fileContent);
            } catch (Exception e) {
                throw new Fault(e);
            }

            // Transform all the Ersättningshändelse within the object to CareEventType and add them to the response.
            List<CareEventType> careEventList = ERSMOIndataToReimbursementEventTransformer.doTransform(xmlObject);
            responseList.addAll(careEventList);
        }

        return responseList;
    }

    /**
     * Returns the mapping service.
     *
     * @return the service instance.
     */
    protected CodeServerMEKCacheManagerService getCodeServerMEKCacheManagerService() {
        return codeServerMEKCacheManagerService;
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

        final SoapFault soapFault = createSoapFault(msg);

        return soapFault;
    }

    /**
     * Creates a soap fault.
     *
     * @param msg the message.
     * @return the soap fault object.
     */
    protected SoapFault createSoapFault(final String msg) {
        final SoapFault soapFault = new SoapFault(msg, SoapFault.FAULT_CODE_SERVER);
        return soapFault;
    }


    /**
     * Returns status bean.
     *
     * @return the status bean.
     */
    protected StatusBean getStatusBean() {
        return statusBean;
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

    public static XMLGregorianCalendar toTime(Date date) {
        if (date == null) {
            return null;
        }
        try {
            final GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(date);
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a {@link Date} date and time representation.
     *
     * @param cal the actual date and time.
     * @return the {@link Date} representation.
     */
    public static Date toDate(XMLGregorianCalendar cal) {
        return (cal == null) ? null : cal.toGregorianCalendar().getTime();
    }

    //
    protected static boolean contains(String[] list, String id) {
        for (final String e : list) {
            if (id.equals(e)) {
                return true;
            }
        }
        return false;
    }
}
