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

import java.io.Reader;
import java.io.Writer;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import se.sll.ersmo.xml.indata.ERSMOIndata;

/**
 * Unmarshals an XML Document in the form of a String to an ERSMOIndata XML Object.
 */
public class ERSMOIndataMarshaller {
    
    public ERSMOIndata unmarshal(Reader src) throws SAXException, JAXBException {
        // Read the schema from the XSD to apply the validation to the unmarshalled XML object.
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        URL url = getClass().getClassLoader().getResource("xsd/ERSMOIndata/ERSMOIndata2.2.xsd");
        Schema schema = sf.newSchema(url);

        // Create a new JAXB Unmarshaller for ERSMOIndata and unmarshal the source XML.
        JAXBContext jaxbContext = JAXBContext.newInstance(ERSMOIndata.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        unmarshaller.setSchema(schema);

        return (ERSMOIndata) unmarshaller.unmarshal(src);
    }

    public void marshal(ERSMOIndata xml, Writer writer) throws SAXException, JAXBException {
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        URL url = getClass().getClassLoader().getResource("xsd/ERSMOIndata/ERSMOIndata2.2.xsd");
        Schema schema = sf.newSchema(url);
        JAXBContext jaxbContext = JAXBContext.newInstance(ERSMOIndata.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty("jaxb.encoding", "ISO-8859-1");
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setSchema(schema);
        marshaller.marshal(xml, writer);
    }
}
