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
package se.sll.reimbursementadapter.gvr.transform;

import java.io.IOException;
import java.io.Reader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sll.ersmo.xml.indata.ERSMOIndata;

/**
 * Unmarshals an XML Document in the form of a String to an ERSMOIndata XML Object.
 */
public class ERSMOIndataUnMarshaller {
	
	private static final Logger log = LoggerFactory.getLogger(ERSMOIndataUnMarshaller.class);

    public static ERSMOIndata unmarshalString(Reader src) {
        log.debug("Starting to unmarshal contents of source Reader.");
        ERSMOIndata indata = null;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ERSMOIndata.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            indata = (ERSMOIndata) unmarshaller.unmarshal(src);
        } catch (JAXBException e) {
            log.error("Error unmarshalling XML Document to ERSMOIndata XML Object.", e);
        } finally {
            if (src != null) {
                try {
                    src.close();
                } catch (IOException e) {
                    log.error("Unexpected IOException when closing BufferedReader.", e);
                }
            }
        }
        return indata;
    }

}
