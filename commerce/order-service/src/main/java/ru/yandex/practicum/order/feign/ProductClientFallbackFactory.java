package ru.yandex.practicum.order.feign;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;

@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {

    private static final Logger log =
            LoggerFactory.getLogger(ProductClientFallbackFactory.class);

    @Override
    public ProductClient create(Throwable cause) {
        return productId -> {
            log.warn(
                    "Fallback product-service для productId={}: {}",
                    productId,
                    cause.toString()
            );

            if (cause instanceof FeignException feignException
                    && feignException.status() >= 400
                    && feignException.status() < 500) {
                throw feignException;
            }

            throw new ProductServiceUnavailableException(productId, cause);
        };
    }
}