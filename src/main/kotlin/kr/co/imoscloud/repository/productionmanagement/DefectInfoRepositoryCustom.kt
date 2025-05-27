package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.DefectInfo
import kr.co.imoscloud.model.productionmanagement.DefectInfoFilter
import java.time.LocalDateTime

interface DefectInfoRepositoryCustom {

    /**
     * 생산실적 ID로 불량 정보 조회
     */
    fun getDefectInfoByProdResultId(
        site: String,
        compCd: String,
        prodResultId: String
    ): List<DefectInfo>

    /**
     * 필터 조건으로 불량 정보 조회
     */
    fun getDefectInfoByFilter(
        site: String,
        compCd: String,
        filter: DefectInfoFilter?
    ): List<DefectInfo>

    /**
     * 일자별 불량 통계용 조회
     */
    fun getDefectInfoForStats(
        site: String,
        compCd: String,
        fromDate: LocalDateTime,
        toDate: LocalDateTime
    ): List<DefectInfo>

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

}