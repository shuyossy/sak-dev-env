package sak.sample.itinerary.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

/** Trip 作成・更新の入力フォーム。 */
@Data
public class TripForm {
  @NotBlank
  @Size(max = 100)
  private String title;

  @NotBlank
  @Size(max = 100)
  private String destination;

  @NotNull private LocalDate startDate;

  @NotNull private LocalDate endDate;
}
