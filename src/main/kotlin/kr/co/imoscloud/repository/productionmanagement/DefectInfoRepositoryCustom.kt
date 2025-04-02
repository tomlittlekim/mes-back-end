package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.DefectInfo
import java.time.LocalDateTime

interface DefectInfoRepositoryCustom {
    /**
     * 여러 조건으로 불량 정보 목록 조회
     */
    fun getDefectInfoList(
        site: String,
        compCd: String,
        workOrderId: String? = null,
        prodResultId: String? = null,
        defectId: String? = null,
        productId: String? = null,
        state: String? = null,
        defectType: String? = null,
        equipmentId: String? = null,
        fromDate: LocalDateTime? = null,
        toDate: LocalDateTime? = null,
        flagActive: Boolean? = true
    ): List<DefectInfo>

    /**
     * 생산실적 ID로 불량 정보 조회
     */
    fun getDefectInfoByProdResultId(
        site: String,
        compCd: String,
        prodResultId: String
    ): List<DefectInfo>

    /**
     * 작업지시 ID로 불량 정보 조회
     */
    fun getDefectInfoByWorkOrderId(
        site: String,
        compCd: String,
        workOrderId: String
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