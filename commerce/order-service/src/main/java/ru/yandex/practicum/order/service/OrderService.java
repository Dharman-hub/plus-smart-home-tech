package ru.yandex.practicum.order.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.entity.OrderItem;
import ru.yandex.practicum.order.entity.OrderStatus;
import ru.yandex.practicum.order.exception.NotFoundException;
import ru.yandex.practicum.order.feign.ProductDto;
import ru.yandex.practicum.order.mapper.OrderMapper;
import ru.yandex.practicum.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    public OrderService(OrderRepository orderRepository,
                        OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }

    @Transactional
    public OrderDto saveConfirmedOrder(CreateOrderRequest request,
                                       Map<Long, ProductDto> products) {
        return saveOrder(
                request,
                products,
                OrderStatus.CONFIRMED,
                "Заказ подтверждён"
        );
    }

    @Transactional
    public OrderDto savePendingOrder(CreateOrderRequest request,
                                     Map<Long, ProductDto> products,
                                     String statusDetails) {
        return saveOrder(
                request,
                products,
                OrderStatus.PENDING_CONFIRMATION,
                statusDetails
        );
    }

    private OrderDto saveOrder(CreateOrderRequest request,
                               Map<Long, ProductDto> products,
                               OrderStatus status,
                               String statusDetails) {
        Order order = new Order();

        order.setCustomerName(request.customerName());
        order.setCustomerEmail(request.customerEmail());
        order.setStatus(status);
        order.setStatusDetails(statusDetails);
        order.setCreatedAt(LocalDateTime.now());

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.items()) {
            ProductDto product = products.get(itemRequest.productId());

            OrderItem item = new OrderItem();

            item.setProductId(itemRequest.productId());
            item.setProductName(product.name());
            item.setQuantity(itemRequest.quantity());
            item.setPrice(product.price());

            order.addItem(item);

            BigDecimal itemTotal = product.price()
                    .multiply(BigDecimal.valueOf(itemRequest.quantity()));

            totalPrice = totalPrice.add(itemTotal);
        }

        order.setTotalPrice(totalPrice);

        return orderMapper.toDto(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderDto getById(Long id) {
        return orderMapper.toDto(findById(id));
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getAll() {
        return orderRepository.findAll()
                .stream()
                .map(orderMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getByEmail(String email) {
        return orderRepository.findAllByCustomerEmail(email)
                .stream()
                .map(orderMapper::toDto)
                .toList();
    }

    private Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Заказ с id " + id + " не найден"
                ));
    }
}