package kr.co.imoscloud.config

import org.jodconverter.core.office.OfficeManager
import org.jodconverter.local.office.ExternalOfficeManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JodConverterConfig {

    @Bean(destroyMethod = "stop")
    fun officeManager(): OfficeManager {
        return ExternalOfficeManager.builder()
            .hostName("libreoffice")
            .portNumbers(2002)
            .connectOnStart(true)
            .build()
            .also { it.start() }
    }
}