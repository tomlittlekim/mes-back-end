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
     * 모든 불량 정보 조회
     * 기본적으로 사용자의 사이트와 회사 코드에 해당하는 데이터만 조회
     * 필터 조건에 따라 결과를 필터링함
     */
    fun getAllDefectInfos(filter: DefectInfoFilter? = null): List<DefectInfo?>? {
        val currentUser = getCurrentUserPrincipal()
        
        // 항상 필터링된 결과 조회
        return defectInfoRepository.getDefectInfoByFilter(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            filter = filter
        )
    }

    /**
     * 생산 실적 ID로 불량 정보 조회
     */
    fun getDefectInfoByProdResultId(prodResultId: String): List<DefectInfo> {
        val currentUser = getCurrentUserPrincipal()
        return defectInfoRepository.getDefectInfoByProdResultId(
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

                    // 불량 유형 정보 저장 (우선순위: resultInfo > defectType > defectName)
                    resultInfo = when {
                        !input.resultInfo.isNullOrBlank() -> input.resultInfo
                        !input.defectType.isNullOrBlank() -> input.defectType
                        !input.defectName.isNullOrBlank() -> input.defectName
                        else -> "불량"
                    }

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
     * 불량 정보 소프트 삭제 (flagActive = false로 설정)
     * ProductionResultService에서 호출하는 메소드
     */
    @Transactional
    fun softDeleteDefectInfo(defectId: String): Boolean {
        try {
            val currentUser = getCurrentUserPrincipal()
            val existingDefectInfo = defectInfoRepository.findByDefectId(defectId)

            existingDefectInfo?.let {
                // flagActive를 false로 설정
                it.flagActive = false
                it.updateCommonCol(currentUser)
                defectInfoRepository.save(it)
                return true
            }

            return false
        } catch (e: Exception) {
            throw e  // 트랜잭션 롤백을 위해 예외를 다시 던짐
        }
    }
}