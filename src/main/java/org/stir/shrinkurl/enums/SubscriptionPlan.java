package org.stir.shrinkurl.enums;

public enum SubscriptionPlan {
    FREE("Free", 0, 0),
    MONTHLY("Monthly", 9.99, 30),
    YEARLY("Yearly", 99.99, 365),
    LIFETIME("Lifetime", 299.99, -1);

    private final String displayName;
    private final double price;
    private final int durationDays;

    SubscriptionPlan(String displayName, double price, int durationDays) {
        this.displayName = displayName;
        this.price = price;
        this.durationDays = durationDays;
    }

    public String getDisplayName() { return displayName; }
    public double getPrice() { return price; }
    public int getDurationDays() { return durationDays; }
}