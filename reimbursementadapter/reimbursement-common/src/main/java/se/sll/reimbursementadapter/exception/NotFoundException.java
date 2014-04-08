package se.sll.reimbursementadapter.exception;

/**
 * Simple exception used by AbstractProducer#fullfill().
 */
public class NotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception.
     *
     * @param message the user message in plain text.
     */
    public NotFoundException(String message) {
        super(message);
    }

}
