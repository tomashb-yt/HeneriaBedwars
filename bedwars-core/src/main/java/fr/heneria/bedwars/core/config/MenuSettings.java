package fr.heneria.bedwars.core.config;

/** Immutable global settings for the future menu framework. */
public record MenuSettings(
    int defaultSize,
    boolean fillEmptySlots,
    boolean closeOnCriticalError,
    boolean playClickSounds,
    int previousSlot,
    int nextSlot,
    int backSlot) {}
