package com.rtf.scoreboard.exception;

public class SameTeamMatchException extends RuntimeException {
    public SameTeamMatchException() {
        super("A team cannot play against itself");
    }
}
