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
import ru.yandex.practicum.order.mapper.OrderMapper;
import ru.yandex.practicum.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    public OrderDto create(CreateOrderRequest request) {
        Order order = new Order();

        order.setCustomerName(request.customerName());
        order.setCustomerEmail(request.customerEmail());
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(LocalDateTime.now());

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.items()) {
            OrderItem item = new OrderItem();

            item.setProductId(itemRequest.productId());
            item.setProductName(itemRequest.productName());
            item.setQuantity(itemRequest.quantity());
            item.setPrice(itemRequest.price());

            order.addItem(item);

            BigDecimal itemTotal = itemRequest.price()
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