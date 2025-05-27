package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.WorkOrder
import org.springframework.data.jpa.repository.JpaRepository

interface WorkOrderRepository: JpaRepository<WorkOrder, Long>, WorkOrderRepositoryCustom {
    fun findByWorkOrderId(workOrderId: String): WorkOrder?

    // Unique Key 필드를 모두 사용하는 메서드 추가
    fun findBySiteAndCompCdAndWorkOrderId(site: String, compCd: String, workOrderId: String): WorkOrder?

    // 특정 생산계획에 활성화된 작업지시가 있는지 확인하는 JPA Query Method
    fun existsBySiteAndCompCdAndProdPlanIdAndFlagActive(
        site: String, 
        compCd: String, 
        prodPlanId: String, 
        flagActive: Boolean
    ): Boolean
}