package com.aizistral.infmachine.indexation;

import net.dv8tion.jda.api.entities.Message;

import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessedMessage {
    private final String message;
    private int linkCount;
    private int emojiCount;
    private final int uniqueLength;

    public ProcessedMessage(Message messageInput) {
        String message = messageInput.getContentRaw();

        if (!this.detectLinks(message).isEmpty()) {
            message = removeLinks(message);
        }

        if (!this.detectEmojis(message).isEmpty()) {
            message = removeEmojis(message);
        }

        this.message = message;

        this.uniqueLength = calculateUniqueLength(message);
    }

    public String getMessage() {
        return this.message;
    }

    public int getLinkCount() {
        return this.linkCount;
    }

    public int getEmojiCount() {
        return this.emojiCount;
    }

    public int getUniqueLength() {
        return this.uniqueLength;
    }

    private String detectLinks(String message) {
        Pattern linkPattern = Pattern.compile("https?://\\S+");
        Matcher matcher = linkPattern.matcher(message);

        StringBuilder links = new StringBuilder();

        while (matcher.find()) {
            links.append(matcher.group()).append("\n");
            this.linkCount++;
        }

        return links.toString();
    }

    private String detectEmojis(String message) {
        Pattern emojiPattern = Pattern.compile("<:[^:\\s]+:\\d{18}\\d*>");
        Matcher matcher = emojiPattern.matcher(message);

        StringBuilder emojis = new StringBuilder();

        while (matcher.find()) {
            emojis.append(matcher.group()).append("\n");
            this.emojiCount++;
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

    private int calculateUniqueLength(String message) {
        String[] stringArray = message.split("\\s+");

        HashSet<String> uniqueWords = new HashSet<>(Arrays.asList(stringArray));
        int uniqueLength = 0;

        for (String uniqueWord : uniqueWords) {
            uniqueLength += uniqueWord.length();
        }
        return uniqueLength;
    }
}
