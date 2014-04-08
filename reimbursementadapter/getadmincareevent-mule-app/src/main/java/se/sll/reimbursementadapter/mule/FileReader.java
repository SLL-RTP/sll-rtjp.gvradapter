package se.sll.reimbursementadapter.mule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
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

public class FileReader {

    /** Logger */
    private static final Logger LOG = LoggerFactory.getLogger(FileReader.class);
	
	public List<Path> getFileList(String requestParam) {
		Path folderToIterate = FileSystems.getDefault().getPath("c:\\Temp");

		String str = requestParam;
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
		Date date;
		try {
			date = df.parse(str);
		} catch (ParseException e1) {
			throw new InvalidParameterException("The date parameter was not valid: " + requestParam);
		}
		final long epoch = date.getTime();
		
		// Creating the filter
		DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {

			@Override
			public boolean accept(Path entry) throws IOException {
				BasicFileAttributeView basicAttrsView = Files.getFileAttributeView(entry, BasicFileAttributeView.class);
				BasicFileAttributes basicAttrs =  basicAttrsView.readAttributes();
				boolean isLastModifiedAfterLocalDate = FileTime.fromMillis(epoch).compareTo(basicAttrs.lastModifiedTime()) <= 0;
				return basicAttrs.isRegularFile() && isLastModifiedAfterLocalDate;
			}
		};

		List<Path> response = new ArrayList<>();
		DirectoryStream<Path> ds = null;
		try  {
			ds = Files.newDirectoryStream(folderToIterate, filter);
			for (Path p : ds) {
				response.add(p);
			}

		} catch (IOException e) {
            LOG.error("Exception when reading the directory stream for the configured folder: "
                    + folderToIterate.getFileName(), e);
		} finally {
			if (ds != null) {
				try {
					ds.close();
				} catch (IOException e) {
					LOG.error("Exception when closing DirectoryStream", e);
				}
			}
		}

		return response;
	}

	public static String readFile(Path path) {
		Charset charset = Charset.forName("ISO-8859-1");
		CharsetDecoder decoder = charset.newDecoder();
		StringBuilder response = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = Files.newBufferedReader(path, charset);
			String line;
			while ((line = reader.readLine()) != null) {
				CharBuffer convert = decoder.decode(ByteBuffer.wrap(line.getBytes()));
				response.append(convert).append(System.getProperty("line.separator"));
			}
		} catch (IOException x) {
			System.err.format("IOException: %s%n", x);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					LOG.error("IOException when closing the BufferedReader.", e);
				}
			}
		}
		
		return response.toString();
	}
}
