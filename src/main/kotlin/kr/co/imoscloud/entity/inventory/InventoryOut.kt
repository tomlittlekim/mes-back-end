package kr.co.imoscloud.entity.inventory

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "INVENTORY_OUT")
class InventoryOut {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ", nullable = false)
    var id: Int? = null

    @Column(name = "SITE", length = 20)
    var site: String? = null

    @Column(name = "COMP_CD", length = 20)
    var compCd: String? = null

    @Column(name = "OUT_MANAGEMENT_ID", length = 50)
    var outManagementId: String? = null

    @Column(name = "OUT_INVENTORY_ID", length = 50)
    var outInventoryId: String? = null

    @Column(name = "SYSTEM_MATERIAL_ID", length = 50)
    var systemMaterialId: String? = null

    @Column(name = "OUT_TYPE", length = 20)
    var outType: String? = null

    @Column(name = "QTY")
    var qty: Double? = null

    @Column(name = "UNIT_PRICE")
    var unitPrice: Int? = null

    @Column(name = "UNIT_VAT")
    var unitVat: Int? = null

    @Column(name = "TOTAL_PRICE")
    var totalPrice: Int? = null

    @Column(name = "FLAG_ACTIVE")
    var flagActive: Boolean? = null

    @Column(name = "CREATE_USER", length = 100)
    var createUser: String? = null

    @Column(name = "CREATE_DATE")
    var createDate: LocalDateTime? = null

    @Column(name = "UPDATE_USER", length = 100)
    var updateUser: String? = null

    @Column(name = "UPDATE_DATE")
    var updateDate: LocalDateTime? = null
}