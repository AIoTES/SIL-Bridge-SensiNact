package eu.interiot.intermw.bridge.sensinact.http.model.exception;

public class InvalidConfigurationValueException extends Exception {

    public InvalidConfigurationValueException() {
        super();
    }

    public InvalidConfigurationValueException(String message) {
        super(message);
    }

    public InvalidConfigurationValueException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidConfigurationValueException(Throwable cause) {
        super(cause);
    }

    protected InvalidConfigurationValueException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
