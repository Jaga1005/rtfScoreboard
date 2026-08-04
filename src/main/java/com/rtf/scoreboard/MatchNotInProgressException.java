package com.rtf.scoreboard;

public class MatchNotInProgressException extends RuntimeException {
    public MatchNotInProgressException(String message) { super(message); }
}
