package com.github.milomarten.santa_furret.matchup;

import com.github.milomarten.santa_furret.models.SecretSantaMatchup;
import com.github.milomarten.santa_furret.models.SecretSantaParticipant;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class GenerateMatchupsJob {
    public Results createMatchups(List<SecretSantaParticipant> participants) {
        if (participants.size() < 5) {
            throw new NotEnoughParticipantsException();
        }

        validateInitialMatchups(participants);

        var assignments = new LinkedList<SecretSantaMatchup>();
        var toBeAssignedQueue = new LinkedList<>(participants);
        var remainingParticipants = new ArrayList<>(participants);
        var problematicSantas = new LinkedList<SecretSantaParticipant>();

        queueProcessor: while (!toBeAssignedQueue.isEmpty()) {
            var santa = toBeAssignedQueue.pop();
            log.info("Matching Santa {}", santa.getParticipantId());
            var gifteeMaybe = pullMatch(santa, remainingParticipants);
            if (gifteeMaybe.isPresent()) {
                log.info("Matched Santa {} to Giftee {}", santa.getParticipantId(), gifteeMaybe.get().getParticipantId());
                var matchup = new SecretSantaMatchup();
                matchup.setEvent(santa.getEvent());
                matchup.setSanta(santa);
                matchup.setGiftee(gifteeMaybe.get());
                assignments.add(matchup);
            } else {
                // We need to find someone that *would* match from the existing matchups.
                log.info("Unable to find a matching Giftee. Searching in previous matchups");
                Collections.shuffle(assignments);
                var iterable = assignments.iterator();
                while (iterable.hasNext()) {
                    var matchup = iterable.next();
                    if (isValidMatch(santa, matchup.getGiftee())) {
                        log.info("Matched Santa {} to Giftee {}. Putting Santa {} back into the queue",
                                santa.getParticipantId(), matchup.getGiftee().getParticipantId(), matchup.getSanta().getParticipantId());
                        iterable.remove();
                        toBeAssignedQueue.add(matchup.getSanta());
                        var newMatchup = new SecretSantaMatchup();
                        newMatchup.setEvent(santa.getEvent());
                        newMatchup.setSanta(santa);
                        newMatchup.setGiftee(matchup.getGiftee());
                        assignments.add(newMatchup);
                        continue queueProcessor;
                    }
                }
                log.info("Unable to match. Marking as a problem Santa");
                problematicSantas.add(santa);
            }
        }
        if (log.isInfoEnabled()) {
            var f = assignments.stream()
                    .map(ssm -> ssm.getSanta().getParticipantId() + "->" + ssm.getGiftee().getParticipantId())
                    .collect(Collectors.joining(", "));
            log.info("Finished. Final matchups: {}", f);
        }
        return new Results(assignments, problematicSantas);
    }

    // Very side effecty method, ugh
    private Optional<SecretSantaParticipant> pullMatch(SecretSantaParticipant santa, List<SecretSantaParticipant> options) {
        Collections.shuffle(options);
        var iterable = options.iterator();
        while (iterable.hasNext()) {
            var option = iterable.next();
            if (isValidMatch(santa, option)) {
                iterable.remove();
                return Optional.of(option);
            }
        }
        return Optional.empty();
    }

    private static boolean isValidMatch(SecretSantaParticipant santa, SecretSantaParticipant option) {
        return santa.getParticipantId() != option.getParticipantId() &&
                !santa.getBlacklist().contains(option.getParticipantId()) &&
                !option.getBlacklist().contains(santa.getParticipantId());
    }

    // Check for gotchas in the participant's blacklist.
    // Does this by computing a table of all the participants a participant could match. If any participants
    // cannot match any, an exception is thrown. If any participant can only match one, that participant is removed,
    // and the table is recalculated.

    // This is an unoptimized mess, but it does seem to handle the cases I need handled.
    private static void validateInitialMatchups(List<SecretSantaParticipant> participants) {
        Map<Long, InitialMatchupHolder> permissibleMatchups = new HashMap<>();
        for (var participantA : participants) {
            for (var participantB : participants) {
                if (isValidMatch(participantA, participantB)) {
                    permissibleMatchups.computeIfAbsent(participantA.getParticipantId(),
                            l -> new InitialMatchupHolder(participantA, new HashSet<>()))
                            .add(participantB.getParticipantId());
                }
            }
        }
        while(true) {
            Long idToPurge = null;
            Long idToRemove = null;
            for (var entry : permissibleMatchups.entrySet()) {
                if (entry.getValue().size() == 0) {
                    throw new NotEnoughParticipantsException();
                } else if (entry.getValue().size() == 1) {
                    idToPurge = entry.getKey();
                    idToRemove = entry.getValue().permissibleMatchups.iterator().next();
                    break;
                }
            }
            if(idToRemove != null) {
                for (var entry : permissibleMatchups.entrySet()) {
                    entry.getValue().remove(idToRemove);
                }
                permissibleMatchups.remove(idToPurge);
            } else {
                break;
            }
        }
    }

    private record InitialMatchupHolder(SecretSantaParticipant data, Set<Long> permissibleMatchups) {
        public void add(long l) { this.permissibleMatchups.add(l); }
        public int size() { return this.permissibleMatchups.size(); }
        public void remove(long l) { this.permissibleMatchups.remove(l); }
    }

    public record Results(List<SecretSantaMatchup> matchups, List<SecretSantaParticipant> problemSantas) {}
}
