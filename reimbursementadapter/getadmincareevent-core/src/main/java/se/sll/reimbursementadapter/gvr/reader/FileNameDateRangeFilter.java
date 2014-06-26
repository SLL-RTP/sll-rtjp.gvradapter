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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filters a file {@link Path} on whether the timestamp found in the file name
 * falls within the provided fromDate and toDate parameters.
 */
public class FileNameDateRangeFilter implements  DirectoryStream.Filter<Path> {

    /** Logger */
    private static final Logger LOG = LoggerFactory.getLogger(FileNameDateRangeFilter.class);

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
            LOG.warn("File " + entry.toString() + " is not an XML file and will therefore be filtered away");
            return false;
        }

        // Read the date from the provided file name according to the spec.
        Date gvrFileDate = fileReader.getDateFromGVRFile(entry);
        if (gvrFileDate == null) {
            // Invalid File, remove from filter.
            LOG.warn("File " + entry.toString() + " does not have a valid date and will therefore be filtered away");
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
