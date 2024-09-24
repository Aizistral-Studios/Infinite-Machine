package com.aizistral.infmachine;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.management.RuntimeErrorException;

import com.aizistral.infmachine.commands.CommandRegistry;
import com.aizistral.infmachine.config.GuildConfig;
import com.aizistral.infmachine.config.JSONLoader;
import com.aizistral.infmachine.utils.SimpleLogger;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class InfiniteMachine {
    private static final SimpleLogger LOGGER = new SimpleLogger("InfiniteMachine");
    private static final List<InfiniteMachine> INSTANCES = new ArrayList<>();

    @Getter
    private final Guild guild;

    @Getter
    private final CommandRegistry commands;

    @Getter(AccessLevel.PACKAGE)
    private final JSONLoader<GuildConfig> configLoader;

    private InfiniteMachine(Guild guild) {
        this.guild = guild;
        this.commands = new CommandRegistry(this);

        Path configFile = Paths.get("./config/guilds/" + guild.getId() + ".json");
        this.configLoader = new JSONLoader<>(configFile, GuildConfig.class, GuildConfig::new);
    }

    public JDA getJDA() {
        return this.guild.getJDA();
    }

    public GuildConfig getConfig() {
        return this.configLoader.getData();
    }

    public Optional<StandardGuildMessageChannel> getModerationLogChannel() {
        StandardGuildMessageChannel channel = this.guild.getChannelById(StandardGuildMessageChannel.class,
                this.getConfig().getModerationLogChannel());

        return Optional.ofNullable(channel);
    }

    public boolean represents(Guild guild) {
        return this.represents(guild.getIdLong());
    }

    public static boolean instanceExistsFor(Guild guild) {
        return instanceExistsFor(guild.getIdLong());
    }

    public static Optional<InfiniteMachine> getInstanceFor(Guild guild) {
        return getInstanceFor(guild.getIdLong());
    }

    public boolean represents(long guildId) {
        return this.guild.getIdLong() == guildId;
    }

    public static boolean instanceExistsFor(long guildId) {
        return getInstanceFor(guildId).isPresent();
    }

    public static Optional<InfiniteMachine> getInstanceFor(long guildId) {
        return INSTANCES.stream().filter(machine -> machine.represents(guildId)).findAny();
    }

    public static List<InfiniteMachine> getInstances() {
        return Collections.unmodifiableList(INSTANCES);
    }

    @SneakyThrows
    static void bootInstance(Guild guild) throws IllegalArgumentException {
        if (instanceExistsFor(guild))
            throw new IllegalArgumentException("Guild " + guild.getId() + " already has a dedicated machine instance!");

        InfiniteMachine machine = new InfiniteMachine(guild);

        machine.getConfigLoader().load();
        machine.getCommands().initialize();

        INSTANCES.add(machine);
        LOGGER.info("Machine instance initialized for guild {} ({}).", guild.getId(), guild.getName());
    }

}