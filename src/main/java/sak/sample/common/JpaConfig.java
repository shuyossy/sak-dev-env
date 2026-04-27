package sak.sample.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** JPA 関連の設定を集約。エントリポイントから分離して slice テストへの波及を防ぐ。 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {}
