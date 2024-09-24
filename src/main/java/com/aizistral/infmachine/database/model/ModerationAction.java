package com.aizistral.infmachine.database.model;

import org.bson.types.ObjectId;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor
public class ModerationAction {
    private ObjectId id = null;
    private Type type = null;
    private String reason = "";
    private long guild = -1, authority = -1, subject = -1, duration = -1, timestamp = -1;
    private boolean success = false;

    public ModerationAction(Type type, long guild, long authority, long subject, String reason, long duration,
            long timestamp, boolean success) {
        this.type = type;
        this.guild = guild;
        this.authority = authority;
        this.subject = subject;
        this.reason = reason;
        this.duration = duration;
        this.timestamp = timestamp;
        this.success = success;
    }

    public static enum Type {
        WARNING, MUTE, BAN, REMOVE_WARNING, REMOVE_MUTE, REMOVE_BAN;
    }
}
