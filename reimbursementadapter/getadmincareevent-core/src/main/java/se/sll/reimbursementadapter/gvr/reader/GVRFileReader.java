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
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Handles listing all the GVR files that are newer (last changed) than the
 * incoming date parameter as well as reading a specified file (Path).
 */
@Component
public class GVRFileReader {

    /** Logger */
    private static final Logger log = LoggerFactory.getLogger(GVRFileReader.class);

    /** Local path to the directory where GVR files are stored. */
    @Value("${pr.ftp.gvr.localPath:/tmp/gvr/in}")
    private String localPath;

    /** Timestamp format that comes from the RIV schema request. */
    @Value("${pr.riv.timestampFormat:yyyyMMddHHmmssSSS}")
    private String rivTimestampFormat;

    /** Timestamp format that is embedded in the GVR file names (if DateFilterMethod.FILENAME is used). */
    @Value("${pr.gvr.timestampFormat:yyyy-MM-dd'T'HHmmss}")
    private String gvrTimestampFormat;

    /** The configured {@link DateFilterMethod} for the class. METADATA or FILENAME. */
    private DateFilterMethod dateFilterMethod = DateFilterMethod.FILENAME;

    /**
     * Gets a list of file {@link java.nio.file.Path}s in a configured directory that has a modified
     * date that is newer than the date parameter provided.
     *
     * @param fromDateString The date to compare the files with (format: yyyyMMddHHmmss).
     * @param toDateString The date to compare the files with (format: yyyyMMddHHmmss).
     * @return a List of {@link java.nio.file.Path} objects.
     * @throws java.security.InvalidParameterException If the supplied date format is not valid.
     * @throws ParseException When the supplied date parameters could not be parsed.
     */
    public List<Path> getFileList(String fromDateString, String toDateString) throws InvalidParameterException, ParseException {
        SimpleDateFormat df = new SimpleDateFormat(rivTimestampFormat);
        Date fromDate = null;
        Date toDate = null;
        if (fromDateString != null && !fromDateString.equals("")) {
            fromDate = df.parse(fromDateString);
        }
        if (toDateString != null && !toDateString.equals("")) {
            toDate = df.parse(toDateString);
        }

        return getFileList(fromDate, toDate);
    }

    /**
     * Gets a list of file {@link java.nio.file.Path}s in a configured directory that has a modified
     * date that is newer than the date parameter provided.
     *
     * @param fromDate The date to compare the files with (format: yyyyMMddHHmmss).
     * @return a List of {@link java.nio.file.Path} objects.
     * @throws java.security.InvalidParameterException If the supplied date format is not valid.
     */
    public List<Path> getFileList(final Date fromDate, final Date toDate) throws InvalidParameterException {
        Path folderToIterate = FileSystems.getDefault().getPath(localPath);

        log.debug("Reading files from date: " + fromDate + " and path: " + folderToIterate.toString());

        // Filter all the files in the configured in directory.
        List<Path> response = new ArrayList<>();

        try(DirectoryStream<Path> ds = Files.newDirectoryStream(folderToIterate,
                dateFilterMethod.equals(DateFilterMethod.METADATA) ?
                new MetadataDateRangeFilter(fromDate, toDate) :
                new FileNameDateRangeFilter(fromDate, toDate, this)))  {
            for (Path p : ds) {
                response.add(p);
            }
        } catch (IOException e) {
            log.error("IOException while filtering the files in the current directory.", e);
        }

        return response;
    }

    /**
     * Returns a {@link java.util.Date} from the file name of the provided GVR file.
     *
     * @param file The GVR file to read the date from.
     * @return A {@link java.util.Date} with the date from the file name of the provided GVR file.
     */
    public Date getDateFromGVRFileName(Path file) {
        SimpleDateFormat gvrFormat = new SimpleDateFormat(gvrTimestampFormat);
        Date gvrFileDate = null;
        if (file.getFileName().toString().contains("T")) {
            String[] tSplit = file.getFileName().toString().split("_");
            // According to the rules the filename must end with "T<timestamp>.xml".
            String timeStamp = tSplit[tSplit.length - 1];
            if (timeStamp.endsWith(".xml")) {
                timeStamp = timeStamp.substring(0, timeStamp.length() - 4);
                try {
                    gvrFileDate = gvrFormat.parse(timeStamp);
                } catch (ParseException e) {
                    log.debug("File date could not be parsed");
                }
            } else {
                log.debug("File is not an XML file and is not valid");
            }
        } else {
            log.debug("File is not an XML file and is not valid");
        }
        return gvrFileDate;
    }

    /**
     * Creates a file reader (ISO-8859-1) for the provided file {@link java.nio.file.Path}.
     * Remember, children, <u>always</u> close the Reader after use!
     *
     * @param path The full path to the file to be read.
     * @return A initialized Reader (ISO-8859-1) for reading the contents of the provided file.
     */
    public Reader GetReaderForFile(Path path) throws IOException {
        return Files.newBufferedReader(path, Charset.forName("ISO-8859-1"));
    }

    public String getGvrTimestampFormat() {
        return gvrTimestampFormat;
    }
}
