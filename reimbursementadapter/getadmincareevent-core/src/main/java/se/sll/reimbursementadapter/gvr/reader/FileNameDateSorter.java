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

import java.nio.file.Path;
import java.util.Comparator;
import java.util.Date;

/**
 * Basic comparator that sorts GVR files (Paths) based on their file name.
 * The file name is extracted from the file name using the provided GVRFileReader.
 */
public class FileNameDateSorter implements Comparator<Path> {

    /** The {@link GVRFileReader} object to use for extracting dates from the file names. */
    private GVRFileReader gvrFileReader;

    public FileNameDateSorter(GVRFileReader gvrFileReader) {
        this.gvrFileReader = gvrFileReader;
    }

    @Override
    public int compare(Path o1, Path o2) {
        Date file1UpdatedTime = gvrFileReader.getDateFromGVRFile(o1);
        Date file2UpdatedTime = gvrFileReader.getDateFromGVRFile(o2);
        return file1UpdatedTime.compareTo(file2UpdatedTime);
    }
}
