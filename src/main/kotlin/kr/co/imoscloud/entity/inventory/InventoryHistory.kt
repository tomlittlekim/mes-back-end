package kr.co.imoscloud.entity.inventory

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "INVENTORY_HISTORY",
    uniqueConstraints = [
        UniqueConstraint(
            name = "UNIQUE_INVENTORY_LOG",
            columnNames = ["SEQ", "SITE", "COMP_CD"]
        )
    ]
)
class InventoryHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ", nullable = false)
    var id: Int? = null

    @Column(name = "SITE", length = 20)
    var site: String? = null

    @Column(name = "COMP_CD", length = 20)
    var compCd: String? = null

    @Column(name = "FACTORY_NAME", length = 100)
    var factoryName: String? = null

    @Column(name = "WAREHOUSE_NAME", length = 100)
    var warehouseName: String? = null

    @Column(name = "IN_OUT_TYPE", length = 20)
    var inOutType: String? = null

    @Column(name = "SUPPLIER_NAME", length = 100)
    var supplierName: String? = null

    @Column(name = "MANUFACTURER_NAME", length = 100)
    var manufacturerName: String? = null

    @Column(name = "MATERIAL_NAME", length = 100)
    var materialName: String? = null

    @Column(name = "UNIT", length = 20)
    var unit: String? = null

    @Column(name = "PREV_QTY")
    var prevQty: Double? = null

    @Column(name = "CHANGE_QTY")
    var changeQty: Double? = null

    @Column(name = "CURRENT_QTY")
    var currentQty: Double? = null

    @Column(name = "REASON", length = 500)
    var reason: String? = null

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