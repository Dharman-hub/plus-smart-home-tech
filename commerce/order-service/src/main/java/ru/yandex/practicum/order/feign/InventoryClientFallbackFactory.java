package ru.yandex.practicum.order.feign;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;

@Component
public class InventoryClientFallbackFactory implements FallbackFactory<InventoryClient> {

    private static final Logger log =
            LoggerFactory.getLogger(InventoryClientFallbackFactory.class);

    @Override
    public InventoryClient create(Throwable cause) {
        return new InventoryClient() {

            @Override
            public ReserveResponse reserveStock(ReserveRequest request) {
                log.warn(
                        "Fallback inventory-service reserve для productId={}: {}",
                        request.productId(),
                        cause.toString()
                );

                throwMappedException(request.productId(), cause);
                return null;
            }

            @Override
            public ReserveResponse releaseStock(ReserveRequest request) {
                log.warn(
                        "Fallback inventory-service release для productId={}: {}",
                        request.productId(),
                        cause.toString()
                );

                throwMappedException(request.productId(), cause);
                return null;
            }
        };
    }

    private void throwMappedException(Long productId, Throwable cause) {
        if (cause instanceof FeignException feignException
                && feignException.status() >= 400
                && feignException.status() < 500) {
            throw feignException;
        }

        throw new InventoryServiceUnavailableException(productId, cause);
    }
}