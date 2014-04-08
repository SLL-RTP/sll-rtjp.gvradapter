package se.sll.reimbursementadapter.exception;

public class NumberOfCareEventsExceededException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception.
     *
     * @param message the user message in plain text.
     */
    public NumberOfCareEventsExceededException(String message) {
        super(message);
    }

}
