package com.shivam.intelliflow.order.service;

import com.shivam.intelliflow.order.config.TraceContextFilter;
import com.shivam.intelliflow.order.dto.PaymentProcessRequest;
import com.shivam.intelliflow.order.dto.PaymentProcessResponse;
import com.shivam.intelliflow.order.entity.Order;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PaymentServiceClient {
    private static final String DEFAULT_PAYMENT_METHOD = "LOCAL_DEV_CARD";

    private final RestClient restClient;

    public PaymentServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${intelliflow.payment-service.base-url:http://localhost:8083}") String paymentServiceBaseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(paymentServiceBaseUrl)
                .build();
    }

    public PaymentProcessResponse processPayment(Order order, String requestedPaymentMethod) {
        PaymentProcessRequest request = new PaymentProcessRequest(
                order.getId(),
                order.getTotalAmount(),
                order.getCurrency(),
                paymentMethod(requestedPaymentMethod)
        );

        return restClient.post()
                .uri("/api/v1/payments/process")
                .header(TraceContextFilter.TRACE_ID_HEADER, traceId())
                .header(TraceContextFilter.SPAN_ID_HEADER, spanId())
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (httpRequest, response) -> {
                    throw new ResponseStatusException(
                            response.getStatusCode(),
                            "Payment service rejected payment request"
                    );
                })
                .body(PaymentProcessResponse.class);
    }

    private String paymentMethod(String requestedPaymentMethod) {
        if (requestedPaymentMethod == null || requestedPaymentMethod.isBlank()) {
            return DEFAULT_PAYMENT_METHOD;
        }
        return requestedPaymentMethod;
    }

    private String traceId() {
        String traceId = MDC.get(TraceContextFilter.TRACE_ID);
        return traceId == null || traceId.isBlank() ? "missing-trace-id" : traceId;
    }

    private String spanId() {
        String spanId = MDC.get(TraceContextFilter.SPAN_ID);
        return spanId == null || spanId.isBlank() ? "missing-span-id" : spanId;
    }
}
