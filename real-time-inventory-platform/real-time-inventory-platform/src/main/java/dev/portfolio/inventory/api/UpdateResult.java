package dev.portfolio.inventory.api;

public record UpdateResult(Status status, InventoryView inventory) {
    public enum Status { APPLIED, STALE_IGNORED, DUPLICATE_IGNORED }
}
