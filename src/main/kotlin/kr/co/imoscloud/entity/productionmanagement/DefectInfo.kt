package kr.co.imoscloud.entity.productionmanagement

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol
import kr.co.imoscloud.security.UserPrincipal

@Entity
@Table(name = "DEFECT_INFO")
class DefectInfo(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ", nullable = false)
    var id: Int? = null,

    @Column(name = "SITE", nullable = false, length = 20)
    var site: String? = null,

    @Column(name = "COMP_CD", nullable = false, length = 20)
    var compCd: String? = null,

    @Column(name = "PROD_RESULT_ID", nullable = false, length = 50)
    var prodResultId: String? = null,

    @Column(name = "DEFECT_ID", nullable = false, length = 50)
    var defectId: String? = null,

    @Column(name = "PRODUCT_ID", length = 50)
    var productId: String? = null,

    @Column(name = "DEFECT_QTY")
    var defectQty: Double? = null,

    @Column(name = "RESULT_INFO", columnDefinition = "TEXT")
    var resultInfo: String? = null,

    @Column(name = "STATE", length = 20)
    var state: String? = null,

    @Column(name = "DEFECT_CAUSE", columnDefinition = "TEXT")
    var defectCause: String? = null,
) : CommonCol() {
    // CODE 테이블과 JOIN하여 가져오는 불량원인명 (데이터베이스 컬럼이 아님)
    @Transient
    var defectCauseName: String? = null

    // ProductionResult 테이블과 JOIN하여 가져오는 설비ID (데이터베이스 컬럼이 아님)
    @Transient
    var equipmentId: String? = null

    // User 테이블과 JOIN하여 가져오는 생성자명 (데이터베이스 컬럼이 아님)
    @Transient
    var createUserName: String? = null

    fun softDelete(updater: UserPrincipal) {
        this.flagActive = false
        this.updateCommonCol(updater)
    }
}