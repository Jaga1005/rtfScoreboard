package com.rtf.scoreboard.exception;

public class InvalidTeamNameException extends RuntimeException {
    public InvalidTeamNameException() {
        super("Team name must not be null or blank");
    }
}
