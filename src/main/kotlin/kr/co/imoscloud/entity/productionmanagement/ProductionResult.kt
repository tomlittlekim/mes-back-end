package kr.co.imoscloud.entity.productionmanagement

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol
import kr.co.imoscloud.security.UserPrincipal
import java.time.LocalDateTime

@Entity
@Table(
    name = "PRODUCTION_RESULT",
    uniqueConstraints = [
        UniqueConstraint(
            name = "UK_PROD_RESULT",
            columnNames = ["SITE", "COMP_CD", "PROD_RESULT_ID"]
        )
    ]
)
class ProductionResult(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ", nullable = false)
    var id: Int? = null,

    @Column(name = "SITE", nullable = false, length = 20)
    var site: String? = null,

    @Column(name = "COMP_CD", nullable = false, length = 20)
    var compCd: String? = null,

    @Column(name = "WORK_ORDER_ID", length = 50)
    var workOrderId: String? = null,

    @Column(name = "PROD_RESULT_ID", nullable = false, length = 50)
    var prodResultId: String? = null,

    @Column(name = "PRODUCT_ID", length = 100)
    var productId: String? = null,

    @Column(name = "GOOD_QTY")
    var goodQty: Double? = null,

    @Column(name = "DEFECT_QTY")
    var defectQty: Double? = null,

    @Column(name = "PROGRESS_RATE", length = 10)
    var progressRate: String? = null,

    @Column(name = "DEFECT_RATE", length = 10)
    var defectRate: String? = null,

    @Column(name = "EQUIPMENT_ID", length = 20)
    var equipmentId: String? = null,

    @Column(name = "WAREHOUSE_ID", length = 50)
    var warehouseId: String? = null,

    @Column(name = "RESULT_INFO", columnDefinition = "TEXT")
    var resultInfo: String? = null,

    @Column(name = "DEFECT_CAUSE", columnDefinition = "TEXT")
    var defectCause: String? = null,

    @Column(name = "PROD_START_TIME")
    var prodStartTime: LocalDateTime? = null,

    @Column(name = "PROD_END_TIME")
    var prodEndTime: LocalDateTime? = null,
) : CommonCol() {
    
    // JOIN으로 가져올 createUserName 필드 (DB에 저장되지 않음)
    @Transient
    var createUserName: String? = null
    
    fun softDelete(updater: UserPrincipal) {
        this.flagActive = false
        this.updateCommonCol(updater)
    }
}