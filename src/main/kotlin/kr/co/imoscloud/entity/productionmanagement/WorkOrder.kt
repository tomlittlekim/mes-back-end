package kr.co.imoscloud.entity.productionmanagement

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol

@Entity
@Table(name = "WORK_ORDER")
class WorkOrder : CommonCol() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ", nullable = false)
    var id: Int? = null

    @Column(name = "SITE", nullable = false, length = 20)
    var site: String? = null

    @Column(name = "COMP_CD", nullable = false, length = 20)
    var compCd: String? = null

    @Column(name = "WORK_ORDER_ID", nullable = false, length = 50)
    var workOrderId: String? = null

    @Column(name = "PROD_PLAN_ID", length = 50)
    var prodPlanId: String? = null

    @Column(name = "PRODUCT_ID", length = 20)
    var productId: String? = null

    @Column(name = "ORDER_QTY")
    var orderQty: Double? = null

    @Column(name = "SHIFT_TYPE", length = 10)
    var shiftType: String? = null

    @Column(name = "STATE", length = 20)
    var state: String? = null
}