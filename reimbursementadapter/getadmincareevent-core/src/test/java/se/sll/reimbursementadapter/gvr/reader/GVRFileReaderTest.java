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

import java.io.Reader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import se.sll.reimbursementadapter.TestSupport;

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

        List<Path> pathList = gvrFileReader.getFileList("2014-01-01T090000.001+0000", "2014-01-01T100000.000+0000");
        Assert.assertTrue(pathList.size() == 0);
    }

    @Test
    public void testGetFileListByFilenameStartInclusiveEquals() throws Exception {
        gvrFileReader.setDateFilterMethod(DateFilterMethod.FILENAME);

        List<Path> pathList = gvrFileReader.getFileList("2014-01-01T090000.000+0000", "2014-01-01T090000.000+0000");
        Assert.assertTrue(pathList.size() == 1);
        Assert.assertTrue(pathList.get(0).getFileName().toString().equals("ERSMO_2014-01-01T090000.000+0000.xml"));
    }

    @Test
    public void testGetFileListByFilenameEndInclusiveEquals() throws Exception {
        gvrFileReader.setDateFilterMethod(DateFilterMethod.FILENAME);

        List<Path> pathList = gvrFileReader.getFileList("2014-01-01T110000.000+0200", "2014-01-01T110000.000+0200");
        Assert.assertTrue(pathList.size() == 1);
        Assert.assertTrue(pathList.get(0).getFileName().toString().equals("ERSMO_2014-01-01T090000.000+0000.xml"));
    }

    @Test
    public void testGetFileListByFilenameEndHighExclusive() throws Exception {
        gvrFileReader.setDateFilterMethod(DateFilterMethod.FILENAME);

        List<Path> pathList = gvrFileReader.getFileList("2014-01-01T000000.000+0200", "2014-01-01T105959.999+0200");
        Assert.assertTrue(pathList.size() == 0);
    }

    @Test
    public void testGetFileListByFilenameAllInclusiveSorted() throws Exception {
        gvrFileReader.setDateFilterMethod(DateFilterMethod.FILENAME);

        List<Path> pathList = gvrFileReader.getFileList("2014-01-01T000000.000+0200", "2014-04-05T000000.000+0200");
        Assert.assertTrue(pathList.size() == 8);

        List<String> testList = new ArrayList<>();
        testList.add("ERSMO_2014-01-01T090000.000+0000.xml");
        testList.add("ERSMO_2014-02-01T080000.000+0000.xml");
        testList.add("ERSMO_2014-02-02T080000.000+0000.xml");
        testList.add("ERSMO_2014-02-03T080000.000+0000.xml");
        testList.add("ERSMO_2014-02-04T080000.000+0000.xml");
        testList.add("ERSMO_2014-02-05T080000.000+0000.xml");
        testList.add("ERSMO_2014-02-06T080000.000+0000.xml");
        testList.add("ERSMO_2014-04-04T080000.000+0000.xml");

        int index = 0;
        for(Path f : pathList) {
            Assert.assertEquals("File index " + index, testList.get(index++), f.getFileName().toString());
        }
    }

    @Test
    public void testGetFileListByFilenameMiddleInclusive() throws Exception {
        gvrFileReader.setDateFilterMethod(DateFilterMethod.FILENAME);

        List<Path> pathList = gvrFileReader.getFileList("2014-02-02T100000.001+0200", "2014-04-04T095959.999+0200");
        Assert.assertTrue(pathList.size() == 4);

        List<String> testList = new ArrayList<>();
        testList.add("ERSMO_2014-02-03T080000.000+0000.xml");
        testList.add("ERSMO_2014-02-04T080000.000+0000.xml");
        testList.add("ERSMO_2014-02-05T080000.000+0000.xml");
        testList.add("ERSMO_2014-02-06T080000.000+0000.xml");
        for(Path f : pathList) {
            Assert.assertTrue(testList.contains(f.getFileName().toString()));
        }
    }

    @Test
    public void testGetFileListByFilenameOneFileDST() throws Exception {
        gvrFileReader.setDateFilterMethod(DateFilterMethod.FILENAME);

        List<Path> pathList = gvrFileReader.getFileList("2014-07-01T100000.000+0200", "2014-07-01T100000.000+0100");
        Assert.assertTrue(pathList.size() == 1);
        Assert.assertEquals("File names", pathList.get(0).getFileName().toString(), "ERSMO_2014-07-01T080000.000+0000.xml");
    }

    @Test
    public void testGetDateFromGVRFileName() throws Exception {
        gvrFileReader.setDateFilterMethod(DateFilterMethod.FILENAME);

        // Results in a list with only ERSMO_2014-02-01T080000.000+0000.xml
        List<Path> pathList = gvrFileReader.getFileList("2014-02-01T080000.000+0000", "2014-02-01T080000.000+0000");

        // Create dates to compare the result with
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss.SSSZZZZZ");
        Date expectedDate = sf.parse("2014-02-01T100000.000+0200");
        Date wrongDate    = sf.parse("2014-02-01T100000.001+0200");

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
        Path entry = FileSystems.getDefault().getPath(localPath + "ERSMO_2014-02-01T080000.000+0000.xml");
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
        Path entry = FileSystems.getDefault().getPath(localPath + "ERSMO_2014-02-02T080000.000+0000.xml");
        Files.setLastModifiedTime(entry, FileTime.fromMillis(expectedDate.getTime()));

        Date fileDate = gvrFileReader.getDateFromGVRFile(entry);
        Assert.assertEquals(expectedDate, fileDate);
        Assert.assertFalse(wrongDate.equals(fileDate));
    }

    @Test
    public void testGetFileListByMetadataAllInclusiveSorted() throws Exception {
        gvrFileReader.setDateFilterMethod(DateFilterMethod.METADATA);

        String localPath = gvrFileReader.getLocalPath();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmssSSS");

        // Knowingly set the dates a year back in order to not get the regular files to interfere.
        Date expectedDate = sf.parse("20130101090000000");
        Path entry = FileSystems.getDefault().getPath(localPath + "ERSMO_2014-01-01T090000.000+0000.xml");
        Files.setLastModifiedTime(entry, FileTime.fromMillis(expectedDate.getTime()));

        // Knowingly set the date earlier here to prove that the sorting is not done by filename.
        expectedDate = sf.parse("20120201090000000");
        entry = FileSystems.getDefault().getPath(localPath + "ERSMO_2014-02-01T080000.000+0000.xml");
        Files.setLastModifiedTime(entry, FileTime.fromMillis(expectedDate.getTime()));

        expectedDate = sf.parse("20130202100000000");
        entry = FileSystems.getDefault().getPath(localPath + "ERSMO_2014-02-02T080000.000+0000.xml");
        Files.setLastModifiedTime(entry, FileTime.fromMillis(expectedDate.getTime()));

        expectedDate = sf.parse("20130203100000000");
        entry = FileSystems.getDefault().getPath(localPath + "ERSMO_2014-02-03T080000.000+0000.xml");
        Files.setLastModifiedTime(entry, FileTime.fromMillis(expectedDate.getTime()));

        List<Path> pathList = gvrFileReader.getFileList("2012-01-01T000000.000+0200", "2013-04-04T000000.000+0200");
        Assert.assertEquals(pathList.size(), 5);

        // Set up a list with the expected file list in the expected order.
        List<String> testList = new ArrayList<>();
        testList.add("ERSMO_2014-02-01T080000.000+0000.xml");
        testList.add("ERSMO_2014-01-01T090000.000+0000.xml");
        testList.add("ERSMO_2014-02-02T080000.000+0000.xml");
        testList.add("ERSMO_2014-02-03T080000.000+0000.xml");
        testList.add("ERSMO_2014-07-01T080000.000+0000.xml");

        int index = 0;
        for(Path f : pathList) {
            Assert.assertEquals("File index " + index, testList.get(index++), f.getFileName().toString());
        }
    }

    @Test
    public void testGetReaderForFile() throws Exception {
        String localPath = gvrFileReader.getLocalPath();
        Path entry = FileSystems.getDefault().getPath(localPath + "ERSMO_2014-02-02T080000.000+0000.xml");
        Reader reader = gvrFileReader.getReaderForFile(entry);
        Assert.assertNotNull(reader);
    }
}

