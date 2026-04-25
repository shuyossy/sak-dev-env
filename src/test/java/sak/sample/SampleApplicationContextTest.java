package sak.sample;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Spring コンテキストが起動できることを mock プロファイルで確認する。 */
@SpringBootTest
@ActiveProfiles("mock")
class SampleApplicationContextTest {

  @Test
  void コンテキストが起動できる() {
    // @SpringBootTest が起動できれば成功
  }
}
