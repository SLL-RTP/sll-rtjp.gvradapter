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
package se.sll.gvradapter.admincareevent.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import se.sll.gvradapter.parser.TermItem;
import se.sll.gvradapter.parser.TermState;

/**
 * Facility state. The id corresponds to "kombikaid"
 * 
 * @author Peter
 *
 */
public class FacilityState extends TermState implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * The list of commissions.
     * @serial
     */
    private List<TermItem<CommissionState>> commissions = new ArrayList<TermItem<CommissionState>>();
    private TermItem<HSAMappingState> hsaMapping;


    public List<TermItem<CommissionState>> getCommissions() {
        return commissions;
    }

    public void setCommissions(List<TermItem<CommissionState>> commissions) {
        this.commissions = commissions;
    }        
    
    public TermItem<HSAMappingState> getHSAMapping() {
    	return hsaMapping;
    }
    
    public void setHSAMapping(TermItem<HSAMappingState> hsaMapping) {
    	this.hsaMapping = hsaMapping;
    }
}