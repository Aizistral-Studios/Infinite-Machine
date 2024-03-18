package com.aizistral.infmachine.indexation;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class RealtimeMessageIndexer extends ListenerAdapter {

    private final Guild domain;

    public RealtimeMessageIndexer(Guild domain){
        this.domain = domain;
    }

    public void onMessageReceived(MessageReceivedEvent event) {
        if(isValidMessageEvent(event))
        {
            CoreMessageIndexer.INSTANCE.indexMessage(event.getMessage());
        }

    }

    public boolean isValidMessageEvent(MessageReceivedEvent messageEvent)
    {
        if (!messageEvent.isFromGuild()) return false;
        if (messageEvent.getGuild() != this.domain) return false;
        return true;
    }
}
