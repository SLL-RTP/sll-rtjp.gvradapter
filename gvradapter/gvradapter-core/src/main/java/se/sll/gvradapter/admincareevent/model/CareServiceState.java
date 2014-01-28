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

import se.sll.gvradapter.parser.TermItem;
import se.sll.gvradapter.parser.TermState;

/**
 * CareService state.
 * 
 * @author Peter
 */
public class CareServiceState extends TermState implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * The service type.
     * @serial
     */
    private String careServiceType;

    /**
     * The company.
     * @serial
     */
    private TermItem<CompanyState> company;
    
    public String getCareServiceType() {
        return careServiceType;
    }
    
    public void setCareServiceType(String careServiceType) {
        this.careServiceType = careServiceType;
    }
    
    public TermItem<CompanyState> getCompany() {
        return company;
    }
    
    public void setCompany(TermItem<CompanyState> company) {
        this.company = company;
    }
}