package com.aizistral.infmachine.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;

@Getter
public class GuildConfig {
    private boolean trusted = false;
    private long moderationLogChannel = -1;
    private String checkmarkEmoji = "[·]";
    private String crossmarkEmoji = "[x]";
    private String justiceEmoji = "[!]";
    private String unpunishedEmoji = "[/]";

    public int getEmbedNormalColor() {
        return InfiniteConfig.getInstance().getEmbedColorDefault();
    }

    public int getEmbedErrorColor() {
        return InfiniteConfig.getInstance().getEmbedColorError();
    }

}
