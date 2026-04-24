package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Spring コンテキスト起動の smoke test。 */
// JUnit 5 のテストクラスは慣習的に package-private で宣言するため、
// CommentDefaultAccessModifier の指摘を抑制する。
@SuppressWarnings("PMD.CommentDefaultAccessModifier")
@SpringBootTest
class DemoApplicationTests {

  /** アプリケーションコンテキストが正常に起動することを検証する。 */
  @Test
  void contextLoads() {
    // Spring Boot のコンテキスト起動のみを検証する smoke test
  }
}
