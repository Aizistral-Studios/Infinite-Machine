package com.aizistral.infmachine.indexation;

import com.aizistral.infmachine.database.local.IndexedMessageDatabase;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;

public class RealtimeMessageIndexer extends ListenerAdapter {

    private final Guild domain;

    public RealtimeMessageIndexer(Guild domain){
        this.domain = domain;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if(isValidMessageEvent(event))
        {

            CoreMessageIndexer.INSTANCE.indexMessage(event.getMessage());
        }
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event){
        long deletedMessageId = event.getMessageIdLong();
        IndexedMessageDatabase db = CoreMessageIndexer.INSTANCE.database;
        List<Long> cachedEntry = db.getCachedMessageByID(deletedMessageId);

        if(!cachedEntry.isEmpty())
        {
            //ToDo Link to ScoreboardDatabase to update relevant data
            //this.database.addMessageCount(cachedEntry.get(0), -1);
            //this.database.addMessageRating(cachedEntry.get(0), -Math.toIntExact(cachedEntry.get(1)));
            db.removeCachedMessageByID(deletedMessageId);
        }
    }

    public boolean isValidMessageEvent(MessageReceivedEvent messageEvent)
    {
        if (!messageEvent.isFromGuild()) return false;
        if (messageEvent.getGuild() != this.domain) return false;
        return true;
    }
}
