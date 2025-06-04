package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.DefectInfo

interface DefectInfoRepositoryCustom {

    /**
     * 다중 생산실적 ID로 불량정보 목록 조회 (배치 삭제용)
     */
    fun getDefectInfosByProdResultIds(
        site: String,
        compCd: String,
        prodResultIds: List<String>
    ): List<DefectInfo>

    /**
     * 다중 생산실적 ID로 불량정보 배치 소프트 삭제 (QueryDSL + @Transactional)
     */
    fun batchSoftDeleteDefectInfosByProdResultIds(
        site: String,
        compCd: String,
        prodResultIds: List<String>,
        updateUser: String,
        updateDate: java.time.LocalDateTime
    ): Long

    /**
     * 필터 조건으로 불량 정보 조회 (QueryDSL + JOIN으로 createUserName 포함)
     */
    fun getDefectInfosWithUserName(
        site: String,
        compCd: String,
        defectId: String?,
        prodResultId: String?,
        productId: String?,
        equipmentId: String?,
        fromDate: java.time.LocalDateTime?,
        toDate: java.time.LocalDateTime?
    ): List<DefectInfo>

    /**
     * 생산실적 ID로 불량 정보 조회 (QueryDSL + JOIN으로 createUserName 포함)
     */
    fun getDefectInfosByProdResultIdWithUserName(
        site: String,
        compCd: String,
        prodResultId: String
    ): List<DefectInfo>

    /**
     * 일자별 불량 통계용 조회 (QueryDSL + JOIN으로 createUserName 포함)
     */
    fun getDefectInfosForStatsWithUserName(
        site: String,
        compCd: String,
        fromDate: java.time.LocalDateTime,
        toDate: java.time.LocalDateTime
    ): List<DefectInfo>
}