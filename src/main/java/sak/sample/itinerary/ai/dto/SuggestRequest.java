package sak.sample.itinerary.ai.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

/** AI 提案リクエスト。 */
@Data
public class SuggestRequest {
  @NotNull private LocalDate date;
}
