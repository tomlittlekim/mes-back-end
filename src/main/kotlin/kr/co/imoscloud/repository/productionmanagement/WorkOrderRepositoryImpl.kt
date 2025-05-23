package kr.co.imoscloud.repository.productionmanagement

import com.querydsl.jpa.impl.JPAQueryFactory
import kr.co.imoscloud.entity.productionmanagement.QWorkOrder
import kr.co.imoscloud.entity.productionmanagement.WorkOrder
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport

class WorkOrderRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : WorkOrderRepositoryCustom, QuerydslRepositorySupport(WorkOrder::class.java) {

    override fun getWorkOrdersByProdPlanId(
        site: String,
        compCd: String,
        prodPlanId: String
    ): List<WorkOrder> {
        val workOrder = QWorkOrder.workOrder

        val query = queryFactory
            .selectFrom(workOrder)
            .where(
                workOrder.site.eq(site),
                workOrder.compCd.eq(compCd),
                workOrder.prodPlanId.eq(prodPlanId),
                workOrder.flagActive.eq(true)
            )
            .orderBy(workOrder.createDate.desc())

        return query.fetch()
    }

    override fun getWorkOrderList(
        site: String,
        compCd: String,
        workOrderId: String?,
        prodPlanId: String?,
        productId: String?,
        shiftType: String?,
        state: String?,
        flagActive: Boolean?
    ): List<WorkOrder> {
        val workOrder = QWorkOrder.workOrder

        val query = queryFactory
            .selectFrom(workOrder)
            .where(
                workOrder.site.eq(site),
                workOrder.compCd.eq(compCd)
            )

        // workOrderId 필터링
        workOrderId?.let {
            if (it.isNotBlank()) {
                query.where(workOrder.workOrderId.like("%$it%"))
            }
        }

        // prodPlanId 필터링
        prodPlanId?.let {
            if (it.isNotBlank()) {
                query.where(workOrder.prodPlanId.like("%$it%"))
            }
        }

        // productId 필터링
        productId?.let {
            if (it.isNotBlank()) {
                query.where(workOrder.productId.like("%$it%"))
            }
        }

        // shiftType 필터링
        shiftType?.let {
            if (it.isNotBlank()) {
                query.where(workOrder.shiftType.eq(it))
            }
        }

        // state 필터링
        state?.let {
            if (it.isNotBlank()) {
                query.where(workOrder.state.eq(it))
            }
        }

        // flagActive 필터링
        flagActive?.let {
            query.where(workOrder.flagActive.eq(it))
        }

        return query.fetch()
    }
}