package dev.portfolio.tracking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.Map;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class TrackingApiTest {
  @Autowired MockMvc mvc; @Autowired ObjectMapper json;
  @Test void buyerSeesMultiPackageTimeline() throws Exception {mvc.perform(get("/orders/ORD-1001/tracking").header("X-Actor-Id","user-123")).andExpect(status().isOk()).andExpect(jsonPath("$.shipments",hasSize(2))).andExpect(jsonPath("$.timeline",hasSize(greaterThanOrEqualTo(5)))).andExpect(jsonPath("$.currentStatus").value("SHIPPED"));}
  @Test void anotherCustomerIsForbiddenButSupportCanRead() throws Exception {mvc.perform(get("/orders/ORD-1001/tracking").header("X-Actor-Id","intruder")).andExpect(status().isForbidden());mvc.perform(get("/orders/ORD-1001/tracking").header("X-Actor-Id","agent-7").header("X-Actor-Role","SUPPORT")).andExpect(status().isOk());}
  @Test void duplicateAndLateEventAreSafe() throws Exception {
    Map<String,Object> body=Map.of("idempotencyKey","test-late-packed","carrier","UPS","trackingNumber","1Z-DEMO-001","eventType","PACKED","eventTime",Instant.now().minusSeconds(7200).toString(),"location","Tacoma, WA","rawPayload","late-scan-test");
    String payload=json.writeValueAsString(body);mvc.perform(post("/carrier/events").contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isAccepted()).andExpect(jsonPath("$.result").value("ACCEPTED"));
    mvc.perform(post("/carrier/events").contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isOk()).andExpect(jsonPath("$.result").value("DUPLICATE"));
    mvc.perform(get("/orders/ORD-1001/tracking").header("X-Actor-Id","user-123")).andExpect(jsonPath("$.currentStatus").value("SHIPPED"));
  }
  @Test void futureCarrierScanIsRejected() throws Exception {String payload=json.writeValueAsString(Map.of("idempotencyKey","future","carrier","UPS","trackingNumber","1Z-DEMO-001","eventType","DELIVERED","eventTime",Instant.now().plusSeconds(3600).toString(),"rawPayload","bad-clock"));mvc.perform(post("/carrier/events").contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_EVENT"));}
}
