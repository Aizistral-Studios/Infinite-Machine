package com.aizistral.infmachine.indexation;

import com.aizistral.infmachine.config.InfiniteConfig;
import com.aizistral.infmachine.utils.StandardLogger;

import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class RealtimeMessageIndexer extends ListenerAdapter {
    private static final StandardLogger LOGGER = new StandardLogger("Realtime Message Indexer");

    public RealtimeMessageIndexer(){
        InfiniteConfig.INSTANCE.getJDA().addEventListener(this);
        LOGGER.log("Registered event listener.");
        LOGGER.log("RealtimeIndexer ready.");
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        LOGGER.log("Message received");
        if(isValidMessageEvent(event)) {
            CoreMessageIndexer.INSTANCE.indexMessage(event.getMessage());
        }
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        if(isValidMessageEvent(event)) {
            CoreMessageIndexer.INSTANCE.unindexMessage(event.getMessageIdLong());
        }
    }

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        if(isValidMessageEvent(event)) {
            CoreMessageIndexer.INSTANCE.indexMessage(event.getMessage());
        }
    }

    public boolean isValidMessageEvent(GenericMessageEvent messageEvent)
    {
        if (!messageEvent.isFromGuild()) return false;
        if (messageEvent.getGuild() != InfiniteConfig.INSTANCE.getDomain()) return false;
        return true;
    }

    @Override
    public void onChannelDelete(@NotNull ChannelDeleteEvent event){
        LOGGER.log("Channel Deleted");
        if(event.isFromGuild() && event.getGuild() == InfiniteConfig.INSTANCE.getDomain()) {
            CoreMessageIndexer.INSTANCE.unindexChannel(event.getChannel().getIdLong());
        }
    }
}
