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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import se.sll.reimbursementadapter.admincareevent.jmx.StatusBean;

import java.io.*;

/**
 * Class that handles configuration regarding, and reading of, files from the GVR
 * source system.
 */
@Service
public class GVRJobService {

    //
    private static final Logger log = LoggerFactory.getLogger(GVRJobService.class);

    // TODO REB: Is it safe to remove the ':' at the end of this property or is it needed?
    @Value("${pr.ftp.gvr.script:}")
    private String script;

    // TODO REB: Remove the hard coded default path in this property?
    @Value("${pr.ftp.gvr.localPath:/tmp/gvr/in}")
    private String localPath;

    @Autowired
    private StatusBean statusBean;

    /**
     * Invokes an externally managed script to fetch master data, and
     * then revalidates the index. <p>
     *
     * The actual cron expression is configurable using "pr.ftp.gvr.cron", and the script runs in the current working
     * directory as the configuration setting "pr.ftp.gvr.localPath".
     * TODO: Break out a common method for this and CodeServerJobService.
     */
    @Scheduled(cron="${pr.ftp.gvr.cron}")
    public void ftpFetchGVRScript() {
        if (script.length() == 0) {
            log.warn("Batch ftp script has not been defined, please check configuration property \"pr.ftp.gvr.script\"");
            return;
        }
        log.info("Fetch files using script {}", script);
        boolean success = false;
        statusBean.start(script);
        try {
            //final Process p = Runtime.getRuntime().exec(script, null, new File(gvrLocalPath));
        	// TODO REB: Only include file separator if needed? (Check if the localPath parameter already ends with one.)
            final Process p = Runtime.getRuntime().exec(localPath + System.getProperty("file.separator") + script, null);
            close(p.getOutputStream());
            handleInputStream(p.getInputStream(), false);
            handleInputStream(p.getErrorStream(), true);
            p.waitFor();
            if (p.exitValue() != 0) {
                log.error("Script {} returned with exit code {}", script, p.exitValue());
            } else {
                log.info("Script {} completed successfully", script);
                //codeServerMekCacheService.revalidate();	// TODO REB: Remove or implement this?
                success = true;
            }
        } catch (Exception e) {
            log.error("Unable to update from master data " + script, e);
        } finally {
            statusBean.stop(success);
        }
    }
    
    // TODO REB: The following methods (log, close, handleInputStream) are duplicates of the ones in CodeServerJobService, perhaps
    // we should move these to a common class?

    /**
     * Logs input from an input stream to error or info level.
     *
     * @param is the input stream.
     * @param err if it's error, otherwise is info assumed.
     */
    private void log(final InputStream is, final boolean err) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
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
        	// ignore
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
