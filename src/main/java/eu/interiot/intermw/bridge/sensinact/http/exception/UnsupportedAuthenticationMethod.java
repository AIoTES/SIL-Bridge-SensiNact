package eu.interiot.intermw.bridge.sensinact.http.exception;

public class UnsupportedAuthenticationMethod extends Exception{

    public UnsupportedAuthenticationMethod() {
        super();
    }

    public UnsupportedAuthenticationMethod(String message) {
        super(message);
    }

    public UnsupportedAuthenticationMethod(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedAuthenticationMethod(Throwable cause) {
        super(cause);
    }

    protected UnsupportedAuthenticationMethod(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
