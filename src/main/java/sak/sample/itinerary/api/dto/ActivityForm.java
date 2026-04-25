package sak.sample.itinerary.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Data;

/** Activity 作成・更新の入力フォーム。 */
@Data
public class ActivityForm {
  @NotNull private LocalDate date;

  private LocalTime time;

  @NotBlank
  @Size(max = 100)
  private String title;

  @Size(max = 100)
  private String location;

  @Size(max = 500)
  private String note;
}
