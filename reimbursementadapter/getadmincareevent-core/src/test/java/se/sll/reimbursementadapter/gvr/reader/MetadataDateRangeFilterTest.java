package se.sll.reimbursementadapter.gvr.reader;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import se.sll.reimbursementadapter.TestSupport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;

/**
 * Tests the file listing filter that uses file metadata to check if a given
 * file is within a given date range.
 */
public class MetadataDateRangeFilterTest extends TestSupport {

    /** Timestamp format that comes from the RIV schema request. */
    @Value("${pr.riv.timestampFormat:yyyyMMddHHmmssSSS}")
    private String rivTimestampFormat;

    // String dates for different files and the date range for the filter.
    private String dateLowInvalid = "20140210235959999";
    private String filterFromDate = "20140211000000000";
    private String dateLowValid = "20140211000000000";
    private String dateHighValid = "20140211235959000";
    private String filterToDate = "20140211235959000";
    private String dateHighInvalid = "20140212000000000";

    /** The filter to be used. */
    private MetadataDateRangeFilter filter;

    /** The SimpleDateFormat that is used to create Date objects. */
    private SimpleDateFormat df;

    @Before
    public void before() throws Exception {
        df = new SimpleDateFormat(rivTimestampFormat);
        filter = new MetadataDateRangeFilter(df.parse(filterFromDate), df.parse(filterToDate));
    }

    /**
     * Creates a file with an updated time just before the filter start date,
     * and will therefore be filtered away.
     */
    @Test
    public void testAcceptLowInvalidDate() throws Exception {
        Path fileBeforeValidTime = Files.createTempFile("reimbursement-", ".xml");
        Files.setLastModifiedTime(fileBeforeValidTime, FileTime.fromMillis(df.parse(dateLowInvalid).getTime()));
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
        Path fileLowValidTime = Files.createTempFile("reimbursement-", ".xml");
        Files.setLastModifiedTime(fileLowValidTime, FileTime.fromMillis(df.parse(dateLowValid).getTime()));
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
        Path fileHighValidTime = Files.createTempFile("reimbursement-", ".xml");
        Files.setLastModifiedTime(fileHighValidTime, FileTime.fromMillis(df.parse(dateHighValid).getTime()));
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
        Path fileHighInvalidTime = Files.createTempFile("reimbursement-", ".xml");
        Files.setLastModifiedTime(fileHighInvalidTime, FileTime.fromMillis(df.parse(dateHighInvalid).getTime()));
        System.out.format("The temporary file" +
                " has been created: %s%n", fileHighInvalidTime);
        Assert.assertFalse(filter.accept(fileHighInvalidTime));
    }
}
