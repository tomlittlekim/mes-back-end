package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.DefectInfo
import java.time.LocalDateTime

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

}