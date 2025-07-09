package org.stir.shrinkurl.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResult {
    private boolean successful;
    private String transactionId;
    private String stripeSubscriptionId;
    private String errorMessage;
    private String errorCode;
    private BigDecimal amountCharged;
    private String currency;
    private LocalDateTime processedAt;
}
