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
import se.sll.reimbursementadapter.service.JobServiceUtilities;

/**
 * Class that handles configuration regarding, and reading of, files from the GVR
 * source system.
 */
@Service
public class GVRJobService {

    /** The Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(GVRJobService.class);

    /** The script file name to execute for fetching files from GVR. */
    @Value("${pr.gvr.ftp.script:}")
    private String script;
    /** The local path to store fetched GVR-files (and read files from). */
    @Value("${pr.gvr.ftp.localPath}")
    private String localPath;

    /** The StatusBean for reporting execution status. */
    @Autowired
    private StatusBean statusBean;

    /**
     * Invokes an externally managed script to fetch master data, and
     * then revalidates the index. <p>
     *
     * The actual cron expression is configurable using "pr.ftp.gvr.cron", and the script runs in the current working
     * directory as the configuration setting "pr.ftp.gvr.localPath".
     */
    @Scheduled(cron="${pr.gvr.ftp.cron}")
    public void ftpFetchGVRScript() {
        JobServiceUtilities jobServiceUtilities = new JobServiceUtilities();
        if (script.length() == 0) {
            LOG.warn("Batch ftp script has not been defined, please check configuration property \"pr.ftp.gvr.script\"");
            return;
        }
        LOG.info("Fetch files using script {}", script);
        boolean success = false;
        statusBean.start(script);
        try {
            // Append a file.separator to the end of the localPath if one is not configured.
            if (!localPath.endsWith("/") && !localPath.endsWith("\\")) {
                localPath += System.getProperty("file.separator");
            }

            // Start the process and run the configured script.
            final Process p = Runtime.getRuntime().exec(localPath + script, null);

            // Pipe the output stream from the script to the logger.
            jobServiceUtilities.close(p.getOutputStream());
            jobServiceUtilities.handleInputStream(p.getInputStream(), false);
            jobServiceUtilities.handleInputStream(p.getErrorStream(), true);

            // Wait for the process to finish executing and handle any error status response.
            p.waitFor();
            if (p.exitValue() != 0) {
                LOG.error("Script {} returned with exit code {}", script, p.exitValue());
            } else {
                LOG.info("Script {} completed successfully", script);
                success = true;
            }
        } catch (Exception e) {
            LOG.error("Unable to update from master data " + script, e);
        } finally {
            statusBean.stop(success);
        }
    }

}
