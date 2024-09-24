package com.aizistral.infmachine.database.model;

import org.bson.BsonType;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonRepresentation;
import org.bson.types.ObjectId;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ActivePunishment {
    private ObjectId id = null;
    private ObjectId actionId = null;
    private long guildId = -1, subjectId = -1, timestamp = -1, duration = -1;
    private ModerationAction.Type type = null;

    public ActivePunishment(ObjectId actionId, long guildId, long subjectId, long timestamp, long duration,
            ModerationAction.Type type) {
        this.actionId = actionId;
        this.guildId = guildId;
        this.subjectId = subjectId;
        this.timestamp = timestamp;
        this.duration = duration;
        this.type = type;
    }

}
