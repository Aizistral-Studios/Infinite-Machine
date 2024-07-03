package com.aizistral.infmachine.voting;

import com.aizistral.infmachine.database.DataBaseHandler;
import com.aizistral.infmachine.utils.StandardLogger;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public class VotingChecker implements Runnable{
    private static final StandardLogger LOGGER = new StandardLogger("Voting Checker");
    private final Runnable callbackOnSuccess;
    private final Runnable callbackOnFailure;
    private final DataBaseHandler dataBaseHandler;

    public VotingChecker(Runnable callbackOnSuccess, Runnable callbackOnFailure) {
        this.callbackOnSuccess = callbackOnSuccess;
        this.callbackOnFailure = callbackOnFailure;
        this.dataBaseHandler = DataBaseHandler.INSTANCE;
        LOGGER.log("Voting Checker ready, awaiting orders.");
    }
    @Override
    public void run() {
        try{
            checkVotes();
            callbackOnSuccess.run();
            LOGGER.log("Vote check completed. Full success");
        } catch(Exception ex) {
            callbackOnFailure.run();
            LOGGER.error("Vote check experienced Fatal Error:" + ex.getMessage());
        }
    }

    private void checkVotes() {
        LOGGER.log("Checking all registered votes");
        List<Map<String, Object>> registeredVotes = VotingHandler.INSTANCE.fetchRegisteredVotes();
        for(Map<String, Object> vote : registeredVotes) {
            checkVote(vote);
        }
    }

    private void checkVote(Map<String, Object> vote) {
        long voteID = (long) vote.get("messageID");
        VotingHandler.INSTANCE.getCouncilChannel().retrieveMessageById(voteID).queue(message -> {
            VotingHandler.INSTANCE.evaluateVote(message, vote);
        }, throwable -> {
            // If an error occurs, handle it
            if (throwable instanceof ErrorResponseException) {
                ErrorResponseException ex = (ErrorResponseException) throwable;
                if (ex.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE) {
                    VotingHandler.INSTANCE.removeVotingFromDatabase(voteID);
                    LOGGER.log("Poll message has been deleted clearing database entry");
                } else {
                    // Other errors can be handled here
                    LOGGER.log("Failed to retrieve message: " + ex.getMessage());
                }
            }
        });
    }
}
