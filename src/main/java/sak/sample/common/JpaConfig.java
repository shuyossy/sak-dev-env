package sak.sample.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** JPA 関連の設定を集約。 */
/*
  @EnableJpaAuditing をエントリポイントから分離することで、
  @WebMvcTest など JPA を起動しない slice テストへ波及しないようにする。
*/
@Configuration
@EnableJpaAuditing
public class JpaConfig {}
