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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Date;

/**
 * Filters a file {@link Path} on whether the last modified time of the file
 * falls within the provided fromDate and toDate parameters.
 */
public class MetadataDateRangeFilter implements DirectoryStream.Filter<Path> {

    private Date fromDate;
    private Date toDate;


    public MetadataDateRangeFilter(Date fromDate, Date toDate) {
        this.fromDate = fromDate;
        this.toDate = toDate;
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
        // Filter reads basic attributes and accepts all the files with lastModifiedTime > inDate
        boolean isXML = entry.getFileName().toString().toLowerCase().endsWith(".xml");

        // Read long epoch from the provided Dates or set to 0L and Long.MAX_VALUE respectively.
        final long fromDateEpoch = fromDate != null ? fromDate.getTime() : 0L;
        final long toDateEpoch = toDate != null ? toDate.getTime() : Long.MAX_VALUE;

        // Fetch file attributes
        BasicFileAttributeView basicAttrsView = Files.getFileAttributeView(entry, BasicFileAttributeView.class);
        BasicFileAttributes basicAttrs =  basicAttrsView.readAttributes();

        // Define response booleans and return
        boolean isLastModifiedAfterLocalFromDate = FileTime.fromMillis(fromDateEpoch).compareTo(basicAttrs.lastModifiedTime()) <= 0;
        boolean isLastModifiedBeforeLocalToDate = FileTime.fromMillis(toDateEpoch).compareTo(basicAttrs.lastModifiedTime()) >= 0;
        return isXML && basicAttrs.isRegularFile() && isLastModifiedAfterLocalFromDate && isLastModifiedBeforeLocalToDate;
    }
}
