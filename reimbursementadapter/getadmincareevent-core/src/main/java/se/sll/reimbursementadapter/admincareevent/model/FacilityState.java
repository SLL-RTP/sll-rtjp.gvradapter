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
import java.util.ArrayList;
import java.util.List;

import se.sll.reimbursementadapter.parser.TermItem;
import se.sll.reimbursementadapter.parser.TermState;

/**
 * Facility state. The id corresponds to "kombikaid"
 *
 * @author Peter
 */
public class FacilityState extends TermState implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * The list of commissions.
     *
     * @serial
     */
    private List<TermItemCommission<CommissionState>> commissions = new ArrayList<>();
    private TermItem<HSAMappingState> hsaMapping;

    /**
     * The customer code.
     */
    private String customerCode;

    /**
     * The type of care unit.
     */
    private String careUnitType;

    public List<TermItemCommission<CommissionState>> getCommissions() {
        return commissions;
    }

    public void setCommissions(List<TermItemCommission<CommissionState>> commissions) {
        this.commissions = commissions;
    }

    public TermItem<HSAMappingState> getHSAMapping() {
        return hsaMapping;
    }

    public void setHSAMapping(TermItem<HSAMappingState> hsaMapping) {
        this.hsaMapping = hsaMapping;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public void setCustomerCode(String customerCode) {
        this.customerCode = customerCode;
    }

    public String getCareUnitType() {
        return careUnitType;
    }

    public void setCareUnitType(String careUnitType) {
        this.careUnitType = careUnitType;
    }

    @Override
    public String toString() {
        return "FacilityState [commissions=" + commissions + ", hsaMapping="
                + hsaMapping + ", customerCode=" + customerCode
                + ", toString()=" + super.toString() + "]";
    }
    
}