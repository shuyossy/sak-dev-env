package sak.sample.itinerary.ai.tool;

import java.time.LocalDate;
import java.util.List;
import java.util.random.RandomGenerator;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** 天気取得ツール（モック）。Spring AI の @Tool 経由で LLM から呼び出される。 */
@Component
public class WeatherTool {

  private static final List<String> WEATHERS = List.of("晴れ", "曇り", "雨", "雪");
  private final RandomGenerator random;

  public WeatherTool() {
    this(RandomGenerator.getDefault());
  }

  /** テスト用に RandomGenerator を注入できるコンストラクタ。 */
  public WeatherTool(final RandomGenerator random) {
    this.random = random;
  }

  /**
   * 指定日・場所のランダムな天気を返す（実 API 接続はしない）。
   *
   * @param date 対象日
   * @param location 場所
   * @return 天気サマリ
   */
  @Tool(description = "指定された日付と場所の天気予報を返す")
  public String getWeather(
      @ToolParam(description = "対象日 (yyyy-MM-dd)") final LocalDate date,
      @ToolParam(description = "場所") final String location) {
    String weather = WEATHERS.get(random.nextInt(WEATHERS.size()));
    return location + " (" + date + "): " + weather;
  }
}
