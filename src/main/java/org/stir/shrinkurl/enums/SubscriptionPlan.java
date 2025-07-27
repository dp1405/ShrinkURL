package org.stir.shrinkurl.enums;

import java.util.List;

public enum SubscriptionPlan {
    FREE("Free", 0, 0, "USD", 
         20, 100, 100L * 1024 * 1024, 30, // 100 MB
         List.of("Basic URL shortening", "30 days URL retention"),
         List.of("Custom short codes", "Custom domains", "Priority support")
    ),
    MONTHLY("Month", 9.99, 30, "USD", 
            50, 10000, 5L * 1024 * 1024 * 1024, 365, // 5 GB
            List.of("50 URLs", "10,000 API calls/day", 
                   "365 days URL retention", "Custom short codes"),
            List.of()
    ),
    YEARLY("Year", 99.99, 365, "USD", 
           100, 100000, 50L * 1024 * 1024 * 1024, -1, // 50 GB
           List.of("100 URLs", "100,000 API calls/day", 
                  "Unlimited URL retention", "Custom short codes"),
           List.of()
    ),
    LIFETIME("Lifetime", 299.99, -1, "USD", 
             Integer.MAX_VALUE, Integer.MAX_VALUE, 1000L * 1024 * 1024 * 1024, -1, // 1 TB
             List.of("Unlimited URLs", "Unlimited API calls", "All premium features", 
                    "Unlimited URL retention", "Custom short codes"),
             List.of()
    );

    private final String displayName;
    private final double price;
    private final int durationDays;
    private final String currency;
    private final int urlLimit;
    private final int apiCallLimit;
    private final long storageLimit;
    private final int urlRetentionDays;
    private final List<String> features;
    private final List<String> restrictions;

    SubscriptionPlan(String displayName, double price, int durationDays, String currency,
                    int urlLimit, int apiCallLimit, long storageLimit, int urlRetentionDays,
                    List<String> features, List<String> restrictions) {
        this.displayName = displayName;
        this.price = price;
        this.durationDays = durationDays;
        this.currency = currency;
        this.urlLimit = urlLimit;
        this.apiCallLimit = apiCallLimit;
        this.storageLimit = storageLimit;
        this.urlRetentionDays = urlRetentionDays;
        this.features = features;
        this.restrictions = restrictions;
    }

    public String getDisplayName() { return displayName; }
    public double getPrice() { return price; }
    public int getDurationDays() { return durationDays; }
    public String getCurrency() { return currency; }
    public int getUrlLimit() { return urlLimit; }
    public int getApiCallLimit() { return apiCallLimit; }
    public long getStorageLimit() { return storageLimit; }
    public int getUrlRetentionDays() { return urlRetentionDays; }
    public List<String> getFeatures() { return features; }
    public List<String> getRestrictions() { return restrictions; }
    
    public String getFormattedPrice() {
        if (price == 0) {
            return "Free";
        }
        return String.format("$%.2f", price);
    }
    
    public String getPricePeriod() {
        switch (this) {
            case FREE: return "";
            case MONTHLY: return "/month";
            case YEARLY: return "/year";
            case LIFETIME: return " one-time";
            default: return "";
        }
    }
    
    public String getFormattedStorage() {
        if (storageLimit >= 1000L * 1024 * 1024 * 1024) { // 1 TB or more
            return String.format("%.0f TB", storageLimit / (1024.0 * 1024.0 * 1024.0 * 1024.0));
        } else if (storageLimit >= 1024 * 1024 * 1024) { // 1 GB or more
            return String.format("%.0f GB", storageLimit / (1024.0 * 1024.0 * 1024.0));
        } else { // MB
            return String.format("%.0f MB", storageLimit / (1024.0 * 1024.0));
        }
    }
    
    public String getFormattedUrlLimit() {
        if (urlLimit == Integer.MAX_VALUE) {
            return "Unlimited";
        }
        return String.format("%,d", urlLimit);
    }
    
    public String getFormattedApiLimit() {
        if (apiCallLimit == Integer.MAX_VALUE) {
            return "Unlimited";
        }
        return String.format("%,d", apiCallLimit);
    }
    
    public String getRetentionDescription() {
        if (urlRetentionDays == -1) {
            return "No expiration";
        }
        return urlRetentionDays + " days";
    }
    
    public boolean isPremium() {
        return this != FREE;
    }
    
    public boolean isPopular() {
        return this == YEARLY;
    }
    
    public int getSavingsPercentage() {
        if (this == YEARLY) {
            double monthlyYearly = MONTHLY.price * 12;
            return (int) Math.round(((monthlyYearly - price) / monthlyYearly) * 100);
        }
        return 0;
    }
}