package kr.co.imoscloud.service.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.DefectInfo
import kr.co.imoscloud.model.productionmanagement.DefectInfoFilter
import kr.co.imoscloud.model.productionmanagement.DefectInfoInput
import kr.co.imoscloud.repository.productionmanagement.DefectInfoRepository
import kr.co.imoscloud.repository.productionmanagement.ProductionResultRepository
import kr.co.imoscloud.util.SecurityUtils.getCurrentUserPrincipal
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 불량 정보 서비스
 * - 불량 정보 조회, 등록 기능을 제공
 */
@Service
class DefectInfoService(
    private val defectInfoRepository: DefectInfoRepository,
    private val productionResultRepository: ProductionResultRepository
) {
    /**
     * 모든 불량 정보 조회 (CODE 테이블과 JOIN하여 defectCauseName 포함)
     * 기본적으로 사용자의 사이트와 회사 코드에 해당하는 데이터만 조회
     * 필터 조건에 따라 결과를 필터링함
     */
    fun getAllDefectInfos(filter: DefectInfoFilter? = null): List<DefectInfo?>? {
        val currentUser = getCurrentUserPrincipal()
        
        // 필터 조건 처리
        val fromDate = filter?.fromDate?.let { fromDateStr ->
            try {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val fromDate = java.time.LocalDate.parse(fromDateStr, formatter)
                java.time.LocalDateTime.of(fromDate, java.time.LocalTime.MIN)
            } catch (e: Exception) {
                try {
                    val formatterWithTime = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                    java.time.LocalDateTime.parse(fromDateStr, formatterWithTime)
                } catch (e: Exception) {
                    null
                }
            }
        }
        
        val toDate = filter?.toDate?.let { toDateStr ->
            try {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val toDate = java.time.LocalDate.parse(toDateStr, formatter)
                val nextDay = toDate.plusDays(1)
                java.time.LocalDateTime.of(nextDay, java.time.LocalTime.MIN)
            } catch (e: Exception) {
                try {
                    val formatterWithTime = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                    java.time.LocalDateTime.parse(toDateStr, formatterWithTime)
                } catch (e: Exception) {
                    null
                }
            }
        }
        
        val results = defectInfoRepository.findDefectInfoWithCauseNameByFilter(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            defectId = filter?.defectId,
            prodResultId = filter?.prodResultId,
            productId = filter?.productId,
            equipmentId = filter?.equipmentId,
            fromDate = fromDate,
            toDate = toDate
        )
        
        return results.map { result ->
            val defectInfo = result[0] as DefectInfo
            val defectCauseName = result[1] as String?
            val equipmentId = result[2] as String?
            
            defectInfo.apply {
                this.defectCauseName = defectCauseName
                this.equipmentId = equipmentId
            }
        }
    }

    /**
     * 생산 실적 ID로 불량 정보 조회 (CODE 테이블과 JOIN하여 defectCauseName 포함, ProductionResult와 JOIN하여 equipmentId 포함)
     */
    fun getDefectInfoByProdResultId(prodResultId: String): List<DefectInfo> {
        val currentUser = getCurrentUserPrincipal()
        val results = defectInfoRepository.findDefectInfoWithCauseNameByProdResultId(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            prodResultId = prodResultId
        )
        
        return results.map { result ->
            val defectInfo = result[0] as DefectInfo
            val defectCauseName = result[1] as String?
            val equipmentId = result[2] as String?
            
            defectInfo.apply {
                this.defectCauseName = defectCauseName
                this.equipmentId = equipmentId
            }
        }
    }

    /**
     * 일자별 불량 통계용 조회 (CODE 테이블과 JOIN하여 defectCauseName 포함, ProductionResult와 JOIN하여 equipmentId 포함)
     */
    fun getDefectInfoForStats(
        fromDate: java.time.LocalDateTime,
        toDate: java.time.LocalDateTime
    ): List<DefectInfo> {
        val currentUser = getCurrentUserPrincipal()
        val results = defectInfoRepository.findDefectInfoWithCauseNameForStats(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            fromDate = fromDate,
            toDate = toDate
        )
        
        return results.map { result ->
            val defectInfo = result[0] as DefectInfo
            val defectCauseName = result[1] as String?
            val equipmentId = result[2] as String?
            
            defectInfo.apply {
                this.defectCauseName = defectCauseName
                this.equipmentId = equipmentId
            }
        }
    }

    /**
     * 생산 실적 등록 시 불량 정보 일괄 등록
     * ProductionResultService에서 호출하는 메소드
     */
    @Transactional
    fun saveDefectInfoForProductionResult(
        prodResultId: String,
        defectInputs: List<DefectInfoInput>
    ): Boolean {
        try {
            val currentUser = getCurrentUserPrincipal()

            // 입력값 유효성 검사
            if (prodResultId.isBlank()) {
                return false
            }

            // 모든 불량정보에 prodResultId 설정 보장
            val validatedDefectInfos = defectInputs.map { input ->
                // prodResultId가 없거나 다른 경우 수정
                if (input.prodResultId.isNullOrBlank() || input.prodResultId != prodResultId) {
                    input.copy(prodResultId = prodResultId)
                } else {
                    input
                }
            }

            validatedDefectInfos.forEachIndexed { index, input ->
                // 불량정보 ID 생성 - UUID 사용으로 더 안전하게 변경
                val uuid = java.util.UUID.randomUUID().toString()
                val defectId = "DF-${uuid.substring(0, 8)}-$index"

                val newDefectInfo = DefectInfo().apply {
                    site = currentUser.getSite()
                    compCd = currentUser.compCd
                    this.prodResultId = prodResultId
                    this.defectId = defectId
                    productId = input.productId
                    defectQty = input.defectQty

                    // 불량 유형 정보 저장 - 사용자가 입력한 resultInfo 값만 저장
                    resultInfo = input.resultInfo

                    // 상태 정보
                    state = input.state ?: "NEW"

                    // 불량 원인 저장 (우선순위: defectCause > defectReason)
                    defectCause = when {
                        !input.defectCause.isNullOrBlank() -> input.defectCause
                        !input.defectReason.isNullOrBlank() -> input.defectReason
                        else -> null
                    }

                    // 활성화 상태 및 생성 정보
                    flagActive = true
                    createCommonCol(currentUser)
                }

                defectInfoRepository.save(newDefectInfo)
            }

            return true
        } catch (e: Exception) {
            throw e  // 트랜잭션 롤백을 위해 예외를 다시 던짐
        }
    }

    /**
     * 불량정보 저장 (단순 save)
     */
    @Transactional
    fun saveDefectInfo(defectInfo: DefectInfo): DefectInfo {
        return defectInfoRepository.save(defectInfo)
    }

    /**
     * 불량정보 배치 저장 (saveAll)
     */
    @Transactional
    fun saveAllDefectInfos(defectInfos: List<DefectInfo>): List<DefectInfo> {
        return defectInfoRepository.saveAll(defectInfos)
    }

    /**
     * 다중 생산실적 ID로 불량정보 배치 소프트 삭제 (saveAll 방식 - 안전하고 디버깅 용이)
     */
    @Transactional
    fun batchSoftDeleteDefectInfosByProdResultIds(prodResultIds: List<String>): Int {
        val currentUser = getCurrentUserPrincipal()
        
        // 1. 연관된 모든 불량정보 조회
        val allDefectInfos = defectInfoRepository.getDefectInfosByProdResultIds(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            prodResultIds = prodResultIds
        )
        
        // 2. 각 불량정보 소프트 삭제 처리
        allDefectInfos.forEach { defectInfo ->
            defectInfo.softDelete(currentUser)
        }
        
        // 3. 배치 저장
        val savedDefectInfos = defectInfoRepository.saveAll(allDefectInfos)
        return savedDefectInfos.size
    }


}