package com.uniqueleaf.library.exception;

public class LoanLimitExceededException extends LibraryException {
    public LoanLimitExceededException(String message) {
        super(message);
    }
}
