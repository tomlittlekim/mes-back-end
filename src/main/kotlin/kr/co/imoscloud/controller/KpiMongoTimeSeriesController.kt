package kr.co.imoscloud.controller

import kr.co.imoscloud.service.sensor.DeviceDataDto
import kr.co.imoscloud.service.sensor.KpiMongoTimeSeriesService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/kpi/devices")
class KpiMongoTimeSeriesController(
    private val kpiMongoTimeSeriesService: KpiMongoTimeSeriesService
) {
    @PostMapping("/data")
    fun receive(@RequestBody dto: DeviceDataDto) = kpiMongoTimeSeriesService.saveKpiMongoData(dto)
}