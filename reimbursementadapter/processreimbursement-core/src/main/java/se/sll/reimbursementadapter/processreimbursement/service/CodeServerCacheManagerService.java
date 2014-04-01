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
package se.sll.reimbursementadapter.processreimbursement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.sll.reimbursementadapter.parser.TermItem;
import se.sll.reimbursementadapter.processreimbursement.model.GeographicalAreaState;
import se.sll.reimbursementadapter.processreimbursement.util.CodeServerCacheBuilder;
import se.sll.reimbursementadapter.util.FileObjectStore;

import java.util.Map;

/**
 * Manages the main cache tree for the processreimbursementevent consisting of a single code server table (BASOMRNY).
 * 
 * The index is built from code-server master XML files, and the result is saved/cached on local disk.
 * The local cache is always used if it exists, and the only way to rebuild the index is to 
 * invoke the <code>revalidate</code> method, which is intended to be called by an external scheduled
 * job.
 *
 * @see #revalidate()
 *
 */
@Service
public class CodeServerCacheManagerService {

    /** The local path to read GVR files from. */
    @Value("${pr.ftp.localPath:}")
    private String localPath;
    /** The local file to write the HSA-index to. */
    @Value("${pr.indexFile:/tmp/hsa-index.gz}")
    private String fileName;
    /** The file name for the Geographical Area file. */
    @Value("${pr.geographicalAreaFile:BASOMRNY.XML}")
    private String geographicalAreaFile;

    /** Flag indicating if the service is currently processing. */
    private boolean busy;
    /** The singleton instance of this class. */
    private static CodeServerCacheManagerService instance;
    /** Logger. */
    private static final Logger log = LoggerFactory.getLogger(CodeServerCacheManagerService.class);
    /** The current index. */
    private Map<String, TermItem<GeographicalAreaState>> currentIndex;
    /** The build lock object. */
    private final Object buildLock = new Object();
    /** The {@link se.sll.reimbursementadapter.util.FileObjectStore} to write the HSA-index with. */
    private FileObjectStore fileObjectStore = new FileObjectStore();

    /** Constructor needed to setup the Mule and some unit test context that are not launched via Spring. */
    public CodeServerCacheManagerService() {
        if (instance == null) {
            instance = this;
        }
    }

    /**
     * Returns a full path for a given file name using the configured values.
     * @param name The File name to read.
     * @return A String with the full path to the file.
     */
    private String path(String name) {
        return localPath + (localPath.endsWith("/") ? "" : "/") + name;
    }

    /**
     * Builds the index from the parameters supplied from the builder pattern helper methods.
     *
     * @return The index fully built from the configured master Code Server files.
     */
    private Map<String, TermItem<GeographicalAreaState>> build() {
        log.debug("build index");

        CodeServerCacheBuilder builder = new CodeServerCacheBuilder()
        .withGeographicalAreaFile(path(geographicalAreaFile));

        final Map<String, TermItem<GeographicalAreaState>> index = builder.build();
        
        log.debug("build index: done");

        return index;
    }

    /**
     * Returns if index build process is active.
     * 
     * @return true if the index is under construction, otherwise false.
     */
    public boolean isBusy() {
        return busy;
    }

    protected void setBusy(boolean busy) {
        this.busy = busy;
    }

    /**
     * Rebuilds the index from XML source. <p>
     * 
     * Can only be invoked once, i.e. if a rebuild process is ongoing
     * this method returns without doing anything.
     */
    public void revalidate() {
        log.debug("revalidate index");

        if (isBusy()) {
            return;
        }

        synchronized (buildLock) {
            if (isBusy()) {
                return;
            }
            setBusy(true);
            try {
                final Map<String, TermItem<GeographicalAreaState>> index = build();
                fileObjectStore.write(index, fileName);
                setCurrentIndex(index);
            } finally {
                setBusy(false);
            }
        }
    }


    /**
     * Returns the singleton instance. <p>
     * 
     * Note: This is a work-around, since the parent mule-app doesn't use spring annotations
     * as configuration mechanism.
     * 
     * @return the singleton instance, or null if none has been created.
     */
    public static CodeServerCacheManagerService getInstance() {
        return instance;
    }
    
    /**
     * Returns the current index, or null if none exists.
     * 
     * @return the current index.
     */

    public synchronized Map<String, TermItem<GeographicalAreaState>> getCurrentIndex() {
        if (currentIndex == null) {
        	Map<String, TermItem<GeographicalAreaState>> index = fileObjectStore.read(fileName);
            setCurrentIndex(index);
        }
        return this.currentIndex;
    }

    /**
     * Updates the current index.
     * 
     * @param currentIndex the new index.
     */
    protected synchronized void setCurrentIndex(Map<String, TermItem<GeographicalAreaState>> currentIndex) {
        this.currentIndex = currentIndex;
        log.info("current index set, size: {}", (this.currentIndex == null) ? 0 : this.currentIndex.size());
    }
}
