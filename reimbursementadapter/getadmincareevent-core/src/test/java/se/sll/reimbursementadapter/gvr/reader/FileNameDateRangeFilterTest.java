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
package se.sll.reimbursementadapter.gvr.reader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import se.sll.reimbursementadapter.TestSupport;

/**
 * Tests the file listing filter that uses the date in the file name to check if a given
 * file is within a given date range.
 */
public class FileNameDateRangeFilterTest extends TestSupport {

    /** Lists files matching a period and provides Readers for individual files. */
    @Autowired
    private GVRFileReader gvrFileReader;

    /** Timestamp format that comes from the RIV schema request. */
    @Value("${pr.riv.timestampFormat:yyyyMMddHHmmssSSS}")
    private String rivTimestampFormat;

    /** Filter start date. */
    private String fromDate = "20140211000000000";
    /** Filter end date. */
    private String toDate = "20140211235959000";

    // File name dates in the format yyyy-MM-dd'T'HHmmss
    private String dateLowInvalid = "2014-02-10T235959";
    private String dateLowValid = "2014-02-11T000000";
    private String dateHighValid = "2014-02-11T235959";
    private String dateHighInvalid = "2014-02-12T000000";

    /** The filter to be used. */
    private FileNameDateRangeFilter filter;
    /** The SimpleDateFormat that is used to create Date objects. */
    private SimpleDateFormat df;

    @Before
    public void before() throws Exception {
        gvrFileReader.setDateFilterMethod(DateFilterMethod.FILENAME);
        df = new SimpleDateFormat(rivTimestampFormat);
        filter = new FileNameDateRangeFilter(df.parse(fromDate), df.parse(toDate), gvrFileReader);
    }

    /**
     * Creates a file with an updated tim'e just before the filter start date,
     * and will therefore be filtered away.
     */
    @Test
    public void testAcceptLowInvalidDate() throws Exception {
        Path fileBeforeValidTime = Files.createTempFile("reimbursement-", "_" + dateLowInvalid + ".xml");
        System.out.format("The temporary file" +
                " has been created: %s%n", fileBeforeValidTime);
        Assert.assertFalse(filter.accept(fileBeforeValidTime));
    }

    /**
     * Creates a file with an updated time equalling the filter start date,
     * and will therefore be accepted.
     */
    @Test
    public void testAcceptLowValidDate() throws Exception {
        Path fileLowValidTime = Files.createTempFile("reimbursement-", "_" + dateLowValid + ".xml");
        System.out.format("The temporary file" +
                " has been created: %s%n", fileLowValidTime);
        Assert.assertTrue(filter.accept(fileLowValidTime));
    }

    /**
     * Creates a file with an updated time equalling the filter end date,
     * and will therefore be accepted.
     */
    @Test
    public void testAcceptHighValidDate() throws Exception {
        Path fileHighValidTime = Files.createTempFile("reimbursement-", "_" + dateHighValid + ".xml");
        System.out.format("The temporary file" +
                " has been created: %s%n", fileHighValidTime);
        Assert.assertTrue(filter.accept(fileHighValidTime));
    }

    /**
     * Creates a file with an updated time just after the filter end date,
     * and will therefore be filtered away.
     */
    @Test
    public void testAcceptHighInvalidDate() throws Exception {
        Path fileHighInvalidTime = Files.createTempFile("reimbursement-", "_" + dateHighInvalid + ".xml");
        System.out.format("The temporary file" +
                " has been created: %s%n", fileHighInvalidTime);
        Assert.assertFalse(filter.accept(fileHighInvalidTime));
    }
}
