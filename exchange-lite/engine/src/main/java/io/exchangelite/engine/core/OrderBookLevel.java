package io.exchangelite.engine.core;

public record OrderBookLevel(long priceTicks, int visibleQuantity, int orderCount) {
}
