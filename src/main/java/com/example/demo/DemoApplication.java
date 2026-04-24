package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Spring Boot アプリケーションのエントリーポイント。 */
// Spring Boot の @SpringBootApplication クラスはコンテナが個別インスタンス化する前提であり、
// 全メンバを static にした utility class に置き換えると起動に失敗する。
@SuppressWarnings("PMD.UseUtilityClass")
@SpringBootApplication
public class DemoApplication {

  /**
   * アプリケーションのエントリポイント。
   *
   * @param args コマンドライン引数
   */
  public static void main(final String[] args) {
    SpringApplication.run(DemoApplication.class, args);
  }
}
