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
package se.sll.reimbursementadapter.gvr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import riv.followup.processdevelopment.reimbursement.v1.ActivityType;
import riv.followup.processdevelopment.reimbursement.v1.ConditionType;
import riv.followup.processdevelopment.reimbursement.v1.DiagnosisType;
import riv.followup.processdevelopment.reimbursement.v1.ObjectFactory;
import riv.followup.processdevelopment.reimbursement.v1.OrderedCVType;
import riv.followup.processdevelopment.reimbursement.v1.ProductType;
import riv.followup.processdevelopment.reimbursement.v1.ProfessionType;


public class SeqNoComparatorTest {
    
    private static final SeqNoComparator SEQ_NO_COMPARATOR = new SeqNoComparator();
    private static final ObjectFactory of = new ObjectFactory();
    
    @Test
    public void testSortingActivityType() {
        // Create some test data.
        ActivityType a1 = of.createActivityType();
        a1.setSeqNo(2);
        ActivityType a2 = of.createActivityType();
        a2.setSeqNo(1);
        ActivityType a3 = of.createActivityType();
        a3.setSeqNo(4);
        ActivityType a4 = of.createActivityType();
        a4.setSeqNo(3);
        ArrayList<ActivityType> list = new ArrayList<ActivityType>(Arrays.asList(a1, a2, a3, a4));
        
        // Sort the list.
        Collections.sort(list, SEQ_NO_COMPARATOR);
        
        // Make sure the order is correct.
        Assert.assertEquals("Number of activities", 4, list.size());
        Assert.assertEquals("ActivityType #1 seqNo", 1, list.get(0).getSeqNo().intValue());
        Assert.assertEquals("ActivityType #2 seqNo", 2, list.get(1).getSeqNo().intValue());
        Assert.assertEquals("ActivityType #3 seqNo", 3, list.get(2).getSeqNo().intValue());
        Assert.assertEquals("ActivityType #4 seqNo", 4, list.get(3).getSeqNo().intValue());
    }
    
    @Test
    public void testSortingOrderedCVType() {
        // Create some test data.
        OrderedCVType o1 = of.createOrderedCVType();
        o1.setSeqNo(2);
        OrderedCVType o2 = of.createOrderedCVType();
        o2.setSeqNo(1);
        OrderedCVType o3 = of.createOrderedCVType();
        o3.setSeqNo(3);
        OrderedCVType o4 = of.createOrderedCVType();
        o4.setSeqNo(5);
        OrderedCVType o5 = of.createOrderedCVType();
        o5.setSeqNo(4);
        ArrayList<OrderedCVType> list = new ArrayList<OrderedCVType>(Arrays.asList(o1, o2, o3, o4, o5));
        
        // Sort the list.
        Collections.sort(list, SEQ_NO_COMPARATOR);
        
        // Make sure the order is correct.
        Assert.assertEquals("Number of elements", 5, list.size());
        Assert.assertEquals("OrderedCVType #1 seqNo", 1, list.get(0).getSeqNo().intValue());
        Assert.assertEquals("OrderedCVType #2 seqNo", 2, list.get(1).getSeqNo().intValue());
        Assert.assertEquals("OrderedCVType #3 seqNo", 3, list.get(2).getSeqNo().intValue());
        Assert.assertEquals("OrderedCVType #4 seqNo", 4, list.get(3).getSeqNo().intValue());
        Assert.assertEquals("OrderedCVType #5 seqNo", 5, list.get(4).getSeqNo().intValue());
    }
    
    @Test
    public void testSortingAnEmptyList() {
        // Create some test data.
        ArrayList<DiagnosisType> list = new ArrayList<DiagnosisType>();
        
        // Sort the list.
        Collections.sort(list, SEQ_NO_COMPARATOR);
        
        Assert.assertEquals("Number of elements", 0, list.size());
    }
    
    @Test
    public void testSortingWithDuplicateSeqNos() {
        // Create some test data.
        ConditionType o1 = of.createConditionType();
        o1.setSeqNo(1);
        ConditionType o2 = of.createConditionType();
        o2.setSeqNo(1);
        ConditionType o3 = of.createConditionType();
        o3.setSeqNo(2);
        ConditionType o4 = of.createConditionType();
        o4.setSeqNo(3);
        ConditionType o5 = of.createConditionType();
        o5.setSeqNo(1);
        ArrayList<ConditionType> list = new ArrayList<ConditionType>(Arrays.asList(o1, o2, o3, o4, o5));
        
        // Sort the list.
        Collections.sort(list, SEQ_NO_COMPARATOR);
        
        // Make sure the order is correct.
        Assert.assertEquals("Number of conditions", 5, list.size());
        Assert.assertEquals("ConditionType #1 seqNo", 1, list.get(0).getSeqNo().intValue());
        Assert.assertEquals("ConditionType #2 seqNo", 1, list.get(1).getSeqNo().intValue());
        Assert.assertEquals("ConditionType #3 seqNo", 1, list.get(2).getSeqNo().intValue());
        Assert.assertEquals("ConditionType #4 seqNo", 2, list.get(3).getSeqNo().intValue());
        Assert.assertEquals("ConditionType #5 seqNo", 3, list.get(4).getSeqNo().intValue());
    }
    
    @Test
    public void testSortingAlreadyOrderedList() {
        // Create some test data.
        ProfessionType o1 = of.createProfessionType();
        o1.setSeqNo(1);
        ProfessionType o2 = of.createProfessionType();
        o2.setSeqNo(2);
        ProfessionType o3 = of.createProfessionType();
        o3.setSeqNo(3);
        ProfessionType o4 = of.createProfessionType();
        o4.setSeqNo(4);
        ArrayList<ProfessionType> list = new ArrayList<ProfessionType>(Arrays.asList(o1, o2, o3, o4));
        
        // Sort the list.
        Collections.sort(list, SEQ_NO_COMPARATOR);
        
        // Make sure the order is correct.
        Assert.assertEquals("Number of elements", 4, list.size());
        Assert.assertEquals("ProfessionType #1 seqNo", 1, list.get(0).getSeqNo().intValue());
        Assert.assertEquals("ProfessionType #2 seqNo", 2, list.get(1).getSeqNo().intValue());
        Assert.assertEquals("ProfessionType #3 seqNo", 3, list.get(2).getSeqNo().intValue());
        Assert.assertEquals("ProfessionType #4 seqNo", 4, list.get(3).getSeqNo().intValue());
    }
    
    @Test(expected=ClassCastException.class)
    public void testClassCastException() {
        // Create some test data.
        ProductType o1 = of.createProductType();
        o1.setCount(2);
        ProductType o2 = of.createProductType();
        o2.setCount(1);
        ArrayList<ProductType> list = new ArrayList<ProductType>(Arrays.asList(o1, o2));
        
        // Sorting should fail since ProductType is not supported by the SeqNoComparator.
        Collections.sort(list, SEQ_NO_COMPARATOR);
    }
    
    @Test(expected=NullPointerException.class)
    public void testSortingWhenSeqNoIsMissing() {
        // Create some test data that is missing seqNo.
        ConditionType o1 = of.createConditionType();
        ConditionType o2 = of.createConditionType();
        o2.setSeqNo(1);
        ConditionType o3 = of.createConditionType();
        ArrayList<ConditionType> list = new ArrayList<ConditionType>(Arrays.asList(o1, o2, o3));
        
        // Sorting should fail since seqNo is missing.
        Collections.sort(list, SEQ_NO_COMPARATOR);
    }
    
}
