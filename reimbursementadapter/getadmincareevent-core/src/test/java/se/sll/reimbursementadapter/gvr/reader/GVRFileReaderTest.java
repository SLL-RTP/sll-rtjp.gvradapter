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

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import se.sll.reimbursementadapter.TestSupport;

import java.io.Reader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Tests various functions in the GVRFileReader component.
 */
public class GVRFileReaderTest extends TestSupport {

    /** Lists files matching a period and provides Readers for individual files. */
    @Autowired
    private GVRFileReader gvrFileReader;

    @Test
    public void testGetFileListByFilenameStartHighExclusive() throws Exception {
        gvrFileReader.setDateFilterMethod(DateFilterMethod.FILENAME);

        List<Path> pathList = gvrFileReader.getFileList("20140101090000001", "20140101100000000");
        Assert.assertTrue(pathList.size() == 0);
    }

    @Test
    public void testGetFileListByFilenameStartInclusiveEquals() throws Exception {
        gvrFileReader.setDateFilterMethod(DateFilterMethod.FILENAME);

        List<Path> pathList = gvrFileReader.getFileList("20140101090000000", "20140101100000000");
        Assert.assertTrue(pathList.size() == 1);
        Assert.assertTrue(pathList.get(0).getFileName().toString().equals("Vardansvar_2014-01-01T090000.xml"));
    }

    @Test
    public void testGetFileListByFilenameEndInclusiveEquals() throws Exception {
        gvrFileReader.setDateFilterMethod(DateFilterMethod.FILENAME);

        List<Path> pathList = gvrFileReader.getFileList("20140101000000000", "20140101090000000");
        Assert.assertTrue(pathList.size() == 1);
        Assert.assertTrue(pathList.get(0).getFileName().toString().equals("Vardansvar_2014-01-01T090000.xml"));
    }

    @Test
    public void testGetFileListByFilenameEndHighExclusive() throws Exception {
        gvrFileReader.setDateFilterMethod(DateFilterMethod.FILENAME);

        List<Path> pathList = gvrFileReader.getFileList("20140101000000000", "20140101085959999");
        Assert.assertTrue(pathList.size() == 0);
    }

    @Test
    public void testGetFileListByFilenameAllInclusive() throws Exception {
        gvrFileReader.setDateFilterMethod(DateFilterMethod.FILENAME);

        List<Path> pathList = gvrFileReader.getFileList("20140101000000000", "20140405000000000");
        Assert.assertTrue(pathList.size() == 8);

        List<String> testList = new ArrayList<String>();
        testList.add("Vardansvar_2014-01-01T090000.xml");
        testList.add("Vardkontakt_2014-02-01T100000.xml");
        testList.add("Vardkontakt_2014-02-02T100000.xml");
        testList.add("Vardkontakt_2014-02-03T100000.xml");
        testList.add("Vardkontakt_2014-02-04T100000.xml");
        testList.add("Vardkontakt_2014-02-05T100000.xml");
        testList.add("Vardkontakt_2014-02-06T100000.xml");
        testList.add("Vardkontakt_2014-04-04T100000.xml");

        for(Path f : pathList) {
            Assert.assertTrue(testList.contains(f.getFileName().toString()));
        }
    }

    @Test
    public void testGetFileListByFilenameMiddleInclusive() throws Exception {
        gvrFileReader.setDateFilterMethod(DateFilterMethod.FILENAME);

        List<Path> pathList = gvrFileReader.getFileList("20140202100000001", "20140404095959999");
        Assert.assertTrue(pathList.size() == 4);

        List<String> testList = new ArrayList<String>();
        testList.add("Vardkontakt_2014-02-03T100000.xml");
        testList.add("Vardkontakt_2014-02-04T100000.xml");
        testList.add("Vardkontakt_2014-02-05T100000.xml");
        testList.add("Vardkontakt_2014-02-06T100000.xml");
        for(Path f : pathList) {
            Assert.assertTrue(testList.contains(f.getFileName().toString()));
        }
    }

    @Test
    public void testGetDateFromGVRFileName() throws Exception {
        gvrFileReader.setDateFilterMethod(DateFilterMethod.FILENAME);

        // Results in a list with only Vardkontakt_2014-02-01T100000.xml
        List<Path> pathList = gvrFileReader.getFileList("20140201100000000", "20140201100000000");

        // Create dates to compare the result with
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        Date expectedDate = sf.parse("20140201100000000");
        Date wrongDate    = sf.parse("20140201100000001");

        // Check the date against the created dates
        Assert.assertEquals(gvrFileReader.getDateFromGVRFile(pathList.get(0)), expectedDate);
        Assert.assertFalse(gvrFileReader.getDateFromGVRFile(pathList.get(0)).equals(wrongDate));
    }

    @Test
    public void testGetDateFromGVRFileMetadata() throws Exception {
        gvrFileReader.setDateFilterMethod(DateFilterMethod.METADATA);

        // Create dates to compare the result with (different than the filename so that we know the metadata is used)
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        Date expectedDate = sf.parse("20140202000000000");
        Date wrongDate    = sf.parse("20140202000000001");

        String localPath = gvrFileReader.getLocalPath();
        Path entry = FileSystems.getDefault().getPath(localPath + "Vardkontakt_2014-02-01T100000.xml");
        Files.setLastModifiedTime(entry, FileTime.fromMillis(expectedDate.getTime()));

        Date fileDate = gvrFileReader.getDateFromGVRFile(entry);
        Assert.assertEquals(expectedDate, fileDate);
        Assert.assertFalse(wrongDate.equals(fileDate));
    }

    @Test
    public void testGetDateFromGVRFileMetadata2() throws Exception {
        gvrFileReader.setDateFilterMethod(DateFilterMethod.METADATA);

        // Create dates to compare the result with (different than the filename so that we know the metadata is used)
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        Date expectedDate = sf.parse("20140203000000000");
        Date wrongDate    = sf.parse("20140203000000001");

        String localPath = gvrFileReader.getLocalPath();
        Path entry = FileSystems.getDefault().getPath(localPath + "Vardkontakt_2014-02-02T100000.xml");
        Files.setLastModifiedTime(entry, FileTime.fromMillis(expectedDate.getTime()));

        Date fileDate = gvrFileReader.getDateFromGVRFile(entry);
        Assert.assertEquals(expectedDate, fileDate);
        Assert.assertFalse(wrongDate.equals(fileDate));
    }

    @Test
    public void testGetReaderForFile() throws Exception {
        String localPath = gvrFileReader.getLocalPath();
        Path entry = FileSystems.getDefault().getPath(localPath + "Vardkontakt_2014-02-02T100000.xml");
        Reader reader = gvrFileReader.getReaderForFile(entry);
        Assert.assertNotNull(reader);
    }
}

