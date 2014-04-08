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

import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sll.hej.xml.indata.HEJIndata;

/**
 * Marshals a HEJIndata XML Object to a string.
 */
public class HEJIndataMarshaller {
	
	private static final Logger LOG = LoggerFactory.getLogger(HEJIndataMarshaller.class);

    /**
     * Marshals the HEJIndata to a string, and writes the string to the provided writer.
     *
     * @param src The HEJIndata object to marshall.
     * @param writer The Writer to write the result to.
     */
    public static void unmarshalString(HEJIndata src, Writer writer) {
        LOG.info("Starting to unmarshal contents of source Reader.");
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(HEJIndata.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty("jaxb.encoding", "ISO-8859-1");
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(src, writer);
        } catch (JAXBException e) {
            LOG.error("Error unmarshalling XML Document to ERSMOIndata XML Object.", e);
        }
    }

}
