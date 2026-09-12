package ru.yandex.practicum.order.exception;

public class InventoryServiceUnavailableException extends RuntimeException {

    private final Long productId;

    public InventoryServiceUnavailableException(Long productId, Throwable cause) {
        super("Inventory service is unavailable for product id=" + productId, cause);
        this.productId = productId;
    }

    public Long getProductId() {
        return productId;
    }
}