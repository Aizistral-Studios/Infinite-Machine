package com.aizistral.infmachine.commands;

import com.aizistral.infmachine.InfiniteMachine;
import com.aizistral.infmachine.Main;
import com.aizistral.infmachine.config.InfiniteConfig;
import com.aizistral.infmachine.config.Localization;
import com.aizistral.infmachine.database.DataBaseHandler;
import com.aizistral.infmachine.indexation.CoreMessageIndexer;
import com.aizistral.infmachine.leaderboard.LeaderBoard;
import com.aizistral.infmachine.utils.StandardLogger;
import com.aizistral.infmachine.utils.Utils;

import com.aizistral.infmachine.voting.VotingHandler;
import com.aizistral.infmachine.voting.VotingType;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.dv8tion.jda.api.Permission.MESSAGE_MENTION_EVERYONE;

public class CommandHandler extends ListenerAdapter {
    private static final StandardLogger LOGGER = new StandardLogger("CommandHandler");
    public static final CommandHandler INSTANCE = new CommandHandler();

    private CommandHandler() {
        InfiniteConfig.INSTANCE.getJDA().addEventListener(this);
    }

    public void init() {
        registerCommands();
    }

    private void registerCommands() {
        LOGGER.log("Registering Commands...");
        // TODO Better localization
        InfiniteConfig.INSTANCE.getJDA().updateCommands().addCommands(
                //Test Commands
                Commands.slash("ping", Localization.translate("cmd.ping.desc")),
                Commands.slash("version", Localization.translate("cmd.version.desc")),
                Commands.slash("uptime", Localization.translate("cmd.uptime.desc")),
                //Rating Commands
                Commands.slash("leaderboard", Localization.translate("cmd.leaderboard.desc"))
                        .addOption(OptionType.INTEGER, "start", Localization.translate("cmd.leaderboard.start"), false),
                Commands.slash("rating", Localization.translate("cmd.rating.desc"))
                        .addOption(OptionType.USER, "user", Localization.translate("cmd.rating.user"), false),
                //Fun Commands
                Commands.slash("pet", Localization.translate("cmd.pet.desc"))
                        .addOption(OptionType.MENTIONABLE, "target", Localization.translate("cmd.pet.user"), false),
                //Admin Commands
                Commands.slash("terminate", Localization.translate("cmd.terminate.desc"))
                        .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
                Commands.slash("fullindex", Localization.translate("cmd.fullindex.desc"))
                        .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
                Commands.slash("getmostactive", Localization.translate("cmd.getmostactive.desc"))
                        .addOption(OptionType.INTEGER, "start", Localization.translate("cmd.leaderboard.start"), false)
                        .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
                Commands.slash("openvoting", Localization.translate("cmd.openvoting.desc"))
                        .addOption(OptionType.USER, "user", Localization.translate("cmd.openvoting.user"), true)
                        .addOption(OptionType.STRING, "type", Localization.translate("cmd.openvoting.type", Arrays.stream(VotingType.values()).map(VotingType::toString).collect(Collectors.joining("/"))), false)
                        .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
                Commands.slash("sendmessage", Localization.translate("cmd.sendmessage.desc"))
                        .addOption(OptionType.CHANNEL, "channel", Localization.translate("cmd.sendmessage.channel"), true)
                        .addOption(OptionType.STRING, "message", Localization.translate("cmd.sendmessage.message"), true)
                        .setDefaultPermissions(DefaultMemberPermissions.DISABLED)
        ).queue();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        LOGGER.log("SlashCommandInteraction received");

        if (event.getChannel() != InfiniteConfig.INSTANCE.getMachineChannel() && event.getChannel() != InfiniteConfig.INSTANCE.getTempleChannel() && !"pet".equals(event.getName())) {
            event.reply("It is the wrong place and time...").queue();
            return;
        }

        switch (event.getName()) {
            case "ping": {
                ping(event);
                break;
            }
            case "version": {
                event.reply(String.format("The machine's version is: **%s**", DataBaseHandler.INSTANCE.retrieveInfiniteVersion())).queue();
                break;
            }
            case "uptime": {
                uptime(event);
                break;
            }
            case "leaderboard": {
                String s  = LeaderBoard.INSTANCE.getLeaderboardString(event, false);
                event.deferReply().flatMap(v -> event.getHook().sendMessage(s).setAllowedMentions(Collections.EMPTY_LIST)).queue();
                break;
            }
            case "rating": {
                String s  = LeaderBoard.INSTANCE.getRatingString(event);
                event.deferReply().flatMap(v -> event.getHook().sendMessage(s).setAllowedMentions(Collections.EMPTY_LIST)).queue();
                break;
            }
            case "pet": {
                pet(event);
                break;
            }
            case "terminate": {
                event.reply("Executing: Halt and catch fire protocol").queue();
                InfiniteMachine.INSTANCE.shutdown();
                break;
            }
            case "getmostactive": {
                String s  = LeaderBoard.INSTANCE.getLeaderboardString(event, true);
                event.deferReply().flatMap(v -> event.getHook().sendMessage(s).setAllowedMentions(Collections.EMPTY_LIST)).queue();
                break;
            }
            case "fullindex": {
                CoreMessageIndexer.INSTANCE.fullIndex();
                event.reply("Indexation reset.").queue();
                break;
            }
            case "openvoting": {
                VotingHandler.INSTANCE.createManualVoting(event);
                break;
            }
            case "sendmessage": {
                sendMessage(event);
                break;
            }
            default: {
                event.reply("Hmm strange this command appears to be not implemented yet. Perhaps ask the Infinite Technician about it?").queue();
            }
        }
    }

    private static void uptime (SlashCommandInteractionEvent event){
        long uptime = System.currentTimeMillis() - InfiniteMachine.INSTANCE.getStartupTime();

        long hrs = TimeUnit.MILLISECONDS.toHours(uptime);
        uptime -= hrs * 60L * 60L * 1000L;
        long mins = TimeUnit.MILLISECONDS.toMinutes(uptime);
        uptime -= mins * 60L * 1000L;
        long secs = TimeUnit.MILLISECONDS.toSeconds(uptime);

        event.reply(Localization.translate("msg.uptime", hrs, mins, secs)).queue();
    }

    private static void ping (SlashCommandInteractionEvent event){
        long time = System.currentTimeMillis();
        event.reply("Pong!").setEphemeral(false).flatMap(v -> event.getHook().editOriginalFormat(
                "Pong: %d ms", System.currentTimeMillis() - time)).queue();
    }

    private void pet(SlashCommandInteractionEvent event) {
        OptionMapping mapping = event.getOption("target");
        IMentionable mentionable = mapping != null ? mapping.getAsMentionable() : null;
        long targetID = 310848622642069504L;
        IMentionable target;
        if(mentionable == null) {
            target = Main.JDA.retrieveUserById(targetID).complete();
        } else if (mentionable instanceof User) {
            target = mapping.getAsUser();
            targetID = target.getIdLong();
        } else if (mentionable instanceof Member) {
            target = mapping.getAsUser();
            targetID = target.getIdLong();
        } else if (mentionable instanceof Role) {
            if(memberHasPermissionToMentionRoleHere(event.getUser(), event.getGuildChannel(), mapping.getAsRole())) {
                target = mapping.getAsRole();
                targetID = target.getIdLong();
            } else {
                event.reply("You lack the required permissions to mention that role.").setEphemeral(true).queue();
                return;
            }
        } else {
            target = Main.JDA.retrieveUserById(targetID).complete();
        }

        Member author = event.getMember();
        assert author != null;
        String msg = "%s has been pet <a:pat_pat_pat:1211592019680694272>";

        //Excluding Arkadiy from normal petting rules
        if (targetID == 440381346339094539L) {
            if (Utils.hasRole(author, InfiniteConfig.INSTANCE.getPetmasterRole().getIdLong())) {
                msg = petMasterPetMessage(target);
                msg += String.format("\nYes %s can pet anyone and anything.", InfiniteConfig.INSTANCE.getPetmasterRole());
            } else {
                msg = "You should know, that a soul can't be `/pet`\nNo matter what machines you wield...";
            }
        } else if (Utils.hasRole(author, InfiniteConfig.INSTANCE.getPetmasterRole().getIdLong())) {
            msg = petMasterPetMessage(target);
        }else if (targetID == 814542724010213427L) {
            if (!Utils.hasRole(author, InfiniteConfig.INSTANCE.getArchitectRole().getIdLong())) {
                msg = String.format("How dare you filthy animal lay hands on the muffin of %s.", InfiniteConfig.INSTANCE.getArchitectRole());
            }
        }  else if (targetID == 1124053065109098708L) { // bot's own ID
            msg = "At the end of times, the %s has pet itself <a:pat_pat_pat:1211592019680694272>";
        }
        event.reply(String.format(msg, target)).queue();
    }

    private boolean memberHasPermissionToMentionRoleHere(User user, GuildMessageChannelUnion guildChannel, Role role) {
        Member member = Utils.userToMember(user);
        if(member == null) return false;
        if(!role.isMentionable() && !member.hasPermission(guildChannel, MESSAGE_MENTION_EVERYONE)) return false;
        return true;
    }

    private String petMasterPetMessage(IMentionable target) {
        Random rand = new Random();
        Role petmaster = InfiniteConfig.INSTANCE.getPetmasterRole();
        ArrayList<String> possibleReactions = new ArrayList<>();
        possibleReactions.add(String.format("In a display of wholesome benevolence, %s has been expertly pet by the skilled hands of %s.", target, petmaster));
        possibleReactions.add(String.format("%s has experienced the gentle touch of a master petter, their contentment evident in every stroke.", target));
        possibleReactions.add(String.format("%s has experienced the gentle touch of a master petter, their spirit uplifted by the soothing strokes.", target));
        possibleReactions.add(String.format("With the utmost precision, %s has been pet to perfection, each gesture a testament of their importance.", target));
        possibleReactions.add(String.format("Under the skilled hands of %s, %s has received the most exquisite pets imaginable.", petmaster, target));
        possibleReactions.add(String.format("With unparalleled finesse, %s has been graced with the touch of a true petting virtuoso.", target));
        possibleReactions.add(String.format("%s has been honored with the tender ministrations of %s, a moment of pure bliss.", target, petmaster));
        possibleReactions.add(String.format("Under the practiced hand of %s, %s has enjoyed masterful petting, finding comfort in each gentle caress.", petmaster, target));
        possibleReactions.add(String.format("With each stroke, %s has been reminded of the power of masterful pets.", target));
        possibleReactions.add(String.format("%s has been graced with masterful petting, each stroke a symphony of comfort and care.", target));
        possibleReactions.add(String.format("In a moment of pure bliss, %s has enjoyed the art of masterful petting, their worries melting away with each touch.", target));
        possibleReactions.add(String.format("With each stroke, %s has been transported to a world of serenity, the masterful petting a source of joy and comfort.", target));
        possibleReactions.add(String.format("Under the watchful eye of %s, %s has relished in the art of masterful petting, finding renewal and refreshment in the simple act.", petmaster, target));
        return possibleReactions.get(Math.abs(rand.nextInt() % possibleReactions.size()));
    }

    private void sendMessage(SlashCommandInteractionEvent event) {
        OptionMapping channelMapping = event.getOption("channel");
        OptionMapping messageMapping = event.getOption("message");

        assert channelMapping != null;
        long channelID = channelMapping.getAsChannel().getIdLong();
        assert messageMapping != null;
        String message = messageMapping.getAsString();

        Channel targetChannel = InfiniteConfig.INSTANCE.getJDA().getGuildChannelById(channelID);
        if (!(targetChannel instanceof TextChannel)) {
            event.reply("The specified channel is not a valid text channel.").queue();
        } else {
            ((TextChannel) targetChannel).sendMessage(message).queue(v -> {
                event.reply("Message as been send.").queue();
            });
        }
    }
}
