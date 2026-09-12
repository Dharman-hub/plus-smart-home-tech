package ru.yandex.practicum.order;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;
import ru.yandex.practicum.order.feign.InventoryClient;
import ru.yandex.practicum.order.feign.ProductClient;
import ru.yandex.practicum.order.feign.ProductDto;
import ru.yandex.practicum.order.feign.ReserveRequest;
import ru.yandex.practicum.order.feign.ReserveResponse;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("unchecked")
class OrderServiceAcceptanceTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @MockBean
    private ProductClient productClient;

    @MockBean
    private InventoryClient inventoryClient;

    @Test
    void shouldCreateConfirmedOrder() throws Exception {
        when(productClient.getProductById(1L))
                .thenReturn(new ProductDto(
                        1L,
                        "Acceptance Smart Lamp",
                        new BigDecimal("3490.00"),
                        true
                ));

        when(productClient.getProductById(2L))
                .thenReturn(new ProductDto(
                        2L,
                        "Acceptance Smart Plug",
                        new BigDecimal("1290.00"),
                        true
                ));

        when(inventoryClient.reserveStock(any(ReserveRequest.class)))
                .thenReturn(new ReserveResponse(
                        true,
                        100,
                        "Резерв выполнен"
                ));

        CreateOrderRequest request = new CreateOrderRequest(
                "Acceptance Buyer",
                "acceptance-buyer@example.com",
                List.of(
                        new OrderItemRequest(1L, 2),
                        new OrderItemRequest(2L, 1)
                )
        );

        MvcResult response = postJson("/api/orders", request);

        assertThat(status(response)).isEqualTo(201);

        Map<String, Object> created = readMap(response);

        assertThat(created.get("status"))
                .isEqualTo("CONFIRMED");

        assertThat(asDecimal(created.get("totalPrice")))
                .isEqualByComparingTo("8270.00");

        assertThat((List<?>) created.get("items"))
                .hasSize(2);
    }

    @Test
    void shouldCreatePendingOrderWhenProductServiceUnavailable() throws Exception {
        when(productClient.getProductById(1L))
                .thenThrow(new ProductServiceUnavailableException(
                        1L,
                        new RuntimeException("product-service unavailable")
                ));

        CreateOrderRequest request = new CreateOrderRequest(
                "Pending Product Buyer",
                "pending-product@example.com",
                List.of(
                        new OrderItemRequest(1L, 2)
                )
        );

        MvcResult response = postJson("/api/orders", request);

        assertThat(status(response)).isEqualTo(201);

        Map<String, Object> created = readMap(response);

        assertThat(created.get("status"))
                .isEqualTo("PENDING_CONFIRMATION");

        assertThat(created.get("statusDetails").toString())
                .contains("product-service");

        assertThat(asDecimal(created.get("totalPrice")))
                .isEqualByComparingTo("0");

        List<Map<String, Object>> items =
                (List<Map<String, Object>>) created.get("items");

        assertThat(items)
                .hasSize(1);

        assertThat(items.getFirst().get("productName"))
                .isEqualTo("Товар #1 (ожидает проверки)");
    }

    @Test
    void shouldCreatePendingOrderWhenInventoryServiceUnavailable() throws Exception {
        when(productClient.getProductById(1L))
                .thenReturn(new ProductDto(
                        1L,
                        "Acceptance Smart Lamp",
                        new BigDecimal("3490.00"),
                        true
                ));

        when(inventoryClient.reserveStock(any(ReserveRequest.class)))
                .thenThrow(new InventoryServiceUnavailableException(
                        1L,
                        new RuntimeException("inventory-service unavailable")
                ));

        CreateOrderRequest request = new CreateOrderRequest(
                "Pending Inventory Buyer",
                "pending-inventory@example.com",
                List.of(
                        new OrderItemRequest(1L, 2)
                )
        );

        MvcResult response = postJson("/api/orders", request);

        assertThat(status(response)).isEqualTo(201);

        Map<String, Object> created = readMap(response);

        assertThat(created.get("status"))
                .isEqualTo("PENDING_CONFIRMATION");

        assertThat(created.get("statusDetails").toString())
                .contains("резервирование");

        assertThat(asDecimal(created.get("totalPrice")))
                .isEqualByComparingTo("6980.00");
    }

    @Test
    void shouldFindCreatedOrderByIdAndEmail() throws Exception {
        when(productClient.getProductById(1L))
                .thenReturn(new ProductDto(
                        1L,
                        "Search Smart Lamp",
                        new BigDecimal("1000.00"),
                        true
                ));

        when(inventoryClient.reserveStock(any(ReserveRequest.class)))
                .thenReturn(new ReserveResponse(
                        true,
                        100,
                        "Резерв выполнен"
                ));

        CreateOrderRequest request = new CreateOrderRequest(
                "Search Buyer",
                "search-buyer@example.com",
                List.of(
                        new OrderItemRequest(1L, 1)
                )
        );

        MvcResult createResponse = postJson("/api/orders", request);
        Map<String, Object> created = readMap(createResponse);

        Long orderId = asLong(created.get("id"));

        MvcResult byIdResponse = mvc.perform(
                get("/api/orders/{id}", orderId)
        ).andReturn();

        assertThat(status(byIdResponse)).isEqualTo(200);

        assertThat(readMap(byIdResponse).get("customerEmail"))
                .isEqualTo("search-buyer@example.com");

        MvcResult byEmailResponse = mvc.perform(
                get("/api/orders/by-email")
                        .param("email", "search-buyer@example.com")
        ).andReturn();

        assertThat(status(byEmailResponse)).isEqualTo(200);

        assertThat(readList(byEmailResponse))
                .anySatisfy(item -> assertThat(item)
                        .containsEntry(
                                "customerEmail",
                                "search-buyer@example.com"
                        ));
    }

    @Test
    void shouldReturnBadRequestForInvalidOrderPayload() throws Exception {
        CreateOrderRequest invalidRequest = new CreateOrderRequest(
                "",
                "not-an-email",
                List.of()
        );

        MvcResult response = postJson("/api/orders", invalidRequest);

        assertThat(status(response)).isEqualTo(400);

        assertThat(readMap(response))
                .containsKeys("message", "validationErrors");
    }

    private MvcResult postJson(String url, Object body) throws Exception {
        return mvc.perform(
                post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body))
        ).andReturn();
    }

    private int status(MvcResult result) {
        return result.getResponse().getStatus();
    }

    private Map<String, Object> readMap(MvcResult result) throws Exception {
        return json.readValue(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8),
                new TypeReference<>() {
                }
        );
    }

    private List<Map<String, Object>> readList(MvcResult result) throws Exception {
        return json.readValue(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8),
                new TypeReference<>() {
                }
        );
    }

    private Long asLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private BigDecimal asDecimal(Object value) {
        return new BigDecimal(value.toString());
    }
}