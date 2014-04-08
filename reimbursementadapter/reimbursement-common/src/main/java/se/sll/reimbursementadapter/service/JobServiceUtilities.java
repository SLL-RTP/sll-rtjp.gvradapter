package se.sll.reimbursementadapter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class JobServiceUtilities {

    private static final Logger LOG = LoggerFactory.getLogger(JobServiceUtilities.class);

    /**
     * Logs input from an input stream to error or info level.
     *
     * @param is the input stream.
     * @param err if it's error, otherwise is info assumed.
     */
    public void log(final InputStream is, final boolean err) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line = reader.readLine();
            while (line != null) {
                if (err) {
                    LOG.error(line);
                }  else {
                    LOG.info(line);
                }
                line = reader.readLine();
            }
        } catch (Exception e) {
            LOG.error("Error while reading input stream", e);
        }
    }

    /**
     * Force a close operation and ignore errors.
     *
     * @param c the closeable to close.
     */
    public void close(Closeable c) {
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
    public void handleInputStream(final InputStream is, final boolean err) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                log(is, err);
            }

        }).run();
    }

}
