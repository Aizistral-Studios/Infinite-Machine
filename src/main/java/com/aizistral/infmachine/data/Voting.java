package com.aizistral.infmachine.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Value;

@Data
@AllArgsConstructor
public class Voting {
    private long messageID = 0;
    private long candidateID = 0;
    private long startingTime = 0;
    private Type type = Type.GRANT_ROLE;

    public static enum Type {
        GRANT_ROLE, REVOKE_ROLE;
    }
}
