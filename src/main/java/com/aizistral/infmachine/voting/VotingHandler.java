package com.aizistral.infmachine.voting;

import com.aizistral.infmachine.InfiniteMachine;
import com.aizistral.infmachine.config.InfiniteConfig;
import com.aizistral.infmachine.config.Localization;
import com.aizistral.infmachine.data.ExitCode;
import com.aizistral.infmachine.database.DataBaseHandler;
import com.aizistral.infmachine.database.FieldType;
import com.aizistral.infmachine.database.Table;
import com.aizistral.infmachine.indexation.CoreMessageIndexer;
import com.aizistral.infmachine.utils.StandardLogger;

import lombok.Getter;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.messages.MessagePoll;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessagePollData;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


public class VotingHandler extends ListenerAdapter {
    private static final StandardLogger LOGGER = new StandardLogger("VotingHandler");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static final VotingHandler INSTANCE = new VotingHandler();

    @Getter
    private TextChannel councilChannel = null;
    private final DataBaseHandler databaseHandler;
    @Getter
    private final String votingTableName = "voting";
    private final String believerTableName = "believers";
    private final VotingChecker votingChecker;


    private VotingHandler() {
        Channel channel = InfiniteMachine.INSTANCE.getJDA().getGuildChannelById(InfiniteConfig.INSTANCE.getCouncilChannelID());
        if(channel instanceof TextChannel) {
            this.councilChannel = (TextChannel) channel;
        }
        this.databaseHandler = DataBaseHandler.INSTANCE;
        createVotingTable();
        createBelieverTable();
        InfiniteMachine.INSTANCE.getJDA().addEventListener(this);

        votingChecker = new VotingChecker(
            () -> {
                //InfiniteMachine.INSTANCE.getMachineChannel().sendMessage(String.format("All registered votings have been checked and updated.")).queue();
                LOGGER.log("All registered votings have been checked and updated.");
            },
            () -> {
                InfiniteMachine.INSTANCE.getMachineChannel().sendMessage(String.format("Registered votings could not be checked.")).queue();
            }
        );
    }

    public void init() {
        startVotingCheckerRunner();
        updateBelieverDatabaseWithCurrentBelievers();
    }

    private void startVotingCheckerRunner() {
        Thread votingCheckerThread = new Thread(votingChecker, "VotingChecker-Thread");
        votingCheckerThread.start();
    }

    private void updateBelieverDatabaseWithCurrentBelievers() {
        Role believerRole = InfiniteMachine.INSTANCE.getDomain().getRoleById(InfiniteConfig.INSTANCE.getBelieversRoleID());
        InfiniteMachine.INSTANCE.getDomain().findMembersWithRoles(believerRole).onSuccess(list -> {
            list.forEach(member -> {
                if(!isBeliever(member.getIdLong())) {
                    promoteBelieverInDatabase(member.getIdLong());
                    LOGGER.log(String.format("Updated database believer status for %s.", member.getEffectiveName()));
                }
            });
        });
    }

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        if(event.getMessage().getPoll() == null) return;
        if(!event.getMessage().getPoll().isExpired()) return;
        long id = event.getMessage().getIdLong();
        Map<String, Object> poll = fetchRegisteredVoteByID(id);
        if(poll == null) return;

        evaluateVote(event.getMessage(), poll);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if(!event.getComponentId().equals("override-accept") && !event.getComponentId().equals("override-decline")) return;
        LOGGER.log("Button was pressed.");
        event.deferReply().setEphemeral(true).queue();
        List<Role> roles = Objects.requireNonNull(event.getMember()).getRoles();
        Role architectRole = Objects.requireNonNull(event.getGuild()).getRoleById(InfiniteConfig.INSTANCE.getArchitectRoleID());
        Role overseerRole = Objects.requireNonNull(event.getGuild()).getRoleById(1145688360543862785L);
        Map<String, Object> registeredVote = fetchRegisteredVoteByID(event.getMessage().getIdLong());
        if (roles.contains(architectRole) || roles.contains(overseerRole)) {
            if(registeredVote == null) {
                event.getHook().editOriginal("This Voting is complete.").queue();
                return;
            }
            event.getHook().editOriginal("Access granted. Executing override...").queue();
        } else {
            event.getHook().editOriginal("Insufficient Permission").queue();
            return;
        }
        executeVote(event.getMessage(), registeredVote, event.getComponentId().equals("override-accept"), true);
    }

    // --------------- //
    // Voting Creation //
    // --------------- //
    public void createManualVoting(SlashCommandInteractionEvent event) {
        OptionMapping mapping = event.getOption("user");
        User votingTarget = mapping != null ? mapping.getAsUser() : null;
        if(votingTarget == null) {
            event.reply("Specified user could not be found.").queue();
            return;
        }
        if(hasVoting(votingTarget.getIdLong())) {
            event.reply("Specified user already has a pending voting.").queue();
            return;
        }
        mapping = event.getOption("type");
        String type = mapping != null ? mapping.getAsString() : VotingType.BELIEVER_PROMOTION.toString();

        boolean success = createVoting(type, votingTarget, true);
        if(success) event.reply("Voting has been created.").queue();
        else event.reply("Voting creation failed. Please check that the User is a Member of the Domain and the voting type is correct.").queue();
    }

    public void createVoteIfNeeded(@NotNull User user) {
        long userID = user.getIdLong();
        if(isBeliever(userID)) return;
        if(!needVote(userID)) return;
        createVoting(VotingType.BELIEVER_PROMOTION.toString(), user, false);
    }

    private boolean needVote(long userID) {
        if(hasVoting(userID)) return false;
        if(userID == 866354348489572352L) return true;
        long voteCount = getVoteAmount(userID);
        long messageCount = CoreMessageIndexer.INSTANCE.getNumberOfMessagesByUserID(userID);
        long totalRating = CoreMessageIndexer.INSTANCE.getRating(userID);
        boolean hasMessages = messageCount >= InfiniteConfig.INSTANCE.getRequiredMessagesForBeliever() * (voteCount + 1);
        boolean hasRating = totalRating >= InfiniteConfig.INSTANCE.getRequiredRatingForBeliever() * (voteCount + 1);

        boolean needsVote = false;
        switch (InfiniteConfig.INSTANCE.getBelieverMethod())
        {
            case MESSAGES:
                if(hasMessages) needsVote = true;
                break;
            case RATING:
                if(hasRating) needsVote = true;
                break;
            case MESSAGES_AND_RATING:
                if(hasMessages && hasRating) needsVote = true;
                break;
            case NONE:
                break;
        }
        return needsVote;
    }

    public boolean createVoting(String type, User votingTarget, boolean isForced) {
        if(!isMemberInDomain(votingTarget)) return false;
        String votingInformation = "";
        String positiveAnswerDescription;
        String negativeAnswerDescription;
        if (type.equals(VotingType.BELIEVER_PROMOTION.toString()) && !isForced) {
            votingInformation = String.format(Localization.translate("msg.votingStandard"), votingTarget.getEffectiveName(), CoreMessageIndexer.INSTANCE.getNumberOfMessagesByUserID(votingTarget.getIdLong()));
            positiveAnswerDescription = "Yes, they will make a fine Believer.";
            negativeAnswerDescription = "No, I don't think they are a worthy Believer.";
        } else if(type.equals(VotingType.BELIEVER_PROMOTION.toString()) && isForced) {
            votingInformation = String.format(Localization.translate("msg.promotionVotingForced"), votingTarget.getEffectiveName());
            positiveAnswerDescription = "Yes, they will make a fine Believer.";
            negativeAnswerDescription = "No, I don't think they are a worthy Believer.";
        } else if (type.equals(VotingType.BELIEVER_DEMOTION.toString()) && isForced) {
            votingInformation = String.format(Localization.translate("msg.demotionVotingForced"), votingTarget.getEffectiveName());
            positiveAnswerDescription = "Yes, they disgraced the believers they are unworthy.";
            negativeAnswerDescription = "No, they deserve another chance.";
        } else {
            return false;
        }

        MessagePollData poll = MessagePollData.builder("Do you agree with this Voting?")
                .addAnswer(positiveAnswerDescription, Emoji.fromFormatted("<:upvote:946944717982142464>"))
                .addAnswer(negativeAnswerDescription, Emoji.fromFormatted("<:downvote:946944748491522098>"))
                .setDuration(2, TimeUnit.DAYS)
                .build();

        if(councilChannel != null)
        {
            Message votingMessage = councilChannel.sendMessage(votingInformation)
            .setPoll(poll)
            .setActionRow(
                    Button.primary("override-accept", "Overrule Yes"),
                    Button.primary("override-decline", "Overrule No")
            )
            .complete();
            LOGGER.log("Registering voting in database.");
            addVotingToDatabase(votingMessage.getIdLong(), votingTarget.getIdLong(), type);
            addVotingDiscussionThread(votingMessage, votingTarget);
            return true;
        }
        return false;
    }

    private boolean isMemberInDomain(User user) {
        return InfiniteMachine.INSTANCE.getDomain().isMember(user);
    }

    private void addVotingDiscussionThread(Message message, User votingTarget) {
        String date = LocalDateTime.now().format(FORMATTER);
        String name = votingTarget.getEffectiveName();
        String threadName = Localization.translate("title.votingThread",name, date);
        message.createThreadChannel(threadName).queue(c -> {
                    //c.sendMessage("May your vote be cast in good spirit and with honest intention.").queue();
                });

    }

    // ----------------- //
    // Voting Evaluation //
    // ----------------- //
    public void evaluateVote(Message message, Map<String, Object> vote) {
        MessagePoll poll = message.getPoll();
        if(poll == null) {
            LOGGER.error("Tried to evaluate message without poll attached");
            return;
        }
        if(!poll.isExpired()) {
            LOGGER.log("Poll is still running.");
            return;
        }
        long positiveVotes = 0;
        long negativeVotes = 0;
        List<MessagePoll.Answer> anwers =  poll.getAnswers();
        for(MessagePoll.Answer answer : anwers) {
            String answerText = answer.getText();
            if(answerText.contains("Yes")){
                positiveVotes = answer.getVotes();
            } else {
                negativeVotes = answer.getVotes();
            }
        }
        executeVote(message, vote, positiveVotes > negativeVotes, false);
    }

    private void executeVote(Message message, Map<String, Object> vote, boolean wasSuccessful, boolean wasOverruled) {
        VotingType votingType = null;
        try{
             votingType = VotingType.valueOf((String) vote.get("type"));
        } catch (IllegalArgumentException e) {
            LOGGER.error(e.toString());
            System.exit(ExitCode.PROGRAM_LOGIC_ERROR.getCode());
        }

        Guild domain = InfiniteMachine.INSTANCE.getDomain();
        VotingType finalVotingType = votingType;
        domain.retrieveMemberById((long) vote.get("voteTargetID")).queue(member -> {
            finalExecution(message, wasSuccessful, member, finalVotingType, wasOverruled);
        });
    }

    private void finalExecution(Message message, boolean wasSuccessful, Member member, VotingType finalVotingType, boolean wasOverruled) {
        if(finalVotingType == VotingType.BELIEVER_PROMOTION) {
            if(wasSuccessful) {
                updateBeliever(member, true);
                promoteBelieverInDatabase(member.getIdLong());
            }
            concludeVote(message, member, finalVotingType, wasSuccessful, wasOverruled);
        } else if (finalVotingType == VotingType.BELIEVER_DEMOTION) {
            if(wasSuccessful) {
                updateBeliever(member, false);
                demoteBelieverInDatabase(member.getIdLong());
            }
            concludeVote(message, member, finalVotingType, wasSuccessful, wasOverruled);
        }
        addBelieverVoteCount(member.getIdLong(), 1);
    }

    private static void updateBeliever(Member member, boolean isPromotion) {
        if(member ==  null) {
            LOGGER.log("Member was not found in guild. Can't add role.");
            return;
        }
        Guild domain = InfiniteMachine.INSTANCE.getDomain();
        Role believerRole = domain.getRoleById(InfiniteConfig.INSTANCE.getBelieversRoleID());
        Role mereDwellerRole = domain.getRoleById((InfiniteConfig.INSTANCE.getDwellersRoleID()));
        changeRoleOnMember(believerRole, member, isPromotion);
        changeRoleOnMember(mereDwellerRole, member, !isPromotion);
    }

    private static void changeRoleOnMember(Role role, Member member, boolean add) {
        if(role == null) {
            LOGGER.error("Role could not be located.");
            System.exit(ExitCode.PROGRAM_LOGIC_ERROR.getCode());
            return;
        }
        Guild domain = InfiniteMachine.INSTANCE.getDomain();
        if(add) {
            domain.addRoleToMember(member, role).queue(
                    success -> LOGGER.log("Role added successfully."),
                    error -> LOGGER.error("Failed to add role.")
            );
        } else {
            domain.removeRoleFromMember(member, role).queue(
                    success -> LOGGER.log("Role removed successfully."),
                    error -> LOGGER.error("Failed to remove role.")
            );
        }
    }
    private void concludeVote(Message message, Member votingTarget, VotingType type, boolean wasSuccessful, boolean wasOverruled) {
        ThreadChannel discussionThread = message.getStartedThread();
        if(discussionThread == null) {
            LOGGER.error("Unable to locate voting discussion thread.");
            return;
        }
        String conclusionMessage = "";
        switch (type) {
            case BELIEVER_PROMOTION: {
                if(wasOverruled){
                    if(wasSuccessful) conclusionMessage = Localization.translate("msg.promotionVotingOverruledPositive", votingTarget);
                    else conclusionMessage = Localization.translate("msg.promotionVotingOverruledNegative", votingTarget);
                    break;
                }
                if(wasSuccessful) conclusionMessage = Localization.translate("msg.promotionVotingElapsedPositive", votingTarget);
                else conclusionMessage = Localization.translate("msg.promotionVotingElapsedNegative", votingTarget);
                break;
            }
            case BELIEVER_DEMOTION: {
                if(wasOverruled){
                    if(wasSuccessful) conclusionMessage = Localization.translate("msg.demotionVotingOverruledPositive", votingTarget);
                    else conclusionMessage = Localization.translate("msg.demotionVotingOverruledNegative", votingTarget);
                    break;
                }
                if(wasSuccessful) conclusionMessage = Localization.translate("msg.demotionVotingElapsedPositive", votingTarget);
                else conclusionMessage = Localization.translate("msg.demotionVotingElapsedNegative", votingTarget);
                break;
            }
        }
        discussionThread.sendMessage(conclusionMessage).queue();
        removeVotingFromDatabase(message.getIdLong());
    }



    // --------------- //
    // Database Access //
    // --------------- //
    private void createVotingTable() {
        Table.Builder tableBuilder = new Table.Builder(votingTableName);
        tableBuilder.addField("messageID", FieldType.LONG, true, true);
        tableBuilder.addField("voteTargetID", FieldType.LONG, false, true);
        tableBuilder.addField("type", FieldType.STRING, false, true);
        Table table = tableBuilder.build();
        databaseHandler.createNewTable(table);
    }

    private void addVotingToDatabase(long messageID, long voteTargetID, String type) {
        String sql = String.format("REPLACE INTO %s (messageID, voteTargetID, type) VALUES(%d,%d,\"%s\")", votingTableName,messageID, voteTargetID, type);
        databaseHandler.executeSQL(sql);
    }

    void removeVotingFromDatabase(long keyID) {
        String sql = String.format("DELETE FROM %s WHERE messageID = %d", votingTableName, keyID);
        databaseHandler.executeSQL(sql);
    }

    List<Map<String, Object>> fetchRegisteredVotes() {
        String sql = String.format("SELECT * FROM %s", VotingHandler.INSTANCE.getVotingTableName());
        return databaseHandler.executeQuerySQL(sql);
    }

    Map<String, Object> fetchRegisteredVoteByID(long voteMessageID) {
        String sql = String.format("SELECT * FROM %s WHERE messageID = %d", VotingHandler.INSTANCE.getVotingTableName(), voteMessageID);
        List<Map<String, Object>> results = databaseHandler.executeQuerySQL(sql);
        return results.isEmpty() ? null : results.get(0);
    }

    public boolean hasVoting(Long userID) {
        String sql = String.format("SELECT * FROM %s WHERE voteTargetID = %d", VotingHandler.INSTANCE.getVotingTableName(), userID);
        List<Map<String, Object>> results = databaseHandler.executeQuerySQL(sql);
        return !results.isEmpty();
    }

    private void createBelieverTable() {
        Table.Builder tableBuilder = new Table.Builder(believerTableName);
        tableBuilder.addField("memberID", FieldType.LONG, true, true);
        tableBuilder.addField("voteNumber", FieldType.LONG, false, true);
        tableBuilder.addField("isBeliever", FieldType.BOOLEAN, false, true);
        Table table = tableBuilder.build();
        databaseHandler.createNewTable(table);
    }

    private void promoteBelieverInDatabase(long userID) {
        String sql = String.format("REPLACE INTO %s (memberID, voteNumber, isBeliever) VALUES(%d,%d,\"%s\")", believerTableName, userID, getVoteAmount(userID), true);
        databaseHandler.executeSQL(sql);
    }

    private void demoteBelieverInDatabase(long userID) {
        String sql = String.format("REPLACE INTO %s (memberID, voteNumber, isBeliever) VALUES(%d,%d,\"%s\")", believerTableName, userID, getVoteAmount(userID), false);
        databaseHandler.executeSQL(sql);
    }

    public void addBelieverVoteCount(long userID, long numberOfVotesToAdd) {
        String sql = String.format("REPLACE INTO %s (memberID, voteNumber, isBeliever) VALUES(%d,%d,\"%s\")", believerTableName, userID, getVoteAmount(userID) + numberOfVotesToAdd, isBeliever(userID));
        databaseHandler.executeSQL(sql);
    }

    public long getVoteAmount(long userID) {
        String sql = String.format("SELECT * FROM %s WHERE memberID = %d", believerTableName, userID);
        List<Map<String, Object>> results = databaseHandler.executeQuerySQL(sql);
        if(results.isEmpty()) return 0;
        return ((Integer) results.get(0).get("voteNumber")).longValue();
    }

    public boolean isBeliever(long userID) {
        String sql = String.format("SELECT * FROM %s WHERE memberID = %d", believerTableName, userID);
        List<Map<String, Object>> results = databaseHandler.executeQuerySQL(sql);
        if(results.isEmpty()) return false;
        return "true".equals((String) results.get(0).get("isBeliever"));
    }


}
