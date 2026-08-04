package com.rtf.scoreboard;

public class TeamAlreadyPlayingException extends RuntimeException {
    public TeamAlreadyPlayingException(String message) { super(message); }
}
