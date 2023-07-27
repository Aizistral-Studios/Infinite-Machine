package com.aizistral.infmachine.data;

import lombok.Value;

@Value
public class MessageInfo {
    private ChannelType channelType;
    private long channelID, messageID;
}
