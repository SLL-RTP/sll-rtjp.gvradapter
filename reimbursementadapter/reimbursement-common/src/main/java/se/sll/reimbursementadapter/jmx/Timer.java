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
package se.sll.reimbursementadapter.jmx;

import java.io.Serializable;

/**
 * Keeps track of elapsed time.
 *
 * @author Peter
 */
public class Timer implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * Number of requests.
     * @serial
     */
    private long n;
    /**
     * Number of errors.
     * @serial
     */
    private long e;
    /**
     * Min time in millis.
     * @serial
     */
    private long min;
    /**
     * Max time in millis.
     * @serial
     */
    private long max;
    /**
     * Total time in millis.
     * @serial
     */
    private long sum;
    /**
     * Name of this timer.
     * @serial
     */
    private String name;


    public Timer(String name) {
        this.name = name;
        reset();
    }

    //
    public String name() {
        return name;
    }


    //
    public void add(final long t, final boolean success) {
        sum += t;
        min = Math.min(min, t);
        max = Math.max(max, t);
        n++;
        if (!success) {
            e++;
        }
    }

    //
    protected void reset() {
        e   = 0L;
        n   = 0L;
        sum = 0L;
        min = Long.MAX_VALUE;
        max = Long.MIN_VALUE;
    }
    
    public long min() {
        return (min == Long.MAX_VALUE) ? 0 : min;
    }

    //
    public long max() {
        return (max == Long.MIN_VALUE) ? 0 : max;
    }

    //
    public long avg() {
        return (n == 0) ? 0 : (sum / n);
    }

    //
    public long n() {
        return n;
    }
    
    //
    public long e() {
        return e;
    }

    @Override
    public String toString() {
        return String.format("{ name: \"%s\", n: %d, min: %d, max: %d, avg: %d }", name(), n(), min(), max(), avg());
    }
}