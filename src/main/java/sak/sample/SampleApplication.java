package sak.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 旅程作成サンプルアプリのエントリポイント。 */
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
