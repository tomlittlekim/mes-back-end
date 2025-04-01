package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.WorkOrder
import org.springframework.data.jpa.repository.JpaRepository

interface WorkOrderRepository: JpaRepository<WorkOrder, Long>, WorkOrderRepositoryCustom {
    fun findByWorkOrderId(workOrderId: String): WorkOrder?
}