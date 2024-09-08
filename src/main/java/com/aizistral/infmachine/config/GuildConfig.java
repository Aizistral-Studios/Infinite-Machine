package com.aizistral.infmachine.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;

@Getter
public class GuildConfig {
    private boolean trusted = false;

    @Getter
    public static class Wrapper {
        private final Guild guild;
        private final Path file;
        private final JSONLoader<GuildConfig> loader;

        public Wrapper(Guild guild) {
            this.guild = guild;
            this.file = Paths.get("./config/guilds/" + guild.getId() + ".json");
            this.loader = new JSONLoader<>(this.file, GuildConfig.class, GuildConfig::new);
        }
    }

}
