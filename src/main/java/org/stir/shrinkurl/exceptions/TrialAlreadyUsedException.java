package org.stir.shrinkurl.exceptions;

public class TrialAlreadyUsedException extends RuntimeException{
    public TrialAlreadyUsedException(String message){
        super(message);
    }
}
