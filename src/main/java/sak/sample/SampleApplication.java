package sak.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** 旅程作成サンプルアプリのエントリポイント。 */
@EnableJpaAuditing
@SpringBootApplication
public class SampleApplication {

  /**
   * アプリケーションのエントリポイント。
   *
   * @param args コマンドライン引数
   */
  public static void main(final String[] args) {
    SpringApplication.run(SampleApplication.class, args);
  }
}
