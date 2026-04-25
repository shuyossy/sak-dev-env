package sak.sample.itinerary.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/** AI 関連の Bean 定義。本番系は Spring AI auto-config に任せ、mock プロファイルでスタブを差し込む。 */
@Slf4j
@Configuration
public class AiConfig {

  /** mock プロファイル時のみ有効な、固定 JSON を返す ChatModel スタブ。 */
  @Bean
  @Primary
  @Profile("mock")
  public ChatModel mockChatModel() {
    log.info("ChatModel: mock スタブを使用");
    final String json =
        """
{"date":"2026-05-02","time":"10:00","title":"金閣寺見学","location":"京都市","note":"曇りでも屋外散策可","weatherSummary":"曇り"}
""";
    return prompt -> {
      Generation generation = new Generation(new AssistantMessage(json));
      return new ChatResponse(java.util.List.of(generation));
    };
  }
}
