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
package se.sll.gvradapter.jmx;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Keeps track of elapsed time for a given history of measurements.
 *
 * @author Peter
 */
public class HistoryTimer extends Timer implements Serializable {
    private static final long serialVersionUID = 1L;
    private int len;
    private int ofs = 0;
    private long[] history;
    private boolean[] success;

    //
    public HistoryTimer(String name, int len) {
        super(name);
        this.len = len;
        this.history = new long[len];
        Arrays.fill(history, -1);
        this.success = new boolean[len];
    }

    //
    public void add(final long t, final boolean success) {
        if (this.ofs >= len) {
            this.ofs = 0;
        }
        this.success[ofs] = success;
        this.history[ofs] = t;
        this.ofs++;
    }

    //
    public synchronized void recalc() {
        reset();
        for (int i = 0; i < len && history[i] >= 0; i++) {
            super.add(history[i], success[i]);
        }
    }

    @Override
    public synchronized String toString() {
        return String.format("{ name: \"%s\", history: %d, min: %d, max: %d, avg: %d, error: %d }", name(), n(), min(), max(), avg(), e());
    }
}