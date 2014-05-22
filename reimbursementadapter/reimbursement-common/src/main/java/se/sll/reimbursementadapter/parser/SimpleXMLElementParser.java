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
import java.util.Map;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;


/**
 * Parses simple XML element data, like the structure below. <p>
 * <pre>
 * &lt;enclosing-element&gt;
 *  &lt;element-a&gt;some-data&lt;/element-a&gt;
 *  &lt;element-b&gt;some-data&lt;/element-b&gt;
 *  &lt;element-n&gt;some-data&lt;/element-n&gt;
 * &lt;/enclosing-element>
 * </pre>
 * 
 * The enclosing element name, and all elements of interest are registered. <p>
 * 
 * The parser invokes a callback method when a section enclosed by the given element begins and then 
 * for each matching element (with data), and finally 
 * when the enclosing element ends.
 * 
 * @see SimpleXMLElementParser.ElementMatcherCallback
 * 
 * @author Peter
 *
 */
public class SimpleXMLElementParser {
    private String file;

    /**
     * Callback interface when matching registered elements.
     * 
     * @author Peter
     *
     */
    public static interface ElementMatcherCallback {
        /**
         * Called when section with enclosed element begins.
         */
        void begin();
        
        /**
         * Registered element has been matched.
         * 
         * @param code the registered code.
         * @param data
         */
        void match(int code, String data);
        
        /**
         * Called when section with enclosed element ends.
         */
        void end();
    }

    public SimpleXMLElementParser(String file) {
        this.file = file;
    }

    public void parse(String enclosingElementName, Map<String, Integer> nameCodeMap, ElementMatcherCallback matcher) {
        try {
            parse0(enclosingElementName, nameCodeMap, matcher);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void parse0(String enclosingName, Map<String, Integer> nameCodeMap, ElementMatcherCallback matcher) throws FileNotFoundException, XMLStreamException {
        final XMLInputFactory factory = XMLInputFactory.newInstance();

        final XMLEventReader r = factory.createXMLEventReader(new BufferedInputStream(new FileInputStream(file)), "UTF-8");

        int code = 0;
        boolean match = false;
        while (r.hasNext()) {

            final XMLEvent e = r.nextEvent();

            switch (e.getEventType()) {
            case XMLEvent.START_ELEMENT:
                if (match) {
                    Integer i = nameCodeMap.get(e.asStartElement().getName().getLocalPart());
                    code = (i == null) ? Integer.MIN_VALUE : i;
                } else if (e.asStartElement().getName().getLocalPart().equals(enclosingName)) {
                    matcher.begin();
                    match = true;
                }
                break;
            case XMLEvent.CHARACTERS:
                if (match && code != Integer.MIN_VALUE) {
                    matcher.match(code, e.asCharacters().getData());
                }
                break;
            case XMLEvent.END_ELEMENT:
                if (match && e.asEndElement().getName().getLocalPart().equals(enclosingName)) {
                    match = false;
                    matcher.end();
                }
                code = 0;
                break;
            }
        }
        r.close();
    }
}
