package com.aizistral.infmachine.data;

import net.dv8tion.jda.api.entities.Message;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessedMessage {
    private String message;
    private int linkCount;
    private int emojiCount;

    public ProcessedMessage(Message messageInput) {
        String message = messageInput.getContentRaw();

        if (!detectLinks(message).isEmpty()) {
            message = removeLinks(message);
        }
        if (!detectEmojis(message).isEmpty()) {
            message = removeEmojis(message);
        }
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public int getLinkCount() {
        return linkCount;
    }

    public int getEmojiCount() {
        return emojiCount;
    }

    private String detectLinks(String message) {
        Pattern linkPattern = Pattern.compile("https?://\\S+");
        Matcher matcher = linkPattern.matcher(message);

        StringBuilder links = new StringBuilder();
        while (matcher.find()) {
            links.append(matcher.group()).append("\n");
            linkCount++;
        }
        return links.toString();
    }

    private String detectEmojis(String message) {
        Pattern emojiPattern = Pattern.compile("<:.+:\\d{18}>");
        Matcher matcher = emojiPattern.matcher(message);

        StringBuilder emojis = new StringBuilder();

        while (matcher.find()) {
            emojis.append(matcher.group()).append("\n");
            emojiCount++;
        }
        return emojis.toString();
    }

    private static String removeLinks(String message) {
        Pattern linkPattern = Pattern.compile("https?://\\S+");
        Matcher matcher = linkPattern.matcher(message);
        return matcher.replaceAll("");
    }

    private static String removeEmojis(String message) {
        Pattern emojiPattern = Pattern.compile("<:.+:\\d{18}>");
        Matcher matcher = emojiPattern.matcher(message);
        return matcher.replaceAll("");
    }
}
