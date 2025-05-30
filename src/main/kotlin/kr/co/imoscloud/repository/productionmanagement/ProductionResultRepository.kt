package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import kr.co.imoscloud.model.kpi.ChartResponseModel
import kr.co.imoscloud.model.kpi.ProductionRateModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductionResultRepository : JpaRepository<ProductionResult, Long>, ProductionResultRepositoryCustom {
    fun findBySiteAndCompCdAndProdResultId(site: String, compCd: String, prodResultId: String): ProductionResult?

    /**
     * 특정 작업지시에 연결된 활성 생산실적이 존재하는지 확인
     */
    fun existsBySiteAndCompCdAndWorkOrderIdAndFlagActive(
        site: String, 
        compCd: String, 
        workOrderId: String, 
        flagActive: Boolean
    ): Boolean

    /**
     * 다중 생산실적 ID로 활성 생산실적 목록 조회 (JPA Query Method - 간단하고 효율적)
     */
    fun findBySiteAndCompCdAndProdResultIdInAndFlagActive(
        site: String,
        compCd: String,
        prodResultId: List<String>,
        flagActive: Boolean
    ): List<ProductionResult>

    @Query(
        value = """
            SELECT
                LPAD(HOUR(PROD_END_TIME), 2, '0') AS timeLabel,
                c.COMPANY_NAME as label,
                ROUND(
                    IFNULL(SUM(DEFECT_QTY), 0) / NULLIF(SUM(GOOD_QTY) + SUM(DEFECT_QTY), 0) * 100, 2
                ) AS value
            FROM
                PRODUCTION_RESULT pr
            JOIN 
                COMPANY c
            ON
                c.SITE = pr.SITE
            AND c.COMP_CD = pr.COMP_CD
            AND c.FLAG_ACTIVE = '1'
            WHERE
                pr.site = :site
                AND pr.COMP_CD = :compCd
                AND pr.FLAG_ACTIVE = '1'
                AND PROD_END_TIME >= :start
                AND PROD_END_TIME < :end
            GROUP BY
                LPAD(HOUR(PROD_END_TIME), 2, '0'),c.COMPANY_NAME
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
                c.COMPANY_NAME as label,
                ROUND(
                    IFNULL(SUM(DEFECT_QTY), 0) / NULLIF(SUM(GOOD_QTY) + SUM(DEFECT_QTY), 0) * 100, 2
                ) AS value
            FROM
                PRODUCTION_RESULT pr
            JOIN 
                COMPANY c 
            ON
                c.SITE = pr.SITE
            AND c.COMP_CD = pr.COMP_CD
            AND c.FLAG_ACTIVE = '1'
            WHERE
                pr.site = :site
                AND pr.COMP_CD = :compCd
                AND pr.FLAG_ACTIVE = '1'
                AND PROD_END_TIME >= :start
                AND PROD_END_TIME < :end
            GROUP BY
                CAST(DATE(PROD_END_TIME) AS CHAR), c.COMPANY_NAME   
            ORDER BY timeLabel
        """, nativeQuery = true
    )
    fun findDayDefectRates(
        @Param("site") site: String,
        @Param("compCd") compCd: String,
        @Param("start") start: java.time.LocalDateTime,
        @Param("end") end: java.time.LocalDateTime
    ): List<ChartResponseModel>

    @Query("""
        SELECT
            c.SITE,
            c.COMP_CD,
            IFNULL(pp.plan_sum, 0) AS planSum,
            IFNULL(pr1.result_sum, 0) AS workOrderSum,
            IFNULL(pr2.result_sum2, 0) AS notWorkOrderSum,
            IFNULL(ROUND(
                    (IFNULL(pr1.result_sum, 0) + IFNULL(pr2.result_sum2, 0))
                        / NULLIF(IFNULL(pp.plan_sum, 0) + IFNULL(pr2.result_sum2, 0), 0) * 100, 2
            ),0) AS productionRate,
            DATE_FORMAT(now(), '%Y-%m-%d %H:00:00') AS aggregation_time
        FROM COMPANY c
            LEFT JOIN (
            SELECT SITE, COMP_CD, SUM(PLAN_QTY) AS plan_sum
            FROM PRODUCTION_PLAN
            WHERE FLAG_ACTIVE = 1
            GROUP BY SITE, COMP_CD
        ) pp ON pp.SITE = c.SITE AND pp.COMP_CD = c.COMP_CD
                 LEFT JOIN (
            SELECT SITE, COMP_CD, SUM(GOOD_QTY) AS result_sum
            FROM PRODUCTION_RESULT
            WHERE FLAG_ACTIVE = 1 AND WORK_ORDER_ID IS NOT NULL
            GROUP BY SITE, COMP_CD
        ) pr1 ON pr1.SITE = c.SITE AND pr1.COMP_CD = c.COMP_CD
                 LEFT JOIN (
            SELECT SITE, COMP_CD, SUM(GOOD_QTY) AS result_sum2
            FROM PRODUCTION_RESULT
            WHERE FLAG_ACTIVE = 1 AND WORK_ORDER_ID IS NULL
            GROUP BY SITE, COMP_CD
        ) pr2 ON pr2.SITE = c.SITE AND pr2.COMP_CD = c.COMP_CD
        WHERE c.FLAG_ACTIVE = 1
        GROUP BY c.COMP_CD, c.SITE
    """, nativeQuery = true)
    fun findProductionYieldRate(): List<ProductionRateModel>
}