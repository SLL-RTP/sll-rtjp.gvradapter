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
package se.sll.reimbursementadapter.processreimbursement.util;

import org.junit.Assert;
import org.junit.Test;
import se.sll.reimbursementadapter.parser.TermItem;
import se.sll.reimbursementadapter.processreimbursement.model.GeographicalAreaState;

import java.util.Date;
import java.util.Map;

public class CodeServerCacheBuilderTest {

    @Test
    public void geographicalAreaToMedicalServicesArea() {
        Map<String, TermItem<GeographicalAreaState>> codeCache = new CodeServerCacheBuilder().withGeographicalAreaFile("c:\\tmp\\out\\BASOMRNY.XML").build();

        String geographicalArea = "2242363";
        String expectedMedicalServicesArea = "131103";

        Assert.assertEquals("Check Geographical Area", expectedMedicalServicesArea, codeCache.get(geographicalArea).getState(new Date()).getMedicalServiceArea());
    }

}
