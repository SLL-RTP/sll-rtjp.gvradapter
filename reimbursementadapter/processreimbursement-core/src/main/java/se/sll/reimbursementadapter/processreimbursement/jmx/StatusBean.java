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
package se.sll.reimbursementadapter.processreimbursement.jmx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.*;
import org.springframework.jmx.support.MetricType;
import org.springframework.stereotype.Component;
import se.sll.reimbursementadapter.jmx.HistoryTimer;
import se.sll.reimbursementadapter.processreimbursement.service.CodeServerCacheManagerService;

import java.util.*;

/**
 * Handles all the JMX information and operation exporting.
 */
@Component
@ManagedResource(objectName = "se.sll.reimbursementadapter.processreimbursement.jmx:name=StatusBean", description="Status information")
public class StatusBean {

    private static final Logger log = LoggerFactory.getLogger(StatusBean.class);

    private CodeServerCacheManagerService codeServerMekCacheService;
    
    //
    private int historyLength = 1000;

    //
    private static ThreadLocal<Stack<Sample>> samples = new ThreadLocal<Stack<Sample>>() {
        @Override
        public Stack<Sample> initialValue() {
            return new Stack<Sample>();
        }
    };

    //
    private static Map<String, HistoryTimer> timerMap = new HashMap<String, HistoryTimer>();

    //
    private static Concurrency concurrency = new Concurrency();

    private void checkAlloc() {
        final byte[] bytes = new byte[1024 * 1024];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte)0xff;
        }
        log.info("health-check memory alloc: OK");
    }

    //
    private void checkLog() {
        final String msg = "health-check log: OK";
        log.error(msg);
        log.debug(msg);
        log.trace(msg);
        log.info(msg);
    }

    @ManagedOperation(description="Performs health check, i.e. are connections, memory, logs working as expected")
    public void healthCheck() {
        checkAlloc();
        checkLog();
    }

    @ManagedMetric(category="utilization", displayName="Active (ongoing) requests", metricType=MetricType.COUNTER, unit="request")
    public long getNumActiveRequests() {
        return concurrency.getActiveRequests();
    }

    @ManagedMetric(category="utilization", displayName="Total requests served", metricType=MetricType.COUNTER, unit="request")
    public long getNumTotalRequests() {
        return concurrency.getTotalRequests();
    }

    @ManagedMetric(category="utilization", displayName="Total errors served", metricType=MetricType.COUNTER, unit="request")
    public long getNumTotalErrors() {
        return concurrency.getTotalErrors();
    }

    @ManagedMetric(category="memory", displayName="Total size of HSA mapping index", metricType=MetricType.COUNTER, unit="size")
    public int getTotalIndexSize() {
        return codeServerMekCacheService.getCurrentIndex().size();
    }

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

    @ManagedOperation(description="Clears sampled performance metrics (timed statistics)")
    public void clearPerformanceMetrics() {
        timerMap.clear();
    }

    @ManagedAttribute(description="Sets the length of request history for timed statistics", defaultValue="1000")
    public void setHistoryLength(@ManagedOperationParameter(name="historyLength", description="Indicates history of requests to keep/calculate averge timed statistcis") int historyLength) {
        if (historyLength > 0) {
            this.historyLength = historyLength;
        }
    }

    @ManagedAttribute(description="Returns the length of request history for timed statistics")
    public int getHistoryLength() {
        return historyLength;
    }

    @ManagedOperation(description="Returns active service contract names, i.e. WSDL operations that have been accessed since last startup")
    public String[] getServiceNames() {
        final Set<String> set = timerMap.keySet();
        return set.toArray(new String[set.size()]);
    }

    //
    public String getGUID() {
        return samples.get().peek().getGUID();
    }

    //
    public String getName() {
        return samples.get().peek().getName();
    }

    //
    public void start(final String path) {
        if (samples.get().size() == 0) {
            concurrency.inc();
        }
        samples.get().push(new Sample(path));
    }

    //
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
        private long timestamp;
        private String name;
        private String guid;

        //
        public Sample(final String name) {
            this.timestamp = System.currentTimeMillis();
            this.name = name;
            this.guid = UUID.randomUUID().toString();
        }

        //
        public String getGUID() {
            return guid;
        }

        //
        public String getName() {
            return name;
        }

        //
        public long elapsed() {
            final long time = (System.currentTimeMillis() - timestamp);
            return (time < 0) ? 0 : time;
        }
    }

    //
    static class Concurrency {
        private long activeRequests;
        private long totalRequests;
        private long totalErrors;

        public synchronized void inc() {
            activeRequests++;
            totalRequests++;
        }

        public synchronized void dec(boolean success) {
            activeRequests--;
            if (!success) {
                totalErrors++;
            }
        }

        public synchronized long getActiveRequests() {
            return activeRequests;
        }

        public synchronized long getTotalRequests() {
            return totalRequests;
        }
        
        public synchronized long getTotalErrors() {
            return totalErrors;
        }

    }
}
