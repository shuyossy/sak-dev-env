package sak.sample.itinerary.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class WeatherToolTest {

  @Test
  void 決定的な_RandomGenerator_を渡せば結果が固定できる() {
    RandomGenerator fixed =
        new RandomGenerator() {
          @Override
          public long nextLong() {
            return 0L;
          }

          @Override
          public int nextInt(final int bound) {
            return 0;
          }
        };
    WeatherTool tool = new WeatherTool(fixed);

    String result = tool.getWeather(LocalDate.of(2026, 5, 1), "京都");

    assertThat(result).contains("京都").contains("2026-05-01").contains("晴れ");
  }
}
