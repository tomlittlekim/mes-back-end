package kr.co.imoscloud.entity.inventory

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "INVENTORY_STATUS")
class InventoryStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ", nullable = false)
    var id: Int? = null

    @Column(name = "SITE", length = 20)
    var site: String? = null

    @Column(name = "COMP_CD", length = 20)
    var compCd: String? = null

    @Column(name = "FACTORY_ID", length = 100)
    var factoryId: String? = null

    @Column(name = "WAREHOUSE_ID", length = 50)
    var warehouseId: String? = null

    @Column(name = "SYSTEM_MATERIAL_ID", length = 50)
    var systemMaterialId: String? = null

    @Column(name = "QTY")
    var qty: Int? = null

    @Column(name = "FLAG_ACTIVE")
    var flagActive: Boolean? = null

    @Column(name = "CREATE_USER", length = 100)
    var createUser: String? = null

    @Column(name = "CREATE_DATE")
    var createDate: LocalDate? = null

    @Column(name = "UPDATE_USER", length = 100)
    var updateUser: String? = null

    @Column(name = "UPDATE_DATE")
    var updateDate: LocalDate? = null
}