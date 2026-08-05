package org.chijai.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.chijai.booking.domain.Hotel;
import org.chijai.booking.repository.HotelRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HotelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HotelRepository hotelRepository;

    @Test
    void getHotelReturnsHotelById() throws Exception {
        mockMvc.perform(get("/hotel/{id}", 1).with(httpBasic("user", "password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("City Center Hotel"))
                .andExpect(jsonPath("$.cityId").value(1))
                .andExpect(jsonPath("$.deleted").value(false));
    }

    @Test
    void getHotelReturnsNotFoundForUnknownOrDeletedHotel() throws Exception {
        mockMvc.perform(get("/hotel/{id}", 5).with(httpBasic("user", "password")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Hotel not found with id: 5"));
    }

    @Test
    void deleteHotelMarksHotelAsDeletedWithoutRemovingTheRow() throws Exception {
        mockMvc.perform(delete("/hotel/{id}", 4).with(httpBasic("admin", "admin")))
                .andExpect(status().isNoContent());

        Hotel deletedHotel = hotelRepository.findById(4L).orElseThrow();
        assertThat(deletedHotel.isDeleted()).isTrue();

        mockMvc.perform(get("/hotel/{id}", 4).with(httpBasic("admin", "admin")))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotDeleteHotel() throws Exception {
        mockMvc.perform(delete("/hotel/{id}", 3).with(httpBasic("user", "password")))
                .andExpect(status().isForbidden());
    }

    @Test
    void searchReturnsActiveHotelsInCitySortedByDistanceFromCityCenter() throws Exception {
        String body = mockMvc.perform(get("/search/{cityId}", 1).with(httpBasic("user", "password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Map<String, Object>> hotels = objectMapper.readValue(body, new TypeReference<>() {
        });

        assertThat(hotels)
                .extracting(hotel -> hotel.get("name"))
                .containsExactly("City Center Hotel", "Midtown Stay", "Brooklyn Lodge");

        assertThat(hotels)
                .extracting(hotel -> ((Number) hotel.get("distanceFromCityCenterKm")).doubleValue())
                .isSorted();
    }

    @Test
    void searchReturnsNotFoundForUnknownCity() throws Exception {
        mockMvc.perform(get("/search/{cityId}", 999).with(httpBasic("user", "password")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("City not found with id: 999"));
    }

    @Test
    void uiAndOpenApiSpecArePublic() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/openapi.yaml"))
                .andExpect(status().isOk());
    }
}
