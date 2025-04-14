package kr.co.imoscloud

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class ImosBackEndApplication

fun main(args: Array<String>) {
    runApplication<ImosBackEndApplication>(*args)
}
