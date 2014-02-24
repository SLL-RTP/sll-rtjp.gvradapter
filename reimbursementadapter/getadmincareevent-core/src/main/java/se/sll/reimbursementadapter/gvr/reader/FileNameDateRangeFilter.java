package se.sll.reimbursementadapter.gvr.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Date;

/**
 * Filters a file {@link Path} on whether the timestamp found in the file name
 * falls within the provided from Date and toDate parameters.
 */
public class FileNameDateRangeFilter implements  DirectoryStream.Filter<Path> {

    /** Logger */
    private static final Logger log = LoggerFactory.getLogger(GVRFileReader.class);

    private Date fromDate;
    private Date toDate;
    private GVRFileReader fileReader;

    public FileNameDateRangeFilter(Date fromDate, Date toDate, GVRFileReader fileReader) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.fileReader = fileReader;
    }

    /**
     * Decides if the given directory entry should be accepted or filtered.
     *
     * @param entry the directory entry to be tested
     * @return {@code true} if the directory entry should be accepted
     * @throws java.io.IOException If an I/O error occurs
     */
    @Override
    public boolean accept(Path entry) throws IOException {
        boolean isXML = entry.getFileName().toString().toLowerCase().endsWith(".xml");

        // Return false if not an XML-file, to make the filter below easier.
        if (!isXML) {
            return false;
        }

        // Read the date om the provided file name according to the spec.
        Date gvrFileDate = fileReader.getDateFromGVRFileName(entry);
        if (gvrFileDate == null) {
            // Invalid File, remove from filter.
            log.info("File " + entry.toString() + " does not have a valid date and will therefore be filtered away");
            return false;
        }

        // [..yes, .compareTo >=/<= works here to, but this is easier to parse imho. :)]
        boolean hasFileTimestampAfterLocalFromDate = fromDate == null
                                                    || gvrFileDate.after(fromDate)
                                                    || gvrFileDate.equals(fromDate);
        boolean hasFileTimestampBeforeLocalToDate = toDate == null
                                                    || gvrFileDate.before(toDate)
                                                    || gvrFileDate.equals(toDate);
        return hasFileTimestampAfterLocalFromDate && hasFileTimestampBeforeLocalToDate;
    }
}