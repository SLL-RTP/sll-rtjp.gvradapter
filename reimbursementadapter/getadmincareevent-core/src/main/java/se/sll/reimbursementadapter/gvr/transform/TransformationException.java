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
package se.sll.reimbursementadapter.gvr.transform;

/**
 * Exception thrown from a Transformator when the transformation for some reason
 * cannot finish the transformation properly.
 */
public class TransformationException extends Exception {

    /**
     * <p>Create a new <code>TransformationException</code> with
     * no specified detail mesage and cause.</p>
     */

    public TransformationException() {
        super();
    }

    /**
     * <p>Create a new <code>TransformationException</code> with
     * the specified detail message.</p>
     *
     * @param message The detail message.
     */

    public TransformationException(String message) {
        super(message);
    }

    /**
     * <p>Create a new <code>TransformationException</code> with
     * the specified detail message and cause.</p>
     *
     * @param message The detail message.
     * @param cause The cause.  A <code>null</code> value is permitted, and indicates that the cause is nonexistent or unknown.
     */

    public TransformationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * <p>Create a new <code>TransformationException</code> with
     * the specified cause.</p>
     *
     * @param cause The cause.  A <code>null</code> value is permitted, and indicates that the cause is nonexistent or unknown.
     */

    public TransformationException(Throwable cause) {
        super(cause);
    }

}
