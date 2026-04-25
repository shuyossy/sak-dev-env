package sak.sample;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Trip 作成 → Activity 追加までの happy path を mock プロファイルで貫通させる統合テスト。 */
@SpringBootTest
@ActiveProfiles("mock")
class SampleApplicationIntegrationTest {

  @Autowired private WebApplicationContext context;

  @Test
  void Trip_作成から_Activity_追加までの_happy_path() throws Exception {
    MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
    ObjectMapper om = new ObjectMapper();

    MvcResult created =
        mvc.perform(
                post("/api/trips")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
{"title":"統合","destination":"京都","startDate":"2026-05-01","endDate":"2026-05-03"}
"""))
            .andExpect(status().isCreated())
            .andReturn();

    JsonNode node = om.readTree(created.getResponse().getContentAsString());
    long tripId = node.get("id").asLong();

    mvc.perform(
            post("/api/trips/" + tripId + "/activities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"date":"2026-05-01","title":"散歩"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("散歩"));
  }

  @Test
  void AI_提案_API_が_mock_ChatModel_経由で_Activity_を返す() throws Exception {
    MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
    ObjectMapper om = new ObjectMapper();

    MvcResult created =
        mvc.perform(
                post("/api/trips")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
{"title":"AI 統合","destination":"京都","startDate":"2026-05-01","endDate":"2026-05-03"}
"""))
            .andExpect(status().isCreated())
            .andReturn();

    JsonNode node = om.readTree(created.getResponse().getContentAsString());
    long tripId = node.get("id").asLong();

    mvc.perform(
            post("/api/trips/" + tripId + "/ai/suggest-activities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"date":"2026-05-02"}
                    """))
        .andExpect(status().isCreated())
        // mock スタブ JSON で title=金閣寺見学 を返すため確認できる
        .andExpect(jsonPath("$.title").value("金閣寺見学"));
  }
}
