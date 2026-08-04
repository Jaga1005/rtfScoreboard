package com.rtf.scoreboard.exception;

public class MatchNotInProgressException extends RuntimeException {
    public MatchNotInProgressException() {
        super("The specified match is not in progress");
    }
}
