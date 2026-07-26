package fr.heneria.zombie.core.game;

final class GameFixtures {
  private GameFixtures() {}

  static RoundConfiguration configuration() {
    return new RoundConfiguration(
        1, 5, true, true, 5, 500, 20, 0, 3, 5, 2, 0.5, 1, 100, 20, 1.1, 200, 4, 2, 20, 20, 5, 2,
        true, 20, 3, 8, 10);
  }
}
