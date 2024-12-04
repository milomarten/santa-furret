package com.github.milomarten.santa_furret.matchup;

import com.github.milomarten.santa_furret.models.SecretSantaParticipant;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class GenerateMatchupsJobTest {
    private static final GenerateMatchupsJob job = new GenerateMatchupsJob();

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4})
    public void testNParticipants(int n) {
        var participates = IntStream.range(0, n)
                        .mapToObj(GenerateMatchupsJobTest::make)
                        .toList();
        assertThrows(NotEnoughParticipantsException.class, () -> job.createMatchups(participates));
    }

    @Test
    public void test5WellBehavedParticipants() {
        var result = job.createMatchups(List.of(
                make(1), make(2), make(3), make(4), make(5)
        ));
        assertEquals(5, result.matchups().size());
        assertEquals(0, result.problemSantas().size());
        for (var matchup : result.matchups()) {
            assertNotEquals(matchup.getSanta().getParticipantId(), matchup.getGiftee().getParticipantId());
        }
    }

    @RepeatedTest(10)
    public void test5Participants1Blacklist() {
        var result = job.createMatchups(List.of(
                make(1), make(2), make(3), make(4), make(5, 3)
        ));
        assertEquals(5, result.matchups().size());
        assertEquals(0, result.problemSantas().size());
    }

    @RepeatedTest(10)
    public void test5ParticipantsAllUniqueBlacklist() {
        var result = job.createMatchups(List.of(
                make(1, 2), make(2, 3), make(3, 4), make(4, 5), make(5, 1)
        ));
        assertEquals(5, result.matchups().size());
        assertEquals(0, result.problemSantas().size());
    }

    @Test
    public void test5ParticipantsEveryoneHatesOneGuy() {
        var result = job.createMatchups(List.of(
                make(1),
                make(2, 1),
                make(3, 1),
                make(4, 1),
                make(5, 1)
        ));
        assertEquals(4, result.matchups().size());
        assertEquals(1, result.problemSantas().size());
    }

    @Test
    public void test5ParticipantsCrazyBlacklists() {
        assertThrows(NotEnoughParticipantsException.class, () -> job.createMatchups(List.of(
                // This list is impossible to handle; two IDs can only match to one person, a no-no.
                make(1),
                make(2, 3, 4),
                make(3, 1),
                make(4, 5, 1),
                make(5, 2)
        )));
    }

    @Test
    public void test5ParticipantsOnlyOneOption() {
        var result = job.createMatchups(List.of(
                // This list is impossible to handle; two IDs can only match to one person, a no-no.
                make(1),
                make(2),
                make(3, 1),
                make(4, 1),
                make(5, 1)
        ));

        assertEquals(5, result.matchups().size());
    }

    private static SecretSantaParticipant make(long id) {
        return new SecretSantaParticipant(null, null, id, false, false, Set.of());
    }

    private static SecretSantaParticipant make(long id, long blacklistId) {
        return new SecretSantaParticipant(null, null, id, false, false, Set.of(blacklistId));
    }

    private static SecretSantaParticipant make(long id, long... blacklistIds) {
        Set<Long> set = new HashSet<>();
        Arrays.stream(blacklistIds).forEach(set::add);
        return new SecretSantaParticipant(null, null, id, false, false, set);
    }
}