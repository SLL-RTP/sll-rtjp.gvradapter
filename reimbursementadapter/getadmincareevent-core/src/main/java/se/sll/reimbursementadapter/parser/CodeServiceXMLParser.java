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
package se.sll.reimbursementadapter.parser;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;


/**
 * Parses codeserver XML input based on the streaming parser (StAX) <p>
 * 
 * All attribute elements and code elements of interest are registered by names.
 * 
 * @see CodeServiceXMLParser.CodeServiceEntryCallback
 * 
 * @author Peter
 *
 */
public class CodeServiceXMLParser {

    public static final Date ONE_YEAR_BACK;
    static { 
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        ONE_YEAR_BACK = cal.getTime();
    }


    private static final String CODE_PREFIX = "c:";
    private static final String ATTR_PREFIX = "a:";
    private static final String CODE = "code";
    private static final String CODESYSTEM = "codesystem";
    private static final String CODEDVALUE = "codedvalue";
    private static final String EXTERNALLINK = "externallink";
    private static final String TYPE = "type";
    private static final String ID = "id";
    private static final String TERMITEMENTRY = "termitementry";
    private static final String ATTRIBUTE = "attribute";

    final static XMLInputFactory factory = XMLInputFactory.newInstance();

    private XMLEventReader reader;
    private Set<String> extractFilter = new HashSet<String>();
    private Map<String, QName> names = new HashMap<String, QName>();
    private CodeServiceEntryCallback codeServiceEntryCallback;
    private Date newerThan = ONE_YEAR_BACK;

    /**
     * Parsing callback interface
     */
    public static interface CodeServiceEntryCallback {
        /**
         * Called when a valid code service entry has been processed.
         * 
         * @param codeServiceEntry the entry item.
         */
        void onCodeServiceEntry(CodeServiceEntry codeServiceEntry);
    }


    public CodeServiceXMLParser(String file, CodeServiceEntryCallback codeServiceEntryCallback) {
        try {
            this.reader = factory.createXMLEventReader(new BufferedInputStream(new FileInputStream(file)), "UTF-8");
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        this.codeServiceEntryCallback = codeServiceEntryCallback;
    }

    /**
     * Returns the date that items must be newer than in order to be processed.
     * 
     * @return the date.
     */
    public Date getNewerThan() {
        return newerThan;
    }

    /**
     * Sets the date that items must be newer than in order to be processed and sent to callback method.
     *
     * @param newerThan the actual date.
     */
    public void setNewerThan(Date newerThan) {
        this.newerThan = newerThan;
    }

    /**
     * Indicate extraction of attribute.
     * 
     * @param attribute the attribute name (corresponds to type in XML)
     */
    public void extractAttribute(final String attribute) {
        extractFilter.add(ATTR_PREFIX + attribute);
    }

    /**
     * Indicates extraction of a code system. <p>
     * 
     * A code always is a sub-element (codevalue) of an attribute of type "externallink".
     * 
     * @param codeSystem the code name to extract (corresponds to codesystem in XML)
     */
    public void extractCodeSystem(final String codeSystem) {
        extractFilter.add(CODE_PREFIX + codeSystem);
    }

    /**
     * Runs the parser.
     */
    public void parse() {
        if (extractFilter.size() == 0) {
            throw new IllegalArgumentException("No attributes, nor code values have been defined");
        }
        try {
            parse0();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                reader.close();
            } catch (XMLStreamException e) {
                e.printStackTrace();
            }
        }
    }

    //
    private boolean same(StartElement startElement, String name) {
        return startElement.getName().getLocalPart().equals(name);
    }

    //
    private boolean same(EndElement endElement, String name) {
        return endElement.getName().getLocalPart().equals(name);
    }

    //
    private QName name(final String namespaceURI, final String localPart) {
        final String key = namespaceURI + localPart;
        QName qname = names.get(key);
        if (qname == null) {
            qname = new QName(namespaceURI, localPart);
            names.put(key, qname);
        }
        return qname;
    }

    //
    private String attribute(StartElement startElement, String attrName) {
        Attribute attr = startElement.getAttributeByName(name(startElement.getName().getNamespaceURI(), attrName));
        return (attr == null) ? null : attr.getValue();
    }

    //
    private void parse0() throws FileNotFoundException, XMLStreamException {
        while (reader.hasNext()) {
            final XMLEvent e = reader.nextEvent();
            switch (e.getEventType()) {
            case XMLEvent.START_ELEMENT:
                if (same(e.asStartElement(), TERMITEMENTRY)) {
                    final Date expirationDate = TermState.toDate(attribute(e.asStartElement(), "expirationdate"));
                    // avoid unnecessary processing, and check time before creating item
                    if (expirationDate.after(newerThan)) {
                        final CodeServiceEntry codeServiceEntry = processCodeServiceEntry(e.asStartElement());
                        codeServiceEntry.setValidFrom(TermState.toDate(attribute(e.asStartElement(), "begindate")));
                        codeServiceEntry.setValidTo(expirationDate);
                        codeServiceEntryCallback.onCodeServiceEntry(codeServiceEntry);
                    }
                }
                break;
            }
        }
    }


    //
    private CodeServiceEntry processCodeServiceEntry(StartElement startElement) throws XMLStreamException {
        final CodeServiceEntry codeServiceEntry = new CodeServiceEntry();
        codeServiceEntry.setId(attribute(startElement, ID));
        while (reader.hasNext()) {
            final XMLEvent e = reader.nextEvent();
            switch (e.getEventType()) {
            case XMLEvent.START_ELEMENT:
                if (same(e.asStartElement(), ATTRIBUTE)) {
                    final String name = attribute(e.asStartElement(), TYPE);
                    if (extractFilter.contains(ATTR_PREFIX + name)) {
                        processAttributeValue(name, codeServiceEntry);
                    } else if (EXTERNALLINK.equals(name)) {
                        processCodeValue(codeServiceEntry);
                    }
                }
                break;
            case XMLEvent.END_ELEMENT:
                if (same(e.asEndElement(), TERMITEMENTRY)) {
                    return codeServiceEntry;
                }
            }
        }
        return null;
    }

    private void processAttributeValue(String name,
            CodeServiceEntry state) throws XMLStreamException {
        while (reader.hasNext()) {
            final XMLEvent e = reader.nextEvent();
            switch (e.getEventType()) {
            case XMLEvent.CHARACTERS:
                state.setAttribute(name, e.asCharacters().getData());
                break;
            case XMLEvent.END_ELEMENT:
                if (same(e.asEndElement(), ATTRIBUTE)) {
                    return;
                }
                break;
            }
        }
    }


    //
    private void processCodeValue(CodeServiceEntry state) throws XMLStreamException {
        while (reader.hasNext()) {
            final XMLEvent e = reader.nextEvent();
            switch (e.getEventType()) {
            case XMLEvent.START_ELEMENT:
                if (same(e.asStartElement(), CODEDVALUE)) {
                    final String codeSystem = attribute(e.asStartElement(), CODESYSTEM);
                    if (extractFilter.contains(CODE_PREFIX + codeSystem)) {
                        state.addCode(codeSystem, attribute(e.asStartElement(), CODE));
                    }
                }
                break;
            case XMLEvent.END_ELEMENT:
                if (same(e.asEndElement(), CODEDVALUE)) {
                    return;
                } else if (same(e.asEndElement(), ATTRIBUTE)) {
                    return;
                }
                break;
            }
        }
    }

}
