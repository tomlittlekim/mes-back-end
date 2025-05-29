package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.DefectInfo
import kr.co.imoscloud.model.productionmanagement.DefectInfoFilter
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface DefectInfoRepository : JpaRepository<DefectInfo, Long>, DefectInfoRepositoryCustom {

    /**
     * 생산실적 ID로 불량 정보 조회 (CODE 테이블과 JOIN하여 defectCauseName 포함, ProductionResult와 JOIN하여 equipmentId 포함)
     */
    @Query("""
        SELECT d, c.codeName, pr.equipmentId
        FROM DefectInfo d
        LEFT JOIN ProductionResult pr ON d.site = pr.site 
            AND d.compCd = pr.compCd 
            AND d.prodResultId = pr.prodResultId
        LEFT JOIN Code c ON c.site = 'default' 
            AND c.compCd = 'default' 
            AND d.defectCause = c.codeId 
            AND c.codeClassId = 'DEFECT_TYPE' 
            AND c.flagActive = true
        WHERE d.site = :site 
            AND d.compCd = :compCd 
            AND d.prodResultId = :prodResultId
            AND d.flagActive = true
        ORDER BY d.createDate DESC
    """)
    fun findDefectInfoWithCauseNameByProdResultId(
        @Param("site") site: String,
        @Param("compCd") compCd: String,
        @Param("prodResultId") prodResultId: String
    ): List<Array<Any?>>

    /**
     * 일자별 불량 통계용 조회 (CODE 테이블과 JOIN하여 defectCauseName 포함, ProductionResult와 JOIN하여 equipmentId 포함)
     */
    @Query("""
        SELECT d, c.codeName, pr.equipmentId
        FROM DefectInfo d
        LEFT JOIN ProductionResult pr ON d.site = pr.site 
            AND d.compCd = pr.compCd 
            AND d.prodResultId = pr.prodResultId
        LEFT JOIN Code c ON c.site = 'default' 
            AND c.compCd = 'default' 
            AND d.defectCause = c.codeId 
            AND c.codeClassId = 'DEFECT_TYPE' 
            AND c.flagActive = true
        WHERE d.site = :site 
            AND d.compCd = :compCd 
            AND d.createDate BETWEEN :fromDate AND :toDate
            AND d.flagActive = true
        ORDER BY d.createDate ASC
    """)
    fun findDefectInfoWithCauseNameForStats(
        @Param("site") site: String,
        @Param("compCd") compCd: String,
        @Param("fromDate") fromDate: LocalDateTime,
        @Param("toDate") toDate: LocalDateTime
    ): List<Array<Any?>>

    /**
     * 필터 조건으로 불량 정보 조회 (CODE 테이블과 JOIN하여 defectCauseName 포함, ProductionResult와 JOIN하여 equipmentId 포함)
     */
    @Query("""
        SELECT d, c.codeName, pr.equipmentId
        FROM DefectInfo d
        LEFT JOIN ProductionResult pr ON d.site = pr.site 
            AND d.compCd = pr.compCd 
            AND d.prodResultId = pr.prodResultId
        LEFT JOIN Code c ON c.site = 'default' 
            AND c.compCd = 'default' 
            AND d.defectCause = c.codeId 
            AND c.codeClassId = 'DEFECT_TYPE' 
            AND c.flagActive = true
        WHERE d.site = :site 
            AND d.compCd = :compCd 
            AND d.flagActive = true
            AND (:defectId IS NULL OR d.defectId LIKE CONCAT('%', :defectId, '%'))
            AND (:prodResultId IS NULL OR d.prodResultId LIKE CONCAT('%', :prodResultId, '%'))
            AND (:productId IS NULL OR d.productId = :productId)
            AND (:equipmentId IS NULL OR pr.equipmentId = :equipmentId)
            AND (:fromDate IS NULL OR d.createDate >= :fromDate)
            AND (:toDate IS NULL OR d.createDate <= :toDate)
        ORDER BY d.id DESC
    """)
    fun findDefectInfoWithCauseNameByFilter(
        @Param("site") site: String,
        @Param("compCd") compCd: String,
        @Param("defectId") defectId: String?,
        @Param("prodResultId") prodResultId: String?,
        @Param("productId") productId: String?,
        @Param("equipmentId") equipmentId: String?,
        @Param("fromDate") fromDate: LocalDateTime?,
        @Param("toDate") toDate: LocalDateTime?
    ): List<Array<Any?>>

}