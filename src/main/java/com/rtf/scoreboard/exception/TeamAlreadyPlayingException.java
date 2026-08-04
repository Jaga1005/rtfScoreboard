package com.rtf.scoreboard.exception;

public class TeamAlreadyPlayingException extends RuntimeException {
    public TeamAlreadyPlayingException(String teamName) {
        super(teamName + " is already playing");
    }
}
