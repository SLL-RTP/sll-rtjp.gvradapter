package se.sll.reimbursementadapter.gvr.transform;

/**
 * Created by erja on 2014-06-13.
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
