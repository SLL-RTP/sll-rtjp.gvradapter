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
package se.sll.gvradapter.gvr.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
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
    @Value("${pr.gvr.timestampFormat:yyyyMMddHHmmss}")
    private String rivTimestampFormat;

    /** Timestamp format that is embedded in the GVR file names (if DateFilterMethod.FILENAME is used). */
    private String gvrTimestampFormat = "yyyy-MM-dd'T'HHmmss";

    /** The configured {@link DateFilterMethod} for the class. METADATA or FILENAME. */
    private DateFilterMethod dateFilterMethod = DateFilterMethod.METADATA;

    /**
     * Gets a list of file {@link java.nio.file.Path}s in a configured directory that has a modified
     * date that is newer than the date parameter provided.
     *
     * @param fromDateString The date to compare the files with (format: yyyyMMddHHmmss).
     * @param toDateString The date to compare the files with (format: yyyyMMddHHmmss).
     * @return a List of {@link java.nio.file.Path} objects.
     * @throws java.security.InvalidParameterException If the supplied date format is not valid.
     */
    public List<Path> getFileList(String fromDateString, String toDateString) throws InvalidParameterException {
        SimpleDateFormat df = new SimpleDateFormat(rivTimestampFormat);
        Date fromDate = null;
        Date toDate = null;
        try {
            if (fromDateString != null && !fromDateString.equals("")) {
                fromDate = df.parse(fromDateString);
            }
            if (toDateString != null && !toDateString.equals("")) {
                toDate = df.parse(toDateString);
            }
        } catch (ParseException e1) {
            log.error("One of the supplied date parameters (" + fromDateString + "/" + toDateString + ") is not valid.", e1);
            throw new InvalidParameterException("One of the supplied date parameters (" + fromDateString + "/" + toDateString + ") is not valid.");
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
        List<Path> response = new ArrayList<Path>();
        DirectoryStream<Path> ds = null;
        // [bound to java6 spec (but not API if you are sneaky!) by the SOI Toolkit dependency, can't use try-with-resources.]
        try  {
            ds = Files.newDirectoryStream(folderToIterate, new DirectoryStream.Filter<Path>() {
                public boolean accept(Path entry) throws IOException {
                    // Depending on the configured filter type, invoke the appropriate filter method.
                    if (dateFilterMethod.equals(DateFilterMethod.METADATA)) {
                        return filterFilePathForMetadata(entry, fromDate, toDate);
                    } else if (dateFilterMethod.equals(DateFilterMethod.FILENAME)) {
                        return filterFilePathForFilename(entry, fromDate, toDate);
                    }
                    return false;
                }
            });
            for (Path p : ds) {
                response.add(p);
            }
        } catch (IOException e) {
            log.error("IOException while filtering the files in the current directory.", e);
        } finally {
            if (ds != null) {
                try {
                    ds.close();
                } catch (IOException e) {
                    log.error("Unexpected IOException when closing DirectoryStream.", e);
                }
            }
        }

        return response;
	}

    /**
     * Filters a file {@link Path} on whether the timestamp found in the file name
     * falls within the provided from Date and toDate parameters.
     *
     * @param entry the {@link Path} object to filter on.
     * @param fromDate the from date to filter on.
     * @param toDate The to date to filter on.
     * @return a boolean that indicates whether the file timestamp falls within the provided date span.
     */
    private boolean filterFilePathForFilename(Path entry, Date fromDate, Date toDate) {
        SimpleDateFormat gvrFormat = new SimpleDateFormat(gvrTimestampFormat);
        boolean isXML = entry.getFileName().toString().toLowerCase().endsWith(".xml");
        if (entry.getFileName().toString().contains("T")) {
            String[] tSplit = entry.getFileName().toString().split("_");
            // According to the rules the filename must end with "T<timestamp>.xml".
            String timeStamp = tSplit[tSplit.length - 1];
            if (timeStamp.endsWith(".xml")) {
                timeStamp = timeStamp.substring(0, timeStamp.length() - 4);
                try {
                    Date gvrFileDate = gvrFormat.parse(timeStamp);

                    // [..yes, .compareTo >=/<= works here to, but this is easier to parse imho. :)]
                    boolean hasFileTimestampAfterLocalFromDate = fromDate == null || gvrFileDate.after(fromDate) || gvrFileDate.equals(fromDate);
                    boolean hasFileTimestampBeforeLocalToDate = toDate == null || gvrFileDate.before(toDate) || gvrFileDate.equals(toDate);
                    return isXML && hasFileTimestampAfterLocalFromDate && hasFileTimestampBeforeLocalToDate;
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * Filters a file {@link Path} on whether the timestamp in the file system metadata
     * falls within the provided from Date and toDate parameters.
     *
     * @param entry the {@link Path} object to filter on.
     * @param fromDate the from date to filter on.
     * @param toDate The to date to filter on.
     * @return a boolean that indicates whether the file timestamp falls within the provided date span.
     */
    private boolean filterFilePathForMetadata(Path entry, Date fromDate, Date toDate) throws IOException {
        // Filter reads basic attributes and accepts all the files with lastModifiedTime > inDate
        boolean isXML = entry.getFileName().toString().toLowerCase().endsWith(".xml");
        final long fromDateEpoch = fromDate != null ? fromDate.getTime() : 0L;
        final long toDateEpoch = toDate != null ? toDate.getTime() : Long.MAX_VALUE;

        BasicFileAttributeView basicAttrsView = Files.getFileAttributeView(entry, BasicFileAttributeView.class);
        BasicFileAttributes basicAttrs =  basicAttrsView.readAttributes();
        boolean isLastModifiedAfterLocalFromDate = FileTime.fromMillis(fromDateEpoch).compareTo(basicAttrs.lastModifiedTime()) <= 0;
        boolean isLastModifiedBeforeLocalToDate = FileTime.fromMillis(toDateEpoch).compareTo(basicAttrs.lastModifiedTime()) >= 0;
        return isXML && basicAttrs.isRegularFile() && isLastModifiedAfterLocalFromDate && isLastModifiedBeforeLocalToDate;
    }

    /**
	 * Reads a specific file from a Path with the correct character set.
	 * 
	 * @param path The full path for the file that will be read.
	 * @return A String with the full file contents.
	 */
	public String readFile(Path path) {
		Charset charset = Charset.forName("ISO-8859-1");
		StringBuilder response = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = Files.newBufferedReader(path, charset);
			String line;
			while ((line = reader.readLine()) != null) {
                response.append(line).append(System.getProperty("line.separator"));
			}
		} catch (IOException x) {
			log.error("IOException when reading GVR file", x);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					log.error("Unexpected IOException when closing BufferedReader.", e);
				}
			}
		}
		
		return response.toString();
	}
}
