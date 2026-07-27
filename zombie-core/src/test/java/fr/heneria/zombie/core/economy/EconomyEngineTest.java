package fr.heneria.zombie.core.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.heneria.zombie.core.powerup.PowerUpDropService;
import fr.heneria.zombie.core.powerup.PowerUpRegistry;
import fr.heneria.zombie.core.powerup.PowerUpService;
import fr.heneria.zombie.core.powerup.PowerUpType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EconomyEngineTest {
  private static final UUID GAME = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private EconomyService economies;
  private TransactionService transactions;

  @BeforeEach
  void setUp() {
    economies = new EconomyService();
    economies.create(GAME, new EconomyPolicy(1_000, OverflowPolicy.CLAMP, false, 10));
    transactions =
        new TransactionService(
            economies, Clock.systemUTC(), EconomyEventDispatcher.noop(), ignored -> {});
  }

  @Test
  void walletStartingCreditDebitStatsAndIdempotenceAreCentralized() {
    assertTrue(transactions.openWallet(GAME, PLAYER, 500, "start").successful());
    assertTrue(
        transactions
            .credit(TransactionRequest.of(GAME, PLAYER, 100, TransactionReason.ZOMBIE_KILL, "kill"))
            .successful());
    assertTrue(
        transactions
            .debit(
                TransactionRequest.of(
                    GAME, PLAYER, 250, TransactionReason.WALL_WEAPON_PURCHASE, "wall"))
            .successful());
    assertEquals(
        TransactionStatus.DUPLICATE_TRANSACTION,
        transactions
            .debit(
                TransactionRequest.of(
                    GAME, PLAYER, 250, TransactionReason.WALL_WEAPON_PURCHASE, "wall"))
            .status());

    PlayerWallet.Snapshot wallet = economies.wallet(GAME, PLAYER).orElseThrow();
    assertEquals(350, wallet.balance());
    assertEquals(600, wallet.totalEarned());
    assertEquals(250, wallet.totalSpent());
    assertEquals(3, wallet.transactionCount());
  }

  @Test
  void rejectsInvalidAndInsufficientDebitsAndClampsOverflow() {
    transactions.openWallet(GAME, PLAYER, 500, "start");
    assertEquals(
        TransactionStatus.INVALID_AMOUNT,
        transactions
            .debit(
                TransactionRequest.of(GAME, PLAYER, -1, TransactionReason.ADMIN_REMOVE, "negative"))
            .status());
    assertEquals(
        TransactionStatus.INSUFFICIENT_FUNDS,
        transactions
            .debit(
                TransactionRequest.of(
                    GAME, PLAYER, 501, TransactionReason.ADMIN_REMOVE, "too-much"))
            .status());
    transactions.credit(
        TransactionRequest.of(GAME, PLAYER, Long.MAX_VALUE, TransactionReason.ADMIN_GRANT, "max"));
    assertEquals(1_000, economies.wallet(GAME, PLAYER).orElseThrow().balance());
  }

  @Test
  void fullAndPartialRefundsCannotExceedOriginalDebit() {
    transactions.openWallet(GAME, PLAYER, 500, "start");
    Transaction debit =
        transactions
            .debit(
                TransactionRequest.of(
                    GAME, PLAYER, 300, TransactionReason.MYSTERY_BOX_PURCHASE, "box"))
            .transaction()
            .orElseThrow();
    assertTrue(
        transactions
            .refund(new RefundRequest(GAME, debit.transactionId(), 100, "refund-1", Map.of()))
            .successful());
    assertTrue(
        transactions
            .refund(new RefundRequest(GAME, debit.transactionId(), 200, "refund-2", Map.of()))
            .successful());
    assertEquals(
        TransactionStatus.INVALID_AMOUNT,
        transactions
            .refund(new RefundRequest(GAME, debit.transactionId(), 1, "refund-3", Map.of()))
            .status());
    assertEquals(500, economies.wallet(GAME, PLAYER).orElseThrow().balance());
    assertEquals(300, economies.wallet(GAME, PLAYER).orElseThrow().totalRefunded());
  }

  @Test
  void purchaseIsAtomicIdempotentAndRefundsFailedGrant() {
    transactions.openWallet(GAME, PLAYER, 500, "start");
    PurchaseService purchases =
        new PurchaseService(
            transactions,
            new PriceResolver(PriceResolver.Rounding.NEAREST, 0, 10_000),
            EconomyEventDispatcher.noop(),
            Clock.systemUTC());
    PurchaseRequest failed = request("failed", 200, () -> false);
    assertEquals(PurchaseResult.Status.GRANT_FAILED_REFUNDED, purchases.purchase(failed).status());
    assertEquals(500, economies.wallet(GAME, PLAYER).orElseThrow().balance());

    PurchaseRequest successful = request("success", 250, () -> true);
    assertTrue(purchases.purchase(successful).successful());
    assertEquals(
        PurchaseResult.Status.DUPLICATE_OPERATION, purchases.purchase(successful).status());
    assertEquals(250, economies.wallet(GAME, PLAYER).orElseThrow().balance());
  }

  @Test
  void resolvesFixedMultipliersRoundingAndBoundsInDocumentedOrder() {
    PriceResolver resolver = new PriceResolver(PriceResolver.Rounding.CEIL, 10, 1_000);
    var result =
        resolver.resolve(
            new PriceResolver.PriceContext(
                100, List.of(25L), List.of(new BigDecimal("1.5"), new BigDecimal("0.5"))));
    assertTrue(result.valid());
    assertEquals(94, result.price());
  }

  @Test
  void doublePointsStacksExpiresAndChangesOnlyRewards() {
    MutableClock clock = new MutableClock();
    PowerUpService bonuses = new PowerUpService(PowerUpRegistry.defaults(), clock, ignored -> {});
    RewardService rewards =
        new RewardService(
            transactions,
            bonuses::pointMultiplier,
            clock,
            new RewardService.RewardPolicy(10, true, 0.15, 20, 100, Duration.ofSeconds(60)));
    transactions.openWallet(GAME, PLAYER, 0, "start");
    bonuses.activate(GAME, PowerUpType.DOUBLE_POINTS, PLAYER, "test");
    rewards.reward(GAME, PLAYER, 60, TransactionReason.ZOMBIE_KILL, "reward", Map.of());
    assertEquals(120, economies.wallet(GAME, PLAYER).orElseThrow().balance());
    bonuses.activate(GAME, PowerUpType.DOUBLE_POINTS, PLAYER, "test-again");
    clock.advance(Duration.ofSeconds(61));
    bonuses.tick();
    assertFalse(bonuses.active(GAME, PowerUpType.DOUBLE_POINTS));
  }

  @Test
  void dropsRespectCapCollectionIdempotenceExpiryAndGameIsolation() {
    MutableClock clock = new MutableClock();
    PowerUpDropService drops =
        new PowerUpDropService(
            PowerUpRegistry.defaults(),
            new PowerUpDropService.Options(true, 1, 1, Duration.ZERO, Duration.ofSeconds(5)),
            clock,
            new ZeroRandom(),
            ignored -> {});
    var created = drops.roll(GAME, 1).orElseThrow();
    assertTrue(drops.roll(GAME, 1).isEmpty());
    assertTrue(drops.collect(created.id(), GAME, PLAYER).collected());
    assertFalse(drops.collect(created.id(), GAME, PLAYER).collected());

    UUID other = UUID.randomUUID();
    var expiring = drops.roll(other, 1).orElseThrow();
    clock.advance(Duration.ofSeconds(6));
    drops.tick();
    assertFalse(drops.collect(expiring.id(), other, PLAYER).collected());
    drops.clear(GAME);
    assertTrue(drops.active(GAME).isEmpty());
  }

  @Test
  void economiesAreIsolatedAndCleanedPerGame() {
    UUID other = UUID.randomUUID();
    economies.create(other, EconomyPolicy.defaults());
    transactions.openWallet(GAME, PLAYER, 100, "first");
    transactions.openWallet(other, PLAYER, 900, "second");
    economies.remove(GAME);
    assertTrue(economies.find(GAME).isEmpty());
    assertEquals(900, economies.wallet(other, PLAYER).orElseThrow().balance());
  }

  private static PurchaseRequest request(
      String operationId, long price, PurchaseRequest.Grant grant) {
    return new PurchaseRequest(
        GAME,
        PLAYER,
        PurchaseType.WALL_WEAPON,
        "starter",
        PriceResolver.PriceContext.base(price),
        operationId,
        TransactionReason.WALL_WEAPON_PURCHASE,
        PurchaseFundingMode.INDIVIDUAL,
        () -> true,
        grant,
        Map.of());
  }

  private static final class MutableClock extends Clock {
    private Instant now = Instant.parse("2026-01-01T00:00:00Z");

    private void advance(Duration duration) {
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

  private static final class ZeroRandom implements RandomGenerator {
    @Override
    public long nextLong() {
      return 0;
    }
  }
}
