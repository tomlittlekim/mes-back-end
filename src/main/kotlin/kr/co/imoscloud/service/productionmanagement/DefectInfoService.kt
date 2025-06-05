package kr.co.imoscloud.service.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.DefectInfo
import kr.co.imoscloud.model.productionmanagement.DefectInfoFilter
import kr.co.imoscloud.model.productionmanagement.DefectInfoInput
import kr.co.imoscloud.repository.productionmanagement.DefectInfoRepository
import kr.co.imoscloud.repository.productionmanagement.ProductionResultRepository
import kr.co.imoscloud.util.SecurityUtils.getCurrentUserPrincipal
import kr.co.imoscloud.util.DateUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 불량 정보 서비스
 * - 불량 정보 조회, 등록 기능을 제공
 */
@Service
class DefectInfoService(
    private val defectInfoRepository: DefectInfoRepository,
) {
    /**
     * 모든 불량 정보 조회 (CODE 테이블과 JOIN하여 defectCauseName 포함)
     * 기본적으로 사용자의 사이트와 회사 코드에 해당하는 데이터만 조회
     * 필터 조건에 따라 결과를 필터링함
     */
    fun getAllDefectInfos(filter: DefectInfoFilter? = null): List<DefectInfo?>? {
        val currentUser = getCurrentUserPrincipal()
        
        // 필터 조건 처리 - 기존 DateUtils 함수들을 조합하여 사용
        val (fromDate, toDate) = parseFilterDateRange(filter?.fromDate, filter?.toDate)
        
        return defectInfoRepository.getDefectInfosWithUserName(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            defectId = filter?.defectId,
            prodResultId = filter?.prodResultId,
            productId = filter?.productId,
            equipmentId = filter?.equipmentId,
            fromDate = fromDate,
            toDate = toDate
        )
    }

    /**
     * 불량 정보 필터링을 위한 날짜 범위 처리 (private 함수)
     * 시작 날짜는 해당 날짜의 00:00으로, 종료 날짜는 다음 날의 00:00으로 설정
     */
    private fun parseFilterDateRange(fromDateStr: String?, toDateStr: String?): Pair<LocalDateTime?, LocalDateTime?> {
        val fromDate = fromDateStr?.let { 
            DateUtils.parseDateTimeExact(it) // 기존 DateUtils 함수 사용
        }
        
        val toDate = toDateStr?.let { 
            DateUtils.parseDate(it)?.let { date ->
                // 종료 날짜는 다음 날 00:00으로 설정 (특정 비즈니스 로직)
                LocalDateTime.of(date.plusDays(1), LocalTime.MIN)
            } ?: DateUtils.parseDateTimeExact(it) // 시간이 포함된 경우
        }
        
        return Pair(fromDate, toDate)
    }

    /**
     * 생산 실적 ID로 불량 정보 조회 (CODE 테이블과 JOIN하여 defectCauseName 포함, ProductionResult와 JOIN하여 equipmentId 포함)
     */
    fun getDefectInfoByProdResultId(prodResultId: String): List<DefectInfo> {
        val currentUser = getCurrentUserPrincipal()
        return defectInfoRepository.getDefectInfosByProdResultIdWithUserName(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            prodResultId = prodResultId
        )
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

            // 모든 DefectInfo 엔티티를 먼저 생성
            val defectInfoEntities = validatedDefectInfos.mapIndexed { index, input ->
                // 불량정보 ID 생성 - UUID 사용으로 더 안전하게 변경
                val uuid = java.util.UUID.randomUUID().toString()
                val defectId = "DF-${uuid.substring(0, 8)}-$index"

                DefectInfo().apply {
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
            }

            // 배치 저장으로 DB 통신 횟수 최적화
            defectInfoRepository.saveAll(defectInfoEntities)

            return true
        } catch (e: Exception) {
            throw e  // 트랜잭션 롤백을 위해 예외를 다시 던짐
        }
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