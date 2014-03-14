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
package se.sll.reimbursementadapter.admincareevent.service;

import java.io.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import se.sll.reimbursementadapter.admincareevent.jmx.StatusBean;

/**
 * Service executing batch jobs. Currently it's only about fetching master data files, and rebuild
 * the index.
 * 
 * @author Peter
 */
@Service
public class CodeServerJobService {
    //
    private static final Logger log = LoggerFactory.getLogger(CodeServerJobService.class);

    @Value("${pr.ftp.script:}")
    private String script;
    
    @Value("${pr.ftp.localPath}")
    private String localPath;

    @Autowired
    private CodeServerMEKCacheManagerService codeServerMekCacheService;
    
    @Autowired
    private StatusBean statusBean;

    /**
     * Invokes and externally managed script to fetch master data, and
     * then revalidates the index. <p>
     * 
     * The actual cron expression is configurable "pr.ftp.cron", and the script runs in the current working
     * directory as the configuration setting "pr.ftp.localPath"
     */
    @Scheduled(cron="${pr.ftp.cron}")
    public void fetchCodeServerFiles() {
        if (script.length() == 0) {
            log.warn("Batch ftp script has not been defined, please check configuration property \"pr.ftp.script\"");
            return;
        }
        log.info("Fetch files using script {}", script);
        boolean success = false;
        statusBean.start(script);
        try {
            final Process p = Runtime.getRuntime().exec(script, null, new File(localPath));
            close(p.getOutputStream());
            handleInputStream(p.getInputStream(), false);
            handleInputStream(p.getErrorStream(), true);
            p.waitFor();
            if (p.exitValue() != 0) {
                log.error("Script {} returned with exit code {}", script, p.exitValue());
            } else {
                log.info("Script {} completed successfully", script);
                codeServerMekCacheService.revalidate();
                success = true;
            }
        } catch (Exception e) {
            log.error("Unable to update from master data " + script, e);
        } finally {
            statusBean.stop(success);
        }
    }

    /**
     * Logs input from an input stream to error or info level.
     * 
     * @param is the input stream.
     * @param err if it's error, otherwise is info assumed.
     */
    private void log(final InputStream is, final boolean err) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(is));
            String line = reader.readLine();
            while (line != null) {
                if (err) {
                    log.error(line);
                }  else {
                    log.info(line);
                }
                line = reader.readLine();
            }    
        } catch (Exception e) {
            log.error("Error while reading input stream", e);
        } finally {
            close(reader);
        }
    }

    /**
     * Force a close operation and ignore errors.
     * 
     * @param c the closeable to close.
     */
    private void close(Closeable c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (IOException e) {
        }
    }
    
    /**
     * Reads an input stream in the background (separate thread).
     * 
     * @param is the input stream.
     * @param err if it's about errors, otherwise is info assumed.
     */
    private void handleInputStream(final InputStream is, final boolean err) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                log(is, err);
            }

        }).run();        
    }
}
