package com.inklusport.users.exception;

public class UserHasFutureEventsException extends RuntimeException {
    public UserHasFutureEventsException(String message) {
        super(message);
    }
}
