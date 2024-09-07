package com.aizistral.infmachine;

import com.aizistral.infmachine.commands.CommandHandler;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.aizistral.infmachine.config.InfiniteConfig;
import com.aizistral.infmachine.data.ExitCode;
import com.aizistral.infmachine.database.DataBaseHandler;
import com.aizistral.infmachine.indexation.CoreMessageIndexer;
import com.aizistral.infmachine.utils.StandardLogger;

import com.aizistral.infmachine.voting.VotingHandler;
import lombok.Getter;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.messages.MessagePollBuilder;
import net.dv8tion.jda.api.utils.messages.MessagePollData;

@Getter
public class InfiniteMachine extends ListenerAdapter {
    protected static final StandardLogger LOGGER = new StandardLogger("InfiniteMachine");
    public static final InfiniteMachine INSTANCE = new InfiniteMachine();

    static {
        INSTANCE.awake();
    }

    @Getter
    private final long startupTime;
    @Getter
    private final Guild domain;

    @SneakyThrows
    private InfiniteMachine() {
        this.startupTime = System.currentTimeMillis();

        this.domain = InfiniteConfig.INSTANCE.getDomain();
        if (this.domain == null) {
            LOGGER.error("Architects Domain could not be located. Is the machine not there yet?");
            System.exit(ExitCode.MISSING_DOMAIN_ERROR.getCode());
            throw new IllegalStateException();
        }

        InfiniteConfig.INSTANCE.getJDA().addEventListener(this);

        //        this.domain.getNewsChannelById(1267062468883120179L)
        //        .sendMessage("Helo guys, it's-a me, Aizistral! ✨\n\nNow, this isn't a stream announcement this time, but rather a poll. Y'all get to choose what you want me to do on the very next stream!\n\nCast your votes wisely 🦉\n\n<@&771377288927117342> <@&1229431177085976587> <@&1227941342873784420>")
        //        .setPoll(new MessagePollBuilder("What should Aizistral do on the next stream?")
        //                .addAnswer("Continue playing Vintage Story")
        //                .addAnswer("Play FTL: Faster Than Light (never played before)")
        //                .addAnswer("Keep working on Enigmatic Legacy")
        //                .addAnswer("Wear a maid outfit (in addition to one of the above)")
        //                .setMultiAnswer(true)
        //                .setDuration(1L, TimeUnit.DAYS)
        //                .build())
        //        .queue();
    }

    private void awake() {
        CommandHandler.INSTANCE.init();
        CoreMessageIndexer.INSTANCE.index();;
        VotingHandler.INSTANCE.init();

        LOGGER.log("Domain channels: " + this.domain.getChannels().size());


        String version = this.getVersion();
        if (!DataBaseHandler.INSTANCE.retrieveInfiniteVersion().equals(version)){
            DataBaseHandler.INSTANCE.setInfiniteVersion(version);
            InfiniteConfig.INSTANCE.getMachineChannel().sendMessage(String.format("<:the_cube:963161249028378735> Version **%s** of Infinite Machine was deployed successfully.", version)).queue();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        LOGGER.log("Message received");
        if (event.isFromGuild() && event.getGuild() == this.domain) {
            if (event.getChannel() == InfiniteConfig.INSTANCE.getSuggestionsChannel()) {
                if (event.getAuthor().isBot() || event.getAuthor().isSystem()) return;
                event.getMessage().addReaction(InfiniteConfig.INSTANCE.getUpvoteEmoji()).queue(v -> event.getMessage().addReaction(InfiniteConfig.INSTANCE.getDownvoteEmoji()).queue());
            }
        }
    }

    public String getVersion() {
        String version = Main.class.getPackage().getImplementationVersion();
        return version != null ? version : "UNKNOWN";
    }

    public void shutdown() {
        LOGGER.log("Infinite Machine is shutting down...");
        System.exit(0);
    }
}