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
