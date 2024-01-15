package org.fetch.exceptions;

public class HttpMethodNotImplementedException extends RuntimeException {
    public HttpMethodNotImplementedException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public HttpMethodNotImplementedException() {
        super();
    }
}
