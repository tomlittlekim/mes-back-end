package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.DefectInfo
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
     * 일자별 불량 통계용 조회
     */
    fun getDefectInfoForStats(
        site: String,
        compCd: String,
        fromDate: LocalDateTime,
        toDate: LocalDateTime
    ): List<DefectInfo>
}