package kr.co.imoscloud.fetcher.productionmanagement

import com.netflix.graphql.dgs.*
import kr.co.imoscloud.entity.material.MaterialMaster
import kr.co.imoscloud.entity.productionmanagement.WorkOrder
import kr.co.imoscloud.model.productionmanagement.*
import kr.co.imoscloud.repository.productionmanagement.WorkOrderRepository
import kr.co.imoscloud.service.productionmanagement.ProductionPlanService
import kr.co.imoscloud.util.DateUtils
import kr.co.imoscloud.util.SecurityUtils
import org.slf4j.LoggerFactory

@DgsComponent
class ProductionPlanDataFetcher(
    private val productionPlanService: ProductionPlanService,
    private val workOrderRepository: WorkOrderRepository,
) {
    private val log = LoggerFactory.getLogger(ProductionPlanDataFetcher::class.java)

    // 생산계획 목록 조회 - DTO를 직접 GraphQL 응답으로 사용
    @DgsQuery
    fun productionPlans(@InputArgument("filter") filterInput: Map<String, Any>?): List<ProductionPlanDTO> {
        try {
            // Map으로 받은 입력값을 ProductionPlanFilter로 변환
            val filter = ProductionPlanFilter()

            filterInput?.let { input ->
                // 문자열 필드들 설정
                filter.prodPlanId = input["prodPlanId"] as? String
                filter.orderId = input["orderId"] as? String
                filter.orderDetailId = input["orderDetailId"] as? String
                filter.productId = input["productId"] as? String
                filter.productName = input["productName"] as? String
                filter.materialCategory = input["materialCategory"] as? String
                filter.shiftType = input["shiftType"] as? String

                // 계획시작일 날짜 필드 변환
                if (input.containsKey("planStartDateFrom")) {
                    val startDateFromStr = input["planStartDateFrom"] as? String
                    filter.planStartDateFrom = DateUtils.parseDate(startDateFromStr)
                }

                if (input.containsKey("planStartDateTo")) {
                    val startDateToStr = input["planStartDateTo"] as? String
                    filter.planStartDateTo = DateUtils.parseDate(startDateToStr)
                }

                // 계획종료일 날짜 필드 변환
                if (input.containsKey("planEndDateFrom")) {
                    val endDateFromStr = input["planEndDateFrom"] as? String
                    filter.planEndDateFrom = DateUtils.parseDate(endDateFromStr)
                }

                if (input.containsKey("planEndDateTo")) {
                    val endDateToStr = input["planEndDateTo"] as? String
                    filter.planEndDateTo = DateUtils.parseDate(endDateToStr)
                }

                // Boolean 필드 설정
                filter.flagActive = input["flagActive"] as? Boolean ?: true
            }

            // DTO를 직접 반환
            return productionPlanService.getProductionPlans(filter)
        } catch (e: SecurityException) {
            log.error("인증 오류: {}", e.message)
            return emptyList()
        } catch (e: Exception) {
            log.error("생산계획 목록 조회 중 오류 발생", e)
            return emptyList()
        }
    }

    // 제품 목록 조회 메서드 유지 (메뉴 마운트 시 한번에 로드하는 용도)
    @DgsQuery
    fun productMaterials(): List<MaterialMaster?> {
        try {
            return productionPlanService.getProductMaterials()
        } catch (e: Exception) {
            log.error("제품 정보 조회 중 오류 발생", e)
            return emptyList()
        }
    }

    // 생산계획 저장 (생성/수정)
    @DgsMutation
    fun saveProductionPlan(
        @InputArgument("createdRows") createdRows: List<ProductionPlanInput>? = null,
        @InputArgument("updatedRows") updatedRows: List<ProductionPlanUpdate>? = null
    ): Boolean {
        try {
            return productionPlanService.saveProductionPlan(
                createdRows = createdRows,
                updatedRows = updatedRows
            )
        } catch (e: SecurityException) {
            log.error("인증 오류: {}", e.message)
            return false
        } catch (e: Exception) {
            log.error("생산계획 저장 중 오류 발생", e)
            return false
        }
    }

    // 생산계획 삭제 (소프트 삭제로 변경)
    @DgsMutation
    fun deleteProductionPlan(
        @InputArgument("prodPlanId") prodPlanId: String
    ): Boolean {
        try {
            return productionPlanService.softDeleteProductionPlan(prodPlanId)
        } catch (e: SecurityException) {
            log.error("인증 오류: {}", e.message)
            return false
        } catch (e: Exception) {
            log.error("생산계획 삭제 중 오류 발생", e)
            return false
        }
    }

    // 생산계획에 속한 작업지시 목록 조회 (GraphQL 리졸버)
    @DgsData(parentType = "ProductionPlan", field = "workOrders")
    fun workOrders(dfe: DgsDataFetchingEnvironment): List<WorkOrder> {
        val productionPlan = dfe.getSource<ProductionPlanDTO>()
        val prodPlanId = productionPlan?.prodPlanId ?: return emptyList()

        try {
            // 사용자 정보 가져오기
            val currentUser = SecurityUtils.getCurrentUserPrincipalOrNull()
                ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

            return workOrderRepository.getWorkOrdersByProdPlanId(
                site = currentUser.getSite(),
                compCd = currentUser.compCd,
                prodPlanId = prodPlanId
            )
        } catch (e: SecurityException) {
            log.error("인증 오류: {}", e.message)
            return emptyList()
        } catch (e: Exception) {
            log.error("작업지시 목록 조회 중 오류 발생", e)
            return emptyList()
        }
    }

    @DgsQuery
    fun planVsActual(@InputArgument("filter") filterInput: PlanVsActualFilter): List<PlanVsActualGraphQLDto> {
        return productionPlanService.getPlanVsActualData(filterInput)
    }

    //planVsActual랑 같은 필터 사용
    @DgsQuery
    fun periodicProduction(@InputArgument("filter") filterInput: PlanVsActualFilter): List<PeriodicProductionResponseDto> {
        return productionPlanService.getPeriodicProduction(filterInput)
    }
}