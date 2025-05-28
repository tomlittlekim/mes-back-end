package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.ProductionRateHour
import org.springframework.data.jpa.repository.JpaRepository

interface ProductionRateHourRep : JpaRepository<ProductionRateHour, Long>