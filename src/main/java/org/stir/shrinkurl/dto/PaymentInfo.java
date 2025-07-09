package org.stir.shrinkurl.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInfo {
    @NotBlank
    private String paymentMethod; // "card", "paypal", etc.
    
    private String paymentToken; // From frontend payment processor
    
    @NotBlank
    @Builder.Default
    private String currency = "INR";
    
    // For card payments
    private String cardNumber;
    private String cardholderName;
    private String expiryMonth;
    private String expiryYear;
    private String cvv;
    
    // For PayPal
    private String paypalEmail;
    private String paypalAuthCode;
    
    // Billing address
    private String billingAddress;
    private String city;
    private String state;
    private String zipCode;
    private String country;
}
