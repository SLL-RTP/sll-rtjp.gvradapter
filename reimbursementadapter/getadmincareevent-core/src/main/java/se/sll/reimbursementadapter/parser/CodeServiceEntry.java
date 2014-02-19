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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * Holds entry state for a code server entry during XML parsing.
 * 
 * @author Peter
 *
 */
public class CodeServiceEntry extends TermState implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private Map<String, String> attributes = new HashMap<String, String>();
    private Map<String, List<String>> codes = new HashMap<String, List<String>>();
    
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setAttribute(String name, String value) {
        attributes.put(name, value);
    }

    public void addCode(String name, String value) {
        List<String> l = codes.get(name);
        if (l == null) {
            l = new ArrayList<String>();
            codes.put(name, l);
        }
        l.add(value);
    }

    public String getAttribute(String name) {
        return attributes.get(name);
    }

    public List<String> getCodes(String name) {
        return codes.get(name);
    }
}

