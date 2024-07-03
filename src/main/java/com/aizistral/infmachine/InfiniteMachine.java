package com.aizistral.infmachine;

import com.aizistral.infmachine.commands.CommandHandler;
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