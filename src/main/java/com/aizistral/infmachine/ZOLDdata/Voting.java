package com.aizistral.infmachine.ZOLDdata;

import com.aizistral.infmachine.voting.VotingType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Voting {
    private long messageID = 0;
    private long candidateID = 0;
    private long startingTime = 0;
    private VotingType type = VotingType.BELIEVER_PROMOTION;
}
