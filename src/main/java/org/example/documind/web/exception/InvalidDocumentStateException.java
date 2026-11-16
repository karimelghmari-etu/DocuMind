package org.example.documind.web.exception;

public class InvalidDocumentStateException extends RuntimeException {
    public InvalidDocumentStateException(String message) {
        super(message);
    }
    public InvalidDocumentStateException(String message, Throwable cause){
        super(message, cause);
    }
}
