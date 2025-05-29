package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.ProductionRateDay
import kr.co.imoscloud.service.sensor.ChartResponseModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductionRateDayRep: JpaRepository<ProductionRateDay, Long> {
    @Query(
        value = """
            SELECT
                CAST(DATE(DATE_SUB(prd.AGGREGATION_TIME, INTERVAL 1 DAY)) AS CHAR) AS timeLabel,
                c.COMPANY_NAME as label,
                prd.PRODUCTION_RATE AS value
            FROM
                PRODUCTION_RATE_DAY prd
            JOIN 
                COMPANY c
            ON
                c.SITE = prd.SITE
            AND c.COMP_CD = prd.COMP_CD
            AND c.FLAG_ACTIVE = '1'
            WHERE
                prd.site = :site
                AND prd.COMP_CD = :compCd
                AND prd.AGGREGATION_TIME >= :start
                AND prd.AGGREGATION_TIME < :end
        """, nativeQuery = true
    )
    fun findDayProductionYieldRates(
        @Param("site") site: String,
        @Param("compCd") compCd: String,
        @Param("start") start: java.time.LocalDateTime,
        @Param("end") end: java.time.LocalDateTime
    ): List<ChartResponseModel>
}