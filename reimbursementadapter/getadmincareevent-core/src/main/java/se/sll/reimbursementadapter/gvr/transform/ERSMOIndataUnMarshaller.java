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

import java.io.File;
import java.io.Reader;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.xml.sax.SAXException;
import se.sll.ersmo.xml.indata.ERSMOIndata;

/**
 * Unmarshals an XML Document in the form of a String to an ERSMOIndata XML Object.
 */
public class ERSMOIndataUnMarshaller {
	
	private static final Logger LOG = LoggerFactory.getLogger(ERSMOIndataUnMarshaller.class);

    public ERSMOIndata unmarshalString(Reader src) {
        ERSMOIndata indata = null;
        try {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            URL url = getClass().getClassLoader().getResource("xsd/ERSMOIndata/ERSMOIndata2.2.xsd");
            Schema schema = sf.newSchema(url);

            JAXBContext jaxbContext = JAXBContext.newInstance(ERSMOIndata.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            unmarshaller.setSchema(schema);
            indata = (ERSMOIndata) unmarshaller.unmarshal(src);
        } catch (JAXBException e) {
            LOG.error("Error unmarshalling XML Document to ERSMOIndata XML Object.", e);
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return indata;
    }

}
