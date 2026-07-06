package com.shivam.intelliflow.order.service;

import com.shivam.intelliflow.order.dto.CreateOrderRequest;
import com.shivam.intelliflow.order.dto.OrderResponse;
import com.shivam.intelliflow.order.dto.PaymentProcessResponse;
import com.shivam.intelliflow.order.dto.UpdateOrderRequest;
import com.shivam.intelliflow.order.entity.Order;
import com.shivam.intelliflow.order.entity.OrderStatus;
import com.shivam.intelliflow.order.publisher.EventPublisher;
import com.shivam.intelliflow.order.repository.OrderRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
    private final PaymentServiceClient paymentServiceClient;

    public OrderService(
            OrderRepository orderRepository,
            EventPublisher eventPublisher,
            PaymentServiceClient paymentServiceClient
    ) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.paymentServiceClient = paymentServiceClient;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = new Order(
                request.customerId(),
                request.totalAmount(),
                request.currency(),
                request.itemCount(),
                OrderStatus.CREATED
        );

        Order savedOrder = orderRepository.save(order);
        PaymentProcessResponse payment = paymentServiceClient.processPayment(savedOrder, request.paymentMethod());
        savedOrder.setStatus(payment.successful() ? OrderStatus.CONFIRMED : OrderStatus.CANCELLED);
        savedOrder = orderRepository.save(savedOrder);
        eventPublisher.publishOrderCreated(savedOrder);
        return OrderResponse.from(savedOrder);
    }

    @Transactional
    public OrderResponse updateOrder(UUID id, UpdateOrderRequest request) {
        Order order = findOrder(id);
        OrderStatus previousStatus = order.getStatus();
        order.setCustomerId(request.customerId());
        order.setTotalAmount(request.totalAmount());
        order.setCurrency(request.currency());
        order.setItemCount(request.itemCount());
        order.setStatus(request.status());

        Order savedOrder = orderRepository.save(order);
        eventPublisher.publishOrderUpdated(savedOrder, previousStatus);
        return OrderResponse.from(savedOrder);
    }

    @Transactional
    public void deleteOrder(UUID id) {
        Order order = findOrder(id);
        orderRepository.delete(order);
        eventPublisher.publishOrderDeleted(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {
        return OrderResponse.from(findOrder(id));
    }

    private Order findOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }
}
