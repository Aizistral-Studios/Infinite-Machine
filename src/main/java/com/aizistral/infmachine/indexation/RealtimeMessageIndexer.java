package com.aizistral.infmachine.indexation;

import com.aizistral.infmachine.InfiniteMachine;
import com.aizistral.infmachine.utils.StandardLogger;

import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class RealtimeMessageIndexer extends ListenerAdapter {
    private static final StandardLogger LOGGER = new StandardLogger("Realtime Message Indexer");

    public RealtimeMessageIndexer(){
        InfiniteMachine.INSTANCE.getJDA().addEventListener(this);
        LOGGER.log("Registered event listener.");
        LOGGER.log("RealtimeIndexer ready.");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if(isValidMessageEvent(event)) {
            CoreMessageIndexer.INSTANCE.indexMessage(event.getMessage());
        }
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        if(isValidMessageEvent(event)) {
            CoreMessageIndexer.INSTANCE.unindexMessage(event.getMessageIdLong());
        }
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) { //ToDo Check whether this works as intended or change if necessary
        if(isValidMessageEvent(event)) {
            CoreMessageIndexer.INSTANCE.indexMessage(event.getMessage());
        }
    }

    public boolean isValidMessageEvent(GenericMessageEvent messageEvent)
    {
        if (!messageEvent.isFromGuild()) return false;
        if (messageEvent.getGuild() != InfiniteMachine.INSTANCE.getDomain()) return false;
        return true;
    }
}
