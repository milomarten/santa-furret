package com.github.milomarten.santa_furret.models;

public record ParticipantOptions(boolean okReceivingNsfw, boolean okGivingNsfw) {
    public static final ParticipantOptions YES = new ParticipantOptions(true, true);
}
