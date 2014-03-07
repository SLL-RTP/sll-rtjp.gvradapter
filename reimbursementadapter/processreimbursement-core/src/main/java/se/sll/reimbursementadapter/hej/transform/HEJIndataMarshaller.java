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
package se.sll.reimbursementadapter.hej.transform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sll.ersmo.xml.indata.ERSMOIndata;
import se.sll.hej.xml.indata.HEJIndata;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * Unmarshals an XML Document in the form of a String to an ERSMOIndata XML Object.
 */
public class HEJIndataMarshaller {
	
	private static final Logger log = LoggerFactory.getLogger(HEJIndataMarshaller.class);

    public static void unmarshalString(HEJIndata src, Writer writer) {
        log.debug("Starting to unmarshal contents of source Reader.");
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(HEJIndata.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty("jaxb.encoding", "ISO-8859-1");
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(src, writer);
        } catch (JAXBException e) {
            log.error("Error unmarshalling XML Document to ERSMOIndata XML Object.", e);
        }
    }

}
