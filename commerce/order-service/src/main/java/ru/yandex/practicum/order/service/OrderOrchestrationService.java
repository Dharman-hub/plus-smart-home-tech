package ru.yandex.practicum.order.service;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;
import ru.yandex.practicum.order.feign.InventoryClient;
import ru.yandex.practicum.order.feign.ProductClient;
import ru.yandex.practicum.order.feign.ProductDto;
import ru.yandex.practicum.order.feign.ReserveRequest;
import ru.yandex.practicum.order.feign.ReserveResponse;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class OrderOrchestrationService {

    private static final Logger log =
            LoggerFactory.getLogger(OrderOrchestrationService.class);

    private final OrderService orderService;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

    public OrderOrchestrationService(OrderService orderService,
                                     ProductClient productClient,
                                     InventoryClient inventoryClient) {
        this.orderService = orderService;
        this.productClient = productClient;
        this.inventoryClient = inventoryClient;
    }

    public OrderDto create(CreateOrderRequest request) {
        Map<Long, Integer> quantities = groupQuantities(request);

        ProductLoadResult productResult = loadProducts(quantities);

        if (productResult.degraded()) {
            return orderService.savePendingOrder(
                    request,
                    productResult.products(),
                    "Требуется ручная проверка: product-service временно недоступен"
            );
        }

        Map<Long, Integer> reservedProducts = new LinkedHashMap<>();

        try {
            reserveProducts(quantities, reservedProducts);
        } catch (InventoryServiceUnavailableException e) {
            releaseReservations(reservedProducts);

            return orderService.savePendingOrder(
                    request,
                    productResult.products(),
                    "Требуется ручная проверка: резервирование товара не подтверждено"
            );
        } catch (RuntimeException e) {
            releaseReservations(reservedProducts);
            throw e;
        }

        try {
            return orderService.saveConfirmedOrder(
                    request,
                    productResult.products()
            );
        } catch (RuntimeException e) {
            releaseReservations(reservedProducts);
            throw e;
        }
    }

    private Map<Long, Integer> groupQuantities(CreateOrderRequest request) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();

        for (OrderItemRequest item : request.items()) {
            quantities.merge(
                    item.productId(),
                    item.quantity(),
                    Integer::sum
            );
        }

        return quantities;
    }

    private ProductLoadResult loadProducts(Map<Long, Integer> quantities) {
        Map<Long, ProductDto> products = new HashMap<>();
        boolean degraded = false;

        for (Long productId : quantities.keySet()) {
            try {
                ProductDto product = getProduct(productId);

                if (!Boolean.TRUE.equals(product.active())) {
                    throw new OrderProcessingException(
                            "Товар с id " + productId + " снят с продажи"
                    );
                }

                products.put(productId, product);
            } catch (ProductServiceUnavailableException e) {
                degraded = true;

                products.put(
                        productId,
                        createPendingProduct(productId)
                );
            }
        }

        return new ProductLoadResult(products, degraded);
    }

    private ProductDto createPendingProduct(Long productId) {
        return new ProductDto(
                productId,
                "Товар #" + productId + " (ожидает проверки)",
                BigDecimal.ZERO,
                null
        );
    }

    private ProductDto getProduct(Long productId) {
        try {
            return productClient.getProductById(productId);
        } catch (FeignException e) {
            if (e.status() == 404) {
                throw new OrderProcessingException(
                        "Товар с id " + productId + " не найден"
                );
            }

            if (e.status() >= 400 && e.status() < 500) {
                throw new OrderProcessingException(
                        "Не удалось использовать товар с id " + productId
                );
            }

            throw e;
        }
    }

    private void reserveProducts(Map<Long, Integer> quantities,
                                 Map<Long, Integer> reservedProducts) {
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            reserveProduct(entry.getKey(), entry.getValue());

            reservedProducts.put(
                    entry.getKey(),
                    entry.getValue()
            );
        }
    }

    private void reserveProduct(Long productId, Integer quantity) {
        try {
            ReserveResponse response = inventoryClient.reserveStock(
                    new ReserveRequest(productId, quantity)
            );

            if (!response.success()) {
                throw new OrderProcessingException(
                        "Недостаточно товара с id " + productId + " на складе"
                );
            }
        } catch (FeignException e) {
            if (e.status() == 404) {
                throw new OrderProcessingException(
                        "Складская запись для товара с id "
                                + productId + " не найдена"
                );
            }

            if (e.status() == 409) {
                throw new OrderProcessingException(
                        "Недостаточно товара с id "
                                + productId + " на складе"
                );
            }

            if (e.status() >= 400 && e.status() < 500) {
                throw new OrderProcessingException(
                        "Не удалось зарезервировать товар с id " + productId
                );
            }

            throw e;
        }
    }

    private void releaseReservations(Map<Long, Integer> reservedProducts) {
        for (Map.Entry<Long, Integer> entry : reservedProducts.entrySet()) {
            try {
                inventoryClient.releaseStock(
                        new ReserveRequest(
                                entry.getKey(),
                                entry.getValue()
                        )
                );
            } catch (RuntimeException e) {
                log.error(
                        "Не удалось снять резерв товара id={}, quantity={}",
                        entry.getKey(),
                        entry.getValue(),
                        e
                );
            }
        }
    }

    private record ProductLoadResult(
            Map<Long, ProductDto> products,
            boolean degraded
    ) {
    }
}