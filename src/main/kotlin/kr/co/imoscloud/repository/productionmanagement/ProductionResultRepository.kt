package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import kr.co.imoscloud.service.sensor.ChartResponseModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductionResultRepository : JpaRepository<ProductionResult, Long>, ProductionResultRepositoryCustom {
    fun findBySiteAndCompCdAndProdResultId(site: String, compCd: String, prodResultId: String): ProductionResult?

    @Query(
        value = """
            SELECT
                LPAD(HOUR(PROD_END_TIME), 2, '0') AS timeLabel,
                COMP_CD as label,
                ROUND(
                    IFNULL(SUM(DEFECT_QTY), 0) / NULLIF(SUM(GOOD_QTY) + SUM(DEFECT_QTY), 0) * 100, 2
                ) AS value
            FROM
                PRODUCTION_RESULT
            WHERE
                site = :site
                AND COMP_CD = :compCd
                AND FLAG_ACTIVE = '1'
                AND PROD_END_TIME >= :start
                AND PROD_END_TIME < :end
            GROUP BY
                LPAD(HOUR(PROD_END_TIME), 2, '0')
            ORDER BY timeLabel
        """, nativeQuery = true
    )
    fun findHourlyDefectRates(
        @Param("site") site: String,
        @Param("compCd") compCd: String,
        @Param("start") start: java.time.LocalDateTime,
        @Param("end") end: java.time.LocalDateTime
    ): List<ChartResponseModel>

    @Query(
        value = """
            SELECT
                CAST(DATE(PROD_END_TIME) AS CHAR) AS timeLabel,   
                COMP_CD as label,
                ROUND(
                    IFNULL(SUM(DEFECT_QTY), 0) / NULLIF(SUM(GOOD_QTY) + SUM(DEFECT_QTY), 0) * 100, 2
                ) AS value
            FROM
                PRODUCTION_RESULT
            WHERE
                site = :site
                AND COMP_CD = :compCd
                AND FLAG_ACTIVE = '1'
                AND PROD_END_TIME >= :start
                AND PROD_END_TIME < :end
            GROUP BY
                CAST(DATE(PROD_END_TIME) AS CHAR)   
            ORDER BY timeLabel
        """, nativeQuery = true
    )
    fun findDayDefectRates(
        @Param("site") site: String,
        @Param("compCd") compCd: String,
        @Param("start") start: java.time.LocalDateTime,
        @Param("end") end: java.time.LocalDateTime
    ): List<ChartResponseModel>

}