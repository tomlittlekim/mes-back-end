package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.ProductionRateHour
import kr.co.imoscloud.model.kpi.ChartResponseModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductionRateHourRep : JpaRepository<ProductionRateHour, Long>{
    @Query(
        value = """
            SELECT
                LPAD(HOUR(DATE_SUB(prh.AGGREGATION_TIME, INTERVAL 1 HOUR)), 2, '0') AS timeLabel,
                c.COMPANY_NAME as label,
                prh.PRODUCTION_RATE AS value
            FROM
                PRODUCTION_RATE_HOUR prh
            JOIN 
                COMPANY c 
            ON
                c.SITE = prh.SITE
            AND c.COMP_CD = prh.COMP_CD
            AND c.FLAG_ACTIVE = '1'
            WHERE
                prh.site = :site
                AND prh.COMP_CD = :compCd
                AND prh.AGGREGATION_TIME > :start
                AND prh.AGGREGATION_TIME <= :end
        """, nativeQuery = true
    )
    fun findHourProductionYieldRates(
        @Param("site") site: String,
        @Param("compCd") compCd: String,
        @Param("start") start: java.time.LocalDateTime,
        @Param("end") end: java.time.LocalDateTime
    ): List<ChartResponseModel>
}