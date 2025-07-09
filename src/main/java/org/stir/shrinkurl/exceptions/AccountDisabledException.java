package org.stir.shrinkurl.exceptions;

public class AccountDisabledException extends RuntimeException {
    public AccountDisabledException(String message){
        super(message);
    }
}
