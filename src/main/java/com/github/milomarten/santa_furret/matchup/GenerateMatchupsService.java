package com.github.milomarten.santa_furret.matchup;

import com.github.milomarten.santa_furret.models.SecretSantaMatchup;
import com.github.milomarten.santa_furret.models.SecretSantaParticipant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GenerateMatchupsService {
    private static final int MINIMUM_PARTICIPANTS = 5;
    private static final RandomGenerator RNG = RandomGenerator.getDefault();

    /**
     * Use a list of participants to create Santa-Giftee matchups, respecting blacklists.
     * Each participant (a santa) will be given a random other participant (a giftee), that satisfies:
     * - It is not themselves
     * - The Santa does not have the Giftee in their blacklist
     * - The Giftee does not have the Santa in their blacklist
     * If someone is unable to be assigned a partner, they will be marked as a problem Santa, and omitted from
     * the matchup. This may be because:
     * - They have blacklisted all other participants; OR all other participants have blacklisted them; OR some combination of this
     * - The interaction of multiple participant blacklists results in one impossible partner. As an example 4-person interaction:
     *  - P1 blacklists P2 and P3
     *  - P2 blacklists P3
     *  - P3
     *  - P4
     *  P1 can only possibly be matched to P4. P2 cannot be matched to P1 (their blacklist), P3 (P2's blacklist), and P4
     *  is already taken. Thus, it is impossible to make a matchup here.
     * @param participants The list of participants
     * @return The list of matchups, as well as a list of santas that are impossible to match.
     */
    public List<SecretSantaMatchup> createMatchups(List<SecretSantaParticipant> participants) {
        if (participants.size() < MINIMUM_PARTICIPANTS) {
            throw new NotEnoughParticipantsException();
        }

        var assignments = deduceInitialMatchups(participants);
        var santaQueue = new LinkedList<>(participants); // This is only ever popped
        var gifteePool = new ArrayList<>(participants); // This is frequently randomized

        if (!assignments.isEmpty()) {
            assignments.forEach(ssm -> {
                santaQueue.remove(ssm.getSanta());
                gifteePool.remove(ssm.getGiftee());
            });
        }

        int reshuffleCounter = participants.size() * 5; // This is a safety net, in case we do start recursing a lot.
        while (!santaQueue.isEmpty() && reshuffleCounter > 0) {
            var santa = santaQueue.pop();
            log.debug("Matching Santa {}", santa.getParticipantId());
            var gifteeMaybe = pullMatch(santa, gifteePool);
            if (gifteeMaybe.isPresent()) {
                log.debug("Matched Santa {} to Giftee {}", santa.getParticipantId(), gifteeMaybe.get().getParticipantId());
                assignments.add(makeMatchup(santa, gifteeMaybe.get()));
            } else {
                // We need to find someone that *would* match from the existing matchups.
                log.debug("Unable to find a matching Giftee. Searching in previous matchups");
                reshuffleCounter--;
                var toDivorce = pullMatchFromExisting(santa, assignments)
                        .orElseThrow(() -> {
                            log.debug("Unable to match. Marking as a problem Santa");
                            return new UnableToAssignGiftee(santa, "Unable to find a giftee that isn't blacklisted by this user.");
                        });
                assignments.add(makeMatchup(santa, toDivorce.getGiftee()));
                santaQueue.add(toDivorce.getSanta());
            }
        }

        if (assignments.size() != participants.size()) {
            throw new UnableToAssignGiftee(null, "Had difficulties assigning matchups. Please contact Milo!");
        }
        return assignments;
    }

    // Very side effecty method, ugh
    private static Optional<SecretSantaParticipant> pullMatch(SecretSantaParticipant santa, List<SecretSantaParticipant> options) {
        Collections.shuffle(options, RNG);
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

    // Also very side effecty method, ugh
    private static Optional<SecretSantaMatchup> pullMatchFromExisting(SecretSantaParticipant santa, List<SecretSantaMatchup> options) {
        Collections.shuffle(options, RNG);
        var iterable = options.iterator();
        while (iterable.hasNext()) {
            var option = iterable.next();
            if (isValidMatch(santa, option.getGiftee())) {
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

    /**
     * Loop repeatedly through the participant list, checking for participants with only one option
     * If, during this process, any are found with zero options, throw an exception (pending: return as problem santas)
     * @param participants All participants
     * @return An initial list of matchups, people with no other options except one.
     */
    private static List<SecretSantaMatchup> deduceInitialMatchups(List<SecretSantaParticipant> participants) {
        Map<Long, Set<Long>> permissibleMatchups = new HashMap<>();
        List<SecretSantaMatchup> matchups = new ArrayList<>(participants.size());
        Map<Long, SecretSantaParticipant> participantsById = participants.stream()
                .collect(Collectors.toMap(SecretSantaParticipant::getParticipantId, p -> p));

        for (var participantA : participants) {
            for (var participantB : participants) {
                if (isValidMatch(participantA, participantB)) {
                    permissibleMatchups.computeIfAbsent(participantA.getParticipantId(),
                            l -> new HashSet<>())
                            .add(participantB.getParticipantId());
                }
            }
        }
        while(true) {
            Long idToPurge = null;
            Long idToRemove = null;
            for (var entry : permissibleMatchups.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    throw new UnableToAssignGiftee(participantsById.get(entry.getKey()), "Unable to find a giftee that isn't blacklisted by this user.");
                } else if (entry.getValue().size() == 1) {
                    idToPurge = entry.getKey();
                    idToRemove = entry.getValue().iterator().next();
                    matchups.add(makeMatchup(participantsById.get(idToPurge), participantsById.get(idToRemove)));
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
        return matchups;
    }

    private static SecretSantaMatchup makeMatchup(SecretSantaParticipant santa, SecretSantaParticipant giftee) {
        return new SecretSantaMatchup(
                null,
                santa, giftee, santa.getEvent()
        );
    }
}
