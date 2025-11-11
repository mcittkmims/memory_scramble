package com.pr.memory_scramble.model;

import com.pr.memory_scramble.exception.CardRemovedException;
import com.pr.memory_scramble.exception.RestrictedCardAccessException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BoardTest {

    @Autowired
    private Board board;

    @Nested
    class InitializationTests {

        @Test
        void testBoardInitialization_LoadsCorrectly() {
            assertTrue(board.getRows() > 0, "Board rows should be greater than 0");
            assertTrue(board.getColumns() > 0, "Board columns should be greater than 0");

            int expectedCardCount = board.getRows() * board.getColumns();
            for (int i = 0; i < expectedCardCount; i++) {
                Card card = board.getCard(i);
                assertNotNull(card, "Card at index " + i + " should not be null");
                assertNotNull(card.getValue(), "Card value at index " + i + " should not be null");
                assertFalse(card.getValue().isBlank(), "Card value at index " + i + " should not be blank");
            }
            assertEquals(expectedCardCount,
                    board.toString("p1").lines().count() - 1,
                    "Number of cards in board should match expected count");
        }
    }

    @Nested
    class RuleTests {
        @BeforeEach
        void resetBoardState() {
            board.reset();
        }

        @Test
        void testFirstCard_FaceDown_TurnsUpAndControlled() throws InterruptedException {
            board.flip("p1", 0);
            assertEquals(CardState.CONTROLLED, board.getCard(0).getState(), "Card should be controlled by p1");
        }

        @Test
        void testFirstCard_FaceUpNotControlled_TakesControl() throws InterruptedException {
            board.flip("p1", 0);
            board.flip("p1", 1);
            board.flip("p2", 0);
            assertEquals(CardState.CONTROLLED, board.getCard(0).getState(), "Card should now be controlled by p2");
        }

        @Test
        void testFirstCard_FaceUpControlledByOther_Waits() throws InterruptedException {
            board.flip("p1", 0);
            Thread t = new Thread(() -> {
                try {
                    board.flip("p2", 0);
                    fail("Should wait while another player controls the card");
                } catch (InterruptedException ignored) {
                }
            });
            t.start();
            Thread.sleep(200); // allow thread to block
            assertTrue(t.isAlive(), "Thread should be waiting (rule 1-D)");
            t.interrupt();
        }

        @Test
        void testFirstCard_NoCardFails() throws InterruptedException {
            board.flip("p1", 1);
            board.flip("p1", 4);
            assertThrows(CardRemovedException.class, () -> board.flip("p1", 1),
                    "Should throw CardRemovedException for removed card");
        }

        @Test
        void testSecondCard_NoCard_RelinquishFirst() throws InterruptedException {
            board.flip("p1", 1);
            board.flip("p1", 4);

            board.flip("p1", 0);
            assertThrows(RestrictedCardAccessException.class, () -> board.flip("p1", 1),
                    "Should throw exception when accessing restricted card");
            assertNotEquals(CardState.CONTROLLED, board.getCard(0).getState(),
                    "First card control should be relinquished");
        }

        @Test
        void testSecondCard_ControlledCardFails_RelinquishFirst() throws InterruptedException {
            board.flip("p2", 1);

            board.flip("p1", 0);
            assertThrows(RestrictedCardAccessException.class, () -> board.flip("p1", 1),
                    "Should throw exception when accessing controlled card");
            assertNotEquals(CardState.CONTROLLED, board.getCard(0).getState(),
                    "First card control should be relinquished");
        }

        @Test
        void testSecondCard_FaceDown_MatchKeepsControl() throws InterruptedException {
            board.flip("p1", 1);
            board.flip("p1", 4);
            assertEquals(CardState.CONTROLLED, board.getCard(1).getState(), "First card should be controlled");
            assertEquals(CardState.CONTROLLED, board.getCard(4).getState(), "Second card should be controlled");
        }

        @Test
        void testSecondCard_FaceDown_MismatchRelinquish() throws InterruptedException {
            board.flip("p1", 1);
            board.flip("p1", 3);
            assertEquals(CardState.UP, board.getCard(1).getState(), "First card should be face up");
            assertEquals(CardState.UP, board.getCard(3).getState(), "Second card should be face up");
        }
    }

    @Nested
    class WatchTests {

        @BeforeEach
        void resetBoardState() {
            board.reset();
        }

        @Test
        void testWatch_TriggeredByMap() throws InterruptedException {
            AtomicBoolean notified = new AtomicBoolean(false);

            Thread watcher = new Thread(() -> {
                try {
                    board.watch();
                    notified.set(true);
                } catch (InterruptedException ignored) { }
            });
            watcher.start();

            Thread.sleep(100);
            board.map(s -> s);
            watcher.join(500);

            assertTrue(notified.get(), "Watcher should be notified after map");
        }

        @Test
        void testWatch_TriggeredByFlip() throws InterruptedException {
            AtomicBoolean notified = new AtomicBoolean(false);

            Thread watcher = new Thread(() -> {
                try {
                    board.watch();
                    notified.set(true);
                } catch (InterruptedException ignored) { }
            });
            watcher.start();

            Thread.sleep(100);
            board.flip("p1", 0);
            watcher.join(500);

            assertTrue(notified.get(), "Watcher should be notified after flipping a card");
        }
    }

    @Nested
    class MapTests {

        @BeforeEach
        void resetBoardState() {
            board.reset();
        }

        @Test
        void testMap_ChangesAllCardValues() {
            Function<String, String> mapper = s -> s + "_mapped";
            board.map(mapper);

            int expectedCardCount = board.getRows() * board.getColumns();
            for (int i = 0; i < expectedCardCount; i++) {
                Card card = board.getCard(i);
                assertTrue(card.getValue().endsWith("_mapped"), "Card value should be mapped");
            }
        }

        @Test
        void testMapDuringCardMatch_DoesNotBreakMatching() throws InterruptedException {
            Card card1 = board.getCard(1);
            Card card2 = board.getCard(4);


            AtomicBoolean mappingDone = new AtomicBoolean(false);

            Thread mapper = new Thread(() -> {
                board.map(s -> s + "_mapped");
                mappingDone.set(true);
            });

            Thread flipper = new Thread(() -> {
                try {
                    board.flip("p1", 1);
                    board.flip("p1", 4);
                } catch (InterruptedException e) {
                    fail("Flipping should not be interrupted");
                }
            });
            flipper.start();
            mapper.start();

            mapper.join(1000);
            flipper.join(1000);

            board.flip("p1", 2);

            assertTrue(card1.getState() == CardState.NONE, "Card1 should be removed after matching");
            assertTrue(card2.getState() == CardState.NONE, "Card2 should be removed after matching");
            assertTrue(mappingDone.get(), "Mapping should have completed concurrently");
        }

    }
}
