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
 */
public class CodeServiceEntry extends TermState implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private Map<String, String> attributes = new HashMap<>();
    private Map<String, List<CodeServerCode>> codes = new HashMap<>();
    
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setAttribute(String name, String value) {
        attributes.put(name, value);
    }

    public void addCode(String name, String value, String referenceId) {
        List<CodeServerCode> codesForCodeSystem = codes.get(name);
        if (codesForCodeSystem == null) {
            codesForCodeSystem = new ArrayList<>();
            codes.put(name, codesForCodeSystem);
        }
        CodeServerCode code = new CodeServerCode();
        code.setValue(value);
        code.setReferenceId(referenceId);

        codesForCodeSystem.add(code);
    }

    public String getAttribute(String name) {
        return attributes.get(name);
    }

    public List<CodeServerCode> getCodes(String name) {
        return codes.get(name);
    }
}

