package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Rachio API Exception
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioApiException extends Exception {
    private static final long serialVersionUID = 1L;

    private final RachioApiResult result;

    public RachioApiResult getResult() {
        return result;
    }

    public RachioApiException(RachioApiResult result) {
        super(result.getMessage());
        this.result = result;
    }

    public RachioApiException(String message) {
        super(message);
        this.result = new RachioApiResult(false, message);
    }

    public RachioApiException(String message, Throwable cause) {
        super(message, cause);
        this.result = new RachioApiResult(false, message);
    }

    public RachioApiException(Throwable cause) {
        super(cause);
        this.result = new RachioApiResult(false, cause.getMessage());
    }
}
