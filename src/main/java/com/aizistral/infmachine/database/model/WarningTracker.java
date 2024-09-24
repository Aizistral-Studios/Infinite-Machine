package com.aizistral.infmachine.database.model;

import org.bson.types.ObjectId;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WarningTracker {
    private ObjectId id = null;
    private long guildId = -1;
    private long subjectId = -1;
    private int warningCount = 0;

    public WarningTracker(long subjectId, int warningCount) {
        this.subjectId = subjectId;
        this.warningCount = warningCount;
    }

}
