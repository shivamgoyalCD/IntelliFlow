package com.shivam.intelliflow.order.dto;

import com.shivam.intelliflow.order.entity.OrderStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateOrderRequest(
        @NotBlank
        @Size(max = 64)
        String customerId,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal totalAmount,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter uppercase ISO code")
        String currency,

        @NotNull
        @Positive
        Integer itemCount,

        @NotNull
        OrderStatus status
) {
}
