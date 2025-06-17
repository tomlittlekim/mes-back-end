package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.ProductionPlan
import kr.co.imoscloud.model.productionmanagement.DefectInfoProjection
import kr.co.imoscloud.model.productionmanagement.PeriodicProductionResponseDto
import kr.co.imoscloud.model.productionmanagement.PlanVsActualResponseDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductionPlanRepository: JpaRepository<ProductionPlan, Long>, ProductionPlanRepositoryCustom {
    fun findByProdPlanId(prodPlanId: String): ProductionPlan?

    // UK 필드를 모두 사용하는 메서드 추가
    fun findBySiteAndCompCdAndProdPlanId(
        site: String,
        compCd: String,
        prodPlanId: String
    ): ProductionPlan?

    @Query(
        value = """
        SELECT
            pp.PROD_PLAN_ID AS prodPlanId,
            pp.PLAN_QTY AS planQty,
            COALESCE(SUM(wo.ORDER_QTY), 0) AS totalOrderQty,
            COALESCE(SUM(CASE WHEN wo.STATE = 'COMPLETED' THEN wo.ORDER_QTY ELSE 0 END), 0) AS completedOrderQty,
            ROUND(
                CASE
                    WHEN pp.PLAN_QTY = 0 THEN 0.0
                    ELSE (COALESCE(SUM(CASE WHEN wo.STATE = 'COMPLETED' THEN wo.ORDER_QTY ELSE 0 END), 0) * 100.0) / pp.PLAN_QTY
                END,
                2
            ) AS achievementRate,
            mm.MATERIAL_NAME AS materialName,
            mm.SYSTEM_MATERIAL_ID AS systemMaterialId
        FROM PRODUCTION_PLAN pp
        LEFT JOIN WORK_ORDER wo 
            ON pp.PROD_PLAN_ID = wo.PROD_PLAN_ID 
            AND pp.SITE = wo.SITE 
            AND pp.COMP_CD = wo.COMP_CD
        JOIN MATERIAL_MASTER mm 
            ON mm.SYSTEM_MATERIAL_ID = pp.PRODUCT_ID 
            AND mm.SITE = pp.SITE 
            AND mm.COMP_CD = pp.COMP_CD
        WHERE
            (:compCd IS NULL OR pp.COMP_CD = :compCd)
            AND (:site IS NULL OR pp.SITE = :site)
            AND (:#{#systemMaterialIds == null || #systemMaterialIds.isEmpty()} = true OR mm.SYSTEM_MATERIAL_ID IN (:#{#systemMaterialIds}))
            AND (:flagActive IS NULL OR pp.FLAG_ACTIVE = :flagActive)
            AND (
                (:startDate IS NULL OR :startDate = '' OR :endDate IS NULL OR :endDate = '')
                OR (
                    pp.CREATE_DATE BETWEEN 
                    CASE 
                        WHEN :startDate IS NULL OR :startDate = '' 
                        THEN '1970-01-01' 
                        ELSE STR_TO_DATE(:startDate, '%Y-%m-%d') 
                    END
                    AND 
                    CASE 
                        WHEN :endDate IS NULL OR :endDate = '' 
                        THEN CURRENT_DATE() 
                        ELSE STR_TO_DATE(:endDate, '%Y-%m-%d') 
                    END
                )
            )
        GROUP BY
            pp.PROD_PLAN_ID,
            pp.PLAN_QTY,
            mm.MATERIAL_NAME,
            mm.SYSTEM_MATERIAL_ID
        """,
        nativeQuery = true
    )
    fun planVsActual(
        @Param("compCd") compCd: String?,
        @Param("site") site: String?,
        @Param("systemMaterialIds") systemMaterialIds: List<String>?,
        @Param("flagActive") flagActive: Boolean?,
        @Param("startDate") startDate: String?,
        @Param("endDate") endDate: String?
    ): List<PlanVsActualResponseDto>

    @Query(
        value = """
                select
            ifnull(mm.MATERIAL_NAME, '품명 미정') as MATERIAL_NAME,
            sum(pr.GOOD_QTY) as TOTAL_GOOD_QTY,
            sum(pr.DEFECT_QTY) as TOTAL_DEFECT_QTY,
            ifnull(round(
            (sum(pr.DEFECT_QTY) / (sum(pr.GOOD_QTY) + sum(pr.DEFECT_QTY))) * 100,2), 0) as TOTAL_DEFECT_RATE,
            mm.UNIT as UNIT,
            pr.PRODUCT_ID as PRODUCT_ID
        from PRODUCTION_RESULT pr
                 left join MATERIAL_MASTER mm on
                     (mm.SYSTEM_MATERIAL_ID = pr.PRODUCT_ID
                         and mm.SITE = pr.SITE
                         and mm.COMP_CD = pr.COMP_CD)
        WHERE
            (:compCd IS NULL OR pr.COMP_CD = :compCd)
            AND (:site IS NULL OR pr.SITE = :site)
            AND (:#{#systemMaterialIds == null || #systemMaterialIds.isEmpty()} = true OR mm.SYSTEM_MATERIAL_ID IN (:#{#systemMaterialIds}))
            AND (:flagActive IS NULL OR pr.FLAG_ACTIVE = :flagActive)
            AND (
                (:startDate IS NULL OR :startDate = '' OR :endDate IS NULL OR :endDate = '')
                OR (
                    pr.CREATE_DATE BETWEEN 
                    CASE 
                        WHEN :startDate IS NULL OR :startDate = '' 
                        THEN '1970-01-01' 
                        ELSE STR_TO_DATE(:startDate, '%Y-%m-%d') 
                    END
                    AND 
                    CASE 
                        WHEN :endDate IS NULL OR :endDate = '' 
                        THEN CURRENT_DATE() 
                        ELSE STR_TO_DATE(:endDate, '%Y-%m-%d') 
                    END
                )
            )
        group by mm.MATERIAL_NAME, mm.UNIT, pr.PRODUCT_ID
        having TOTAL_GOOD_QTY > 0
        """,
        nativeQuery = true
    )
    fun periodicProduction(
        @Param("compCd") compCd: String?,
        @Param("site") site: String?,
        @Param("systemMaterialIds") systemMaterialIds: List<String>?,
        @Param("flagActive") flagActive: Boolean?,
        @Param("startDate") startDate: String?,
        @Param("endDate") endDate: String?
    ): List<PeriodicProductionResponseDto>


    @Query(
        value = """
        SELECT
            di.DEFECT_QTY as defectQty,
            di.CREATE_DATE as createDate,
            IFNULL(c.CODE_NAME, '기타불량') as codeName,
            IFNULL(c.CODE_DESC, '기타불량') as codeDesc
        FROM
        DEFECT_INFO di
        LEFT JOIN
        CODE c ON di.defect_cause = c.code_id
        AND c.CODE_CLASS_ID = 'DEFECT_TYPE'
        WHERE 1=1
        AND di.COMP_CD = :compCd
        AND di.SITE = :site
        AND di.PRODUCT_ID = :productId
    """,
        nativeQuery = true
    )
    fun getDefectInfo(
        @Param("compCd") compCd: String?,
        @Param("site") site: String?,
        @Param("productId") productId: String?
    ): List<DefectInfoProjection>  // Projection으로 변경


}