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

import se.sll.hej.xml.indata.HEJIndata;

/**
 * Unmarshals an XML Document in the form of a String to a HEJIndata XML Object.
 */
public class HEJIndataUnMarshaller {
    
    private static final Logger LOG = LoggerFactory.getLogger(HEJIndata.class);

    public HEJIndata unmarshalString(Reader src) throws SAXException, JAXBException {
        LOG.info("Entering HEJIndataUnMarshaller with Reader: " + src);

        // Read the schema from the XSD to apply the validation to the unmarshalled XML object.
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        URL url = getClass().getClassLoader().getResource("xsd/HEJIndata/HEJIndata2.0.xsd");
        Schema schema = sf.newSchema(url);

        // Create a new JAXB Unmarshaller for ERSMOIndata and unmarshal the source XML.
        JAXBContext jaxbContext = JAXBContext.newInstance(HEJIndata.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        unmarshaller.setSchema(schema);

        return (HEJIndata) unmarshaller.unmarshal(src);
    }

}
