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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.InvalidParameterException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

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
    private static final Logger LOG = LoggerFactory.getLogger(GVRFileReader.class);

    /** Local path to the directory where GVR files are stored. */
    @Value("${pr.gvr.ftp.localPath:/tmp/gvr/in}")
    public String localPath;

    /** Timestamp format from the RIV schema request. Used for listing the files within the parameter period. */
    @Value("${pr.riv.timestampFormat:yyyyMMddHHmmssSSS}")
    private String rivTimestampFormat;

    /** Timestamp format that is embedded in the GVR file names (only if DateFilterMethod.FILENAME is used). */
    @Value("${pr.gvr.io.timestampFormat:yyyy-MM-dd'T'HHmmss}")
    private String gvrTimestampFormat;

    /** Regex for extracting a timestamp of the above format from a GVR file (only if DateFilterMethod.FILENAME is used).
     *  Uses the first capture group from the expression as the timestamp. */
    @Value("${pr.gvr.io.timestampExtractionRegEx}")
    private String gvrTimestampExtractionRegEx;

    /** Regex for extracting a timestamp of the above format from a GVR file (only if DateFilterMethod.FILENAME is used).
     *  Uses the first capture group from the expression as the timestamp. */
    @Value("${pr.gvr.io.filterMethod}")
    private String gvrFilterMethod;

    /** The number of retries before failing when listing GVR files from disk. */
    @Value("${pr.gvr.io.numRetries}")
    private int gvrNumRetries;

    /** The delay between each retry when listing GVR files from disk. */
    @Value("${pr.gvr.io.retryInterval}")
    private int gvrRetryInterval;

    /** The configured {@link DateFilterMethod} for the class. METADATA or FILENAME. */
    private DateFilterMethod dateFilterMethod;

    /**
     * Post construct initiator for setting the dateFilterMethod enum based on the injected contents of gvrFilterMethod.
     */
    @PostConstruct
    private void init() {
        if (gvrFilterMethod == null || "".equals(gvrFilterMethod)) {
            LOG.error("DateFilterMethod for filtering GVR files on dates is not set!");
        }
        this.dateFilterMethod = DateFilterMethod.valueOf(gvrFilterMethod);
    }

    /**
     * Gets a list of file {@link java.nio.file.Path}s in a configured directory that has a modified
     * date that is newer than the date parameter provided.
     * Still used by the unit tests where I can't be bothered to create new Date objects for the request.
     *
     * @param fromDateString The date to compare the files with (format: yyyyMMddHHmmss).
     * @param toDateString The date to compare the files with (format: yyyyMMddHHmmss).
     * @return a List of {@link java.nio.file.Path} objects.
     * @throws java.security.InvalidParameterException If the supplied date format is not valid.
     * @throws ParseException When the supplied date parameters could not be parsed.
     *
     */
    public List<Path> getFileList(String fromDateString, String toDateString)
            throws InvalidParameterException, ParseException, IOException {
        SimpleDateFormat df = new SimpleDateFormat(gvrTimestampFormat);
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
    public List<Path> getFileList(final Date fromDate, final Date toDate) throws IOException {
        Path directoryToIterate = FileSystems.getDefault().getPath(localPath);

        // Read all the wanted files from the current directory.
        List<Path> response = new ArrayList<>();

        // Create the appropriate directory filter that matches the current configuration (DateFilterMethod).
        DirectoryStream.Filter<Path> configuresDirectoryFilter = dateFilterMethod.equals(DateFilterMethod.METADATA) ?
                new MetadataDateRangeFilter(fromDate, toDate) :
                new FileNameDateRangeFilter(fromDate, toDate, this);

        // Create a new DirectoryStream for the configured directory and corresponding filter.
        for (int currentTry = 0; currentTry < gvrNumRetries; currentTry++) {
            try(DirectoryStream<Path> ds = Files.newDirectoryStream(directoryToIterate, configuresDirectoryFilter))  {
                for (Path p : ds) {
                    // Add every non-filtered file to the response list.
                    response.add(p);
                }
                break;
            } catch (IOException e) {
                // If within the retry count, warn and sleep for a bit, then try again.
                if (currentTry < (gvrNumRetries - 1)) {
                    LOG.warn("IOException while filtering the files in the current directory.", e);
                    if (gvrRetryInterval != 0) {
                        try {
                            Thread.sleep(gvrRetryInterval);
                        } catch (InterruptedException e1) { }
                    }
                } else if (currentTry==(gvrNumRetries - 1)) {
                    // If this was the final try, error LOG for alert and rethrow the exception to the WS stack.
                    LOG.error("IOException while filtering the files in the current directory.", e);
                    throw e;
                }
            }
        }

        // Sort the list according to the configured DateFilterMethod.
        Collections.sort(response, dateFilterMethod.equals(DateFilterMethod.METADATA) ?
                new FileMetadataDateSorter() :
                new FileNameDateSorter(this));

        return response;
    }

    /**
     * Returns a {@link java.util.Date} from the file name of the provided GVR file.
     *
     * @param file The GVR file to read the date from.
     * @return A {@link java.util.Date} with the date from the file name of the provided GVR file.
     */
    public Date getDateFromGVRFile(Path file) {
        Date gvrFileDate = null;

        if (dateFilterMethod.equals(DateFilterMethod.FILENAME)) {
            SimpleDateFormat gvrFormat = new SimpleDateFormat(gvrTimestampFormat);

            String fileName = file.getFileName().toString();
            String fileTimestamp = fileName.replaceFirst(gvrTimestampExtractionRegEx, "$1");
            try {
                gvrFileDate = gvrFormat.parse(fileTimestamp);
            } catch (ParseException e) {
                LOG.error("The timestamp of format: " + gvrTimestampFormat + " could not be parsed from file name: "
                        + fileName);
            }
        } else if (dateFilterMethod.equals(DateFilterMethod.METADATA)) {
            try {
                BasicFileAttributeView basicAttrsView = Files.getFileAttributeView(file, BasicFileAttributeView.class);
                BasicFileAttributes basicAttrs =  basicAttrsView.readAttributes();
                return  new Date(basicAttrs.lastModifiedTime().toMillis());
            } catch (IOException e) {
                LOG.error("Error while reading the BasicFileAttributes from the current file: " + file.getFileName());
            }

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
    public Reader getReaderForFile(Path path) throws IOException {
        BufferedReader bufferedReader = null;
        // Create a new DirectoryStream for the configured directory and corresponding filter.
        for (int currentTry = 0; currentTry < gvrNumRetries; currentTry++) {
            try {
                bufferedReader = Files.newBufferedReader(path, Charset.forName("ISO-8859-1"));
                break;
            } catch (IOException e) {
                // If within the retry count, warn and sleep for a bit, then try again.
                if (currentTry < (gvrNumRetries - 1)) {
                    LOG.warn("IOException while fetching reader for file: " + path, e);
                    if (gvrRetryInterval != 0) {
                        try {
                            Thread.sleep(gvrRetryInterval);
                        } catch (InterruptedException e1) { }
                    }
                } else if (currentTry==(gvrNumRetries - 1)) {
                    // If this was the final try, error LOG for alert and rethrow the exception to the WS stack.
                    LOG.error("IOException while fetching reader for file: " + path, e);
                    throw e;
                }
            }
        }
        return bufferedReader;
    }

    public String getGvrTimestampFormat() {
        return gvrTimestampFormat;
    }

    public void setDateFilterMethod(DateFilterMethod method) {
        this.dateFilterMethod = method;
    }

    public String getLocalPath() { return localPath; }

    public void setLocalPath(String localPath) { this.localPath = localPath; }
}
