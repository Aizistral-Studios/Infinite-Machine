package com.aizistral.infmachine.data;

import lombok.Value;

@Value
public class Voting {
    long messageID;
    long candidateID;
    long startingTime;
}
