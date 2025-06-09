package kr.co.imoscloud.entity.productionmanagement

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol
import kr.co.imoscloud.security.UserPrincipal

@Entity
@Table(
    name = "WORK_ORDER",
    uniqueConstraints = [
        UniqueConstraint(
            name = "UK_WORK_ORDER_SITE_COMP_ORDER",
            columnNames = ["SITE", "COMP_CD", "WORK_ORDER_ID"]
        )
    ]
)
class WorkOrder(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ", nullable = false)
    var id: Int? = null,

    @Column(name = "SITE", nullable = false, length = 20)
    var site: String? = null,

    @Column(name = "COMP_CD", nullable = false, length = 20)
    var compCd: String? = null,

    @Column(name = "WORK_ORDER_ID", nullable = false, length = 50)
    var workOrderId: String? = null,

    @Column(name = "PROD_PLAN_ID", length = 50)
    var prodPlanId: String? = null,

    @Column(name = "PRODUCT_ID", length = 20)
    var productId: String? = null,

    @Column(name = "ORDER_QTY")
    var orderQty: Double? = null,

    @Column(name = "SHIFT_TYPE", length = 10)
    var shiftType: String? = null,

    @Column(name = "STATE", length = 20)
    var state: String? = null,
) : CommonCol() {
    fun start(updater: UserPrincipal) {
        this.state = "IN_PROGRESS"
        this.updateCommonCol(updater)
    }

    fun complete(updater: UserPrincipal) {
        this.state = "COMPLETED"
        this.updateCommonCol(updater)
    }

    fun softDelete(updater: UserPrincipal) {
        this.flagActive = false
        this.updateCommonCol(updater)
    }
}