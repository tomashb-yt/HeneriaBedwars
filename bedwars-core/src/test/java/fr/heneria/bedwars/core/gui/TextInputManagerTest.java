package fr.heneria.bedwars.core.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TextInputManagerTest {
  private final UUID player = UUID.randomUUID();
  private final MutableClock clock = new MutableClock();
  private final List<String> events = new ArrayList<>();
  private TextInputManager manager;

  @BeforeEach
  void setUp() {
    manager = new TextInputManager(clock);
  }

  @Test
  void startsOneSessionAndMarksChatForCapture() {
    assertTrue(manager.begin(player, request()));
    assertTrue(manager.active(player));
    assertFalse(manager.begin(player, request()));
  }

  @Test
  void acceptsValidatedTrimmedInputOnce() {
    manager.begin(player, request());
    assertEquals(TextInputSubmission.ACCEPTED, manager.submit(player, "  alpha  "));
    assertEquals(List.of("success:alpha"), events);
    assertFalse(manager.active(player));
  }

  @Test
  void invalidInputStaysActive() {
    manager.begin(player, request());
    assertEquals(TextInputSubmission.INVALID, manager.submit(player, "bad val"));
    assertEquals(List.of("invalid:spaces"), events);
    assertTrue(manager.active(player));
  }

  @Test
  void blankAndOverlongAnswersAreRejected() {
    manager.begin(player, request());
    assertEquals(TextInputSubmission.INVALID, manager.submit(player, "   "));
    assertEquals(TextInputSubmission.INVALID, manager.submit(player, "123456789"));
    assertTrue(manager.active(player));
  }

  @Test
  void cancelKeywordIsCaseInsensitiveAndNeverAccepted() {
    manager.begin(player, request());
    assertEquals(TextInputSubmission.CANCELLED, manager.submit(player, "ANNULER"));
    assertEquals(List.of("cancel:PLAYER"), events);
  }

  @Test
  void timeoutCancelsAndCleansSession() {
    manager.begin(player, request());
    clock.advance(Duration.ofSeconds(11));
    assertEquals(List.of(player), manager.expire());
    assertEquals(List.of("cancel:TIMEOUT"), events);
    assertFalse(manager.active(player));
  }

  @Test
  void disconnectAndShutdownHaveExplicitReasons() {
    manager.begin(player, request());
    manager.cancel(player, TextInputCancelReason.DISCONNECT);
    UUID other = UUID.randomUUID();
    manager.begin(other, request());
    manager.cancelAll(TextInputCancelReason.PLUGIN_STOP);
    assertEquals(List.of("cancel:DISCONNECT", "cancel:PLUGIN_STOP"), events);
  }

  @Test
  void answerWithoutSessionIsNotCaptured() {
    assertEquals(TextInputSubmission.NOT_ACTIVE, manager.submit(player, "hello"));
    assertTrue(events.isEmpty());
  }

  private TextInputRequest request() {
    return new TextInputRequest(
        "prompt",
        Duration.ofSeconds(10),
        8,
        Set.of("annuler", "cancel"),
        value -> value.contains(" ") ? Optional.of("spaces") : Optional.empty(),
        value -> events.add("success:" + value),
        problem -> events.add("invalid:" + problem),
        reason -> events.add("cancel:" + reason));
  }

  private static final class MutableClock extends Clock {
    private Instant now = Instant.parse("2026-07-16T12:00:00Z");

    void advance(Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
