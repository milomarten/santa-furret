package com.github.milomarten.santa_furret.util;

import java.math.BigInteger;
import java.util.BitSet;

public enum Permission {
    CREATE_INSTANT_INVITE,
    KICK_MEMBERS,
    BAN_MEMBERS,
    ADMINISTRATOR,
    MANAGE_CHANNELS,
    MANAGE_GUILD,
    ADD_REACTIONS,
    VIEW_AUDIT_LOG,
    PRIORITY_SPEAKER,
    STREAM,
    VIEW_CHANNEL,
    SEND_MESSAGES,
    SEND_TTS_MESSAGES,
    MANAGE_MESSAGES,
    EMBED_LINKS,
    ATTACH_FILES,
    READ_MESSAGE_HISTORY,
    MENTION_EVERYONE,
    USE_EXTERNAL_EMOJIS,
    VIEW_GUILD_INSIGHTS,
    CONNECT,
    SPEAK,
    MUTE_MEMBERS,
    DEAFEN_MEMBERS,
    MOVE_MEMBERS,
    USE_VAD,
    CHANGE_NICKNAME,
    MANAGE_NICKNAMES,
    MANAGE_ROLES,
    MANAGE_WEBHOOKS,
    MANAGE_GUILD_EXPRESSIONS,
    USE_APPLICATION_COMMANDS,
    REQUEST_TO_SPEAK,
    MANAGE_EVENTS,
    MANAGE_THREADS,
    CREATE_PUBLIC_THREADS,
    CREATE_PRIVATE_THREADS,
    USE_EXTERNAL_STICKERS,
    SEND_MESSAGES_IN_THREADS,
    USE_EMBEDDED_ACTIVITIES,
    MODERATE_MEMBERS,
    VIEW_CREATOR_MONETIZATION_ANALYTICS,
    USE_SOUNDBOARD,
    CREATE_GUILD_EXPRESSIONS,
    CREATE_EVENTS,
    USE_EXTERNAL_SOUNDS,
    SEND_VOICE_MESSAGES,
    NOOP_47,
    NOOP_48,
    SEND_POLLS,
    USE_EXTERNAL_APPS;

    public String single() {
        return BigInteger.ONE.shiftLeft(this.ordinal()).toString();
    }

    public static String group(Permission... permissions) {
        if (permissions.length == 0) {
            return "0";
        } else if (permissions.length == 1) {
            return permissions[0].single();
        }

        var bs = new BitSet(Permission.values().length);
        for (var permission : permissions) {
            bs.set(permission.ordinal());
        }

        return new BigInteger(reverse(bs.toByteArray())).toString();
    }

    private static byte[] reverse(byte[] original) {
        // What is this, 10th grade
        byte[] nu = new byte[original.length];
        for (int i = 0; i < nu.length; i++) {
            nu[i] = original[original.length - i - 1];
        }
        return nu;
    }
}
