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
package se.sll.reimbursementadapter.admincareevent.jmx;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.support.MetricType;
import org.springframework.stereotype.Component;

import se.sll.reimbursementadapter.admincareevent.service.CodeServerMEKCacheManagerService;
import se.sll.reimbursementadapter.jmx.HistoryTimer;

/**
 * Handles all the JMX information and operation exporting.
 */
@Component
@ManagedResource(objectName = "se.sll.reimbursementadapter.admincareevent.jmx:name=StatusBean", description="Status information")
public class StatusBean {

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(StatusBean.class);
    /** A copy of the current code server cache. */
    @Autowired
    private CodeServerMEKCacheManagerService codeServerMEKCacheManagerService;

    /** The history length that the JMX bean should support. */
    private int historyLength = 1000;

    /** A ThreadLocal Stack of Samples that the bean uses for storing request and response information */
    private static ThreadLocal<Stack<Sample>> samples = new ThreadLocal<Stack<Sample>>() {
        @Override
        public Stack<Sample> initialValue() {
            return new Stack<>();
        }
    };

    /** A map of currently active {@link se.sll.reimbursementadapter.jmx.HistoryTimer} objects. */
    private static Map<String, HistoryTimer> timerMap = new HashMap<>();

    /** A pre-initialized {@link se.sll.reimbursementadapter.admincareevent.jmx.StatusBean.Concurrency}. */
    private static Concurrency concurrency = new Concurrency();

    /**
     * Check the ability to allocate memory.
     */
    private void checkAlloc() {
        final byte[] bytes = new byte[1024 * 1024];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte)0xff;
        }
        LOG.info("health-check memory alloc: OK");
    }

    /**
     * Check the ability to LOG.
     */
    private void checkLog() {
        final String msg = "health-check LOG: OK";
        LOG.error(msg);
        LOG.debug(msg);
        LOG.trace(msg);
        LOG.info(msg);
    }

    /**
     * Performs health check, i.e. are connections, memory, logs working as expected.
     */
    @ManagedOperation(description="Performs health check, i.e. are connections, memory, logs working as expected")
    public void healthCheck() {
        checkAlloc();
        checkLog();
    }

    /**
     * Returns active (ongoing) requests.
     * @return active (ongoing) requests.
     */
    @ManagedMetric(category="utilization", displayName="Active (ongoing) requests", metricType=MetricType.COUNTER, unit="request")
    public long getNumActiveRequests() {
        return concurrency.getActiveRequests();
    }

    /**
     * Returns Total requests served.
     * @return Total requests served.
     */
    @ManagedMetric(category="utilization", displayName="Total requests served", metricType=MetricType.COUNTER, unit="request")
    public long getNumTotalRequests() {
        return concurrency.getTotalRequests();
    }

    /**
     * Returns Total errors served.
     * @return Total errors served.
     */
    @ManagedMetric(category="utilization", displayName="Total errors served", metricType=MetricType.COUNTER, unit="request")
    public long getNumTotalErrors() {
        return concurrency.getTotalErrors();
    }

    /**
     * Returns Total size of HSA mapping index.
     * @return Total size of HSA mapping index.
     */
    @ManagedMetric(category="memory", displayName="Total size of HSA mapping index", metricType=MetricType.COUNTER, unit="size")
    public int getTotalIndexSize() {
        return codeServerMEKCacheManagerService.getCurrentIndex().size();
    }

    /**
     * Returns performance metrics (JSON strings) in millisceonds for all instrumented operations.
     * @return performance metrics (JSON strings) in millisceonds for all instrumented operations.
     */
    @ManagedOperation(description="Returns performance metrics (JSON strings) in millisceonds for all instrumented operations")
    public String[] getPerformanceMetricsAsJSON() {
        final Collection<HistoryTimer> c = timerMap.values();
        final String[] list = new String[c.size()];
        int i = 0;
        for (final HistoryTimer t : c) {
            t.recalc();
            list[i++] = t.toString();
        }
        return list;
    }

    /**
     * Clears sampled performance metrics (timed statistics).
     */
    @ManagedOperation(description="Clears sampled performance metrics (timed statistics)")
    public void clearPerformanceMetrics() {
        timerMap.clear();
    }

    /**
     * Sets the length of request history for timed statistics
     * @param historyLength The length of request history for timed statistics to set.
     */
    @ManagedAttribute(description="Sets the length of request history for timed statistics", defaultValue="1000")
    public void setHistoryLength(@ManagedOperationParameter(name="historyLength", description="Indicates history of requests to keep/calculate averge timed statistcis") int historyLength) {
        if (historyLength > 0) {
            this.historyLength = historyLength;
        }
    }

    /**
     * Returns the length of request history for timed statistics.
     * @return he length of request history for timed statistics.
     */
    @ManagedAttribute(description="Returns the length of request history for timed statistics")
    public int getHistoryLength() {
        return historyLength;
    }

    /**
     * Returns active service contract names, i.e. WSDL operations that have been accessed since last startup.
     * @return active service contract names, i.e. WSDL operations that have been accessed since last startup.
     */
    @ManagedOperation(description="Returns active service contract names, i.e. WSDL operations that have been accessed since last startup")
    public String[] getServiceNames() {
        final Set<String> set = timerMap.keySet();
        return set.toArray(new String[set.size()]);
    }

    /**
     * Returns a string with the GUID of the last sample.
     * @return a string with the GUID of the last sample.
     */
    public String getGUID() {
        return samples.get().peek().getGUID();
    }

    /**
     * Returns the name of the last sample.
     * @return the name of the last sample.
     */
    public String getName() {
        return samples.get().peek().getName();
    }

    /**
     * Starts processing of a single request.
     * @param path The path to register the sample on.
     */
    public void start(final String path) {
        if (samples.get().isEmpty()) {
            concurrency.inc();
        }
        samples.get().push(new Sample(path));
    }

    /**
     * Stops the processing.
     * @param success if the request was handled without errors or not.
     */
    public void stop(boolean success) {
        final Sample sample = samples.get().pop();
        final String name = sample.getName();
        final long elapsed = sample.elapsed();
        HistoryTimer timer = timerMap.get(name);
        if (timer == null) {
            timer = new HistoryTimer(name, this.historyLength);
            timerMap.put(name, timer);
        }
        timer.add(elapsed, success);
        if (samples.get().size() == 0) {
            concurrency.dec(success);
        }
    }

    /**
     * Samples processing time for one transaction.
     */
    static class Sample {
        /** Timestamp in milliseconds. */
        private long timestamp;
        /** Name of the path. */
        private String name;
        /** The GUID. */
        private String guid;

        /**
         * Basic constructor that sets the name of the path.
         * @param name The name of the path.
         */
        public Sample(final String name) {
            this.timestamp = System.currentTimeMillis();
            this.name = name;
            this.guid = UUID.randomUUID().toString();
        }

        /**
         * Returns the GUID for the Sample.
         * @return the GUID for the Sample.
         */
        public String getGUID() {
            return guid;
        }

        /**
         * Returns the Name of the sample.
         * @return the Name of the sample.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the elapsed time in milliseconds since the Sample was created.
         * @return the elapsed time in milliseconds since the Sample was created.
         */
        public long elapsed() {
            final long time = (System.currentTimeMillis() - timestamp);
            return (time < 0) ? 0 : time;
        }
    }

    /**
     * Synchronized class for handling statistics.
     */
    static class Concurrency {
        /** The number of active requests. */
        private long activeRequests;
        /** The total number of requests. */
        private long totalRequests;
        /** The total number of errors. */
        private long totalErrors;

        /**
         * Increment the activeRequests and totalRequests.
         * Called when a new sample is added.
         */
        public synchronized void inc() {
            activeRequests++;
            totalRequests++;
        }

        /**
         * Decrease the active requests and increment the total errors if the request was not successful.
         * called when the request is done.
         * @param success was the request processed without errors?
         */
        public synchronized void dec(boolean success) {
            activeRequests--;
            if (!success) {
                totalErrors++;
            }
        }

        /**
         * Returns the active number of requests.
         * @return the active number of requests.
         */
        public synchronized long getActiveRequests() {
            return activeRequests;
        }

        /**
         * Returns the total number of requests.
         * @return the total number of requests.
         */
        public synchronized long getTotalRequests() {
            return totalRequests;
        }

        /**
         * Returns the total number of errors.
         * @return the total number of errors.
         */
        public synchronized long getTotalErrors() {
            return totalErrors;
        }

    }
}
