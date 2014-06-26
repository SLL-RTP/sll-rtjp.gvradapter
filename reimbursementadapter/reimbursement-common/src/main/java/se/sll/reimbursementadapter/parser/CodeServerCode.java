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

/**
 * Model class for holding information about a single Code Server code.
 */
public class CodeServerCode {

    /** The value of the code. */
    private String value;
    /** The reference id for the code. */
    private String referenceId;

    /**
     * Gets the value of the code.
     * @return a String with the value of the code.
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the code.
     * @param value the value of the code.
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the reference id for the code.
     * @return a String with the reference id for the code.
     */
    public String getReferenceId() {
        return referenceId;
    }

    /**
     * Sets the reference id for the code.
     * @param referenceId the reference id for the code.
     */
    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }
}
