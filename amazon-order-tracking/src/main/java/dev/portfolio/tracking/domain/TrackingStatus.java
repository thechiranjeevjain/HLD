package dev.portfolio.tracking.domain;

public enum TrackingStatus {
  ORDERED(0), PACKED(10), SHIPPED(20), OUT_FOR_DELIVERY(30), DELIVERED(40), EXCEPTION(25);
  private final int rank;
  TrackingStatus(int rank) { this.rank = rank; }
  public int rank() { return rank; }
}
