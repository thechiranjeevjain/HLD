package dev.portfolio.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryApiIntegrationTest {
    @Autowired MockMvc mvc;

    @Test void handlesAppliedStaleAndDuplicateUpdatesAndSummary() throws Exception {
        mvc.perform(post("/api/v1/inventory/updates").contentType(MediaType.APPLICATION_JSON).content(update("u-2", 2, 25)))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.status").value("APPLIED"));
        mvc.perform(post("/api/v1/inventory/updates").contentType(MediaType.APPLICATION_JSON).content(update("u-1", 1, 10)))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.status").value("STALE_IGNORED"))
                .andExpect(jsonPath("$.inventory.quantity").value(25));
        mvc.perform(post("/api/v1/inventory/updates").contentType(MediaType.APPLICATION_JSON).content(update("u-2", 2, 25)))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.status").value("DUPLICATE_IGNORED"));
        mvc.perform(get("/api/v1/inventory/SKU-RED/summary"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalQuantity").value(25))
                .andExpect(jsonPath("$.reportingStores").value(1));
    }

    @Test void validatesBadInput() throws Exception {
        mvc.perform(post("/api/v1/inventory/updates").contentType(MediaType.APPLICATION_JSON)
                .content("{\"updateId\":\"\",\"sku\":\"S\",\"storeId\":\"A\",\"quantity\":-1,\"version\":1,\"eventTime\":\"2026-08-16T10:00:00Z\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
    private String update(String id, long version, long quantity) {
        return "{\"updateId\":\"" + id + "\",\"sku\":\"SKU-RED\",\"storeId\":\"STORE-101\",\"quantity\":" + quantity
                + ",\"version\":" + version + ",\"eventTime\":\"2026-08-16T10:0" + version + ":00Z\"}";
    }
}
