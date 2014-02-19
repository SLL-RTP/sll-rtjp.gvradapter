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
package se.sll.gvradapter.admincareevent.util;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple file store for {@link Serializable} objects. <p>
 * 
 * Files are compressed with GZIP.
 * 
 * @author Peter
 */
public class FileObjectStore {
    private static final Logger log = LoggerFactory.getLogger(FileObjectStore.class);

    /**
     * Reads an object from file.
     * 
     * @param fileName the file name (full path).
     * @return the object or null if unable to read, ot if the file doesn't exists.
     */
    @SuppressWarnings("unchecked")
    public <T> T read(final String fileName) {
        ObjectInputStream is = null;
        try {
            is = new ObjectInputStream(new GZIPInputStream(new FileInputStream(fileName)));
            return (T) is.readObject();
        } catch (Exception e) {
            log.warn(e.toString());
        } finally {
            close(is);
        }
        return null;            
    }

    /**
     * Writes an object to file.
     * 
     * @param object the object.
     * @param fileName the file name (full path).
     * @return the object.
     */
    public <T> T write(T object, final String fileName) {
        ObjectOutputStream os = null;
        try {
            os = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(fileName)));
            os.writeObject(object);
        } catch (Exception e) {
            log.error("Unable to write HSA index to file: " + fileName, e);
        } finally {
            close(os);
        }
        return object;
    }

    //
    private void close(Closeable c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (IOException e) {}
    }

}
