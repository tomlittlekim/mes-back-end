package kr.co.imoscloud.entity.productionmanagement

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol
import java.time.LocalDateTime

@Entity
@Table(name = "PRODUCTION_PLAN")
class ProductionPlan : CommonCol() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ", nullable = false)
    var id: Int? = null

    @Column(name = "SITE", nullable = false, length = 20)
    var site: String? = null

    @Column(name = "COMP_CD", nullable = false, length = 20)
    var compCd: String? = null

    @Column(name = "PROD_PLAN_ID", nullable = false, length = 50)
    var prodPlanId: String? = null

    @Column(name = "ORDER_ID", length = 20)
    var orderId: String? = null

    @Column(name = "ORDER_DETAIL_ID", length = 20)
    var orderDetailId: String? = null

    @Column(name = "PRODUCT_ID", length = 100)
    var productId: String? = null

    @Column(name = "SHIFT_TYPE", length = 10)
    var shiftType: String? = null

    @Column(name = "PLAN_QTY")
    var planQty: Double? = null

    @Column(name = "PLAN_START_DATE")
    var planStartDate: LocalDateTime? = null

    @Column(name = "PLAN_END_DATE")
    var planEndDate: LocalDateTime? = null
}