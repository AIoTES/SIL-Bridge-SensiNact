package eu.interiot.intermw.bridge.sensinact.wrapper.exception;

/**
 * Created by nj246216 on 14/02/17.
 */
public class UnsupportedAuthenticationMethod extends Exception {

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
}
