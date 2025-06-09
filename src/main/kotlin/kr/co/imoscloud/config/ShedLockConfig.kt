package kr.co.imoscloud.config

import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "5m")
class ShedLockConfig {
    @Bean
    fun lockProvider(dataSource: DataSource) = JdbcTemplateLockProvider(
        JdbcTemplateLockProvider.Configuration.builder()
            .withJdbcTemplate(org.springframework.jdbc.core.JdbcTemplate(dataSource))
            .usingDbTime()
            .build()
    )
}