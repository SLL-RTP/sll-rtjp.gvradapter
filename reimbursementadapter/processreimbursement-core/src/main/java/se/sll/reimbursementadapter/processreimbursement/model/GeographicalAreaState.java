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
package se.sll.reimbursementadapter.processreimbursement.model;

import java.io.Serializable;

import se.sll.reimbursementadapter.parser.TermState;

/**
 * Geographical area state. The id corresponds to "basområdeskod".
 */
public class GeographicalAreaState extends TermState implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * The list of medicalServiceArea.
     * @serial
     */
    private String medicalServiceArea = null;

    /**
     * Returns the Medical Services Area connected to this Geographical area.
     * @return the Medical Services Area connected to this Geographical area.
     */
    public String getMedicalServiceArea() {
        return medicalServiceArea;
    }

    /**
     * Sets the Medical Services Area connected to this Geographical area.
     * @param medicalServiceArea The Medical Services Area to set.
     */
    public void setMedicalServiceArea(String medicalServiceArea) {
        this.medicalServiceArea = medicalServiceArea;
    }
}