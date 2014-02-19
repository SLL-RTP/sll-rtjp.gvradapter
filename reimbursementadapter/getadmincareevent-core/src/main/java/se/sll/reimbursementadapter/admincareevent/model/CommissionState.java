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
package se.sll.reimbursementadapter.admincareevent.model;

import java.io.Serializable;

import se.sll.reimbursementadapter.parser.TermItem;
import se.sll.reimbursementadapter.parser.TermState;

/**
 * Commission state.
 * 
 * @author Peter
 *
 */
public class CommissionState extends TermState implements Serializable {
    private static final long serialVersionUID = 2L;
    /**
     * The type.
     * @serial
     */
    private TermItem<CommissionTypeState> commissionType;

    
    /**
     * The contract reference.
     * @return
     */
    private String contractCode;
    
    /**
     * The contract reference.
     * @return
     */
    private String assignmentType;
    

    public TermItem<CommissionTypeState> getCommissionType() {
        return commissionType;
    }
    
    public void setCommissionType(TermItem<CommissionTypeState> commissionType) {
        this.commissionType = commissionType;
    }

    public String getContractCode() {
        return contractCode;
    }

    public void setContractCode(String contractCode) {
        this.contractCode = contractCode;
    }
    public String getAssignmentType() {
        return assignmentType;
    }

    public void setAssignmentType(String assignmentType) {
        this.assignmentType = assignmentType;
    }   
}