package com.rtf.scoreboard.exception;

public class TeamAlreadyPlayingException extends RuntimeException {
    public TeamAlreadyPlayingException(String teamName) {
        super(String.format("%s is already playing", teamName));
    }
}
