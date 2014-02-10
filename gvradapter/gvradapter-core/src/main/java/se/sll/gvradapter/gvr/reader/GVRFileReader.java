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
import org.springframework.stereotype.Service;
import se.sll.gvradapter.admincareevent.service.GVRJobService;

/**
 * Handles listing all the GVR files that are newer (last changed) than the
 * incoming date parameter as well as reading a specified file (Path).
 */
@Component
public class GVRFileReader {
	
	private static final Logger log = LoggerFactory.getLogger(GVRFileReader.class);

    @Value("${pr.ftp.gvr.localPath:/tmp/gvr/in}")
    private String localPath;

    @Value("${pr.gvr.timestampFormat:yyyyMMddHHmmss}")
    private String gvrTimestampFormat;

	/**
	 * Gets a list of file {@link Path}s in a configured directory that has a modified
	 * date that is newer than the date parameter provided.
	 * 
	 * @param inDate The date to compare the files with (format: yyyyMMddHHmmss).
	 * @return a List of {@link Path} objects.
	 * @throws InvalidParameterException If the supplied date format is not valid.
	 */
	public List<Path> getFileList(String inDate) throws InvalidParameterException {
		Path folderToIterate = FileSystems.getDefault().getPath(localPath);
		
		log.info("Reading files from date: " + inDate + " and path: " + folderToIterate.toString());

        SimpleDateFormat df = new SimpleDateFormat(gvrTimestampFormat);
		Date date;
		try {
			date = df.parse(inDate);
		} catch (ParseException e1) {
			log.error("The supplied date parameter (" + inDate + ") is not valid.", e1);
			throw new InvalidParameterException("The date parameter was not valid: " + inDate);
		}
		final long epoch = date.getTime();
		
		// Creating the filter
		DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {

			@Override
			public boolean accept(Path entry) throws IOException {
                boolean isXML = entry.getFileName().toString().toLowerCase().endsWith(".xml");
				// Filter reads basic attributes and accepts all the files with lastModifiedTime > inDate
				BasicFileAttributeView basicAttrsView = Files.getFileAttributeView(entry, BasicFileAttributeView.class);
				BasicFileAttributes basicAttrs =  basicAttrsView.readAttributes();
				boolean isLastModifiedAfterLocalDate = FileTime.fromMillis(epoch).compareTo(basicAttrs.lastModifiedTime()) <= 0;
				return isXML && basicAttrs.isRegularFile() && isLastModifiedAfterLocalDate;
			}
		};

		// Apply the filter to all the files in the configured in directory
		List<Path> response = new ArrayList<Path>();
		DirectoryStream<Path> ds = null;
		try  {
			ds = Files.newDirectoryStream(folderToIterate, filter);
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
