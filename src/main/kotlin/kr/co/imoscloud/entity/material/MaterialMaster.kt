package kr.co.imoscloud.entity.material

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol

@Entity
@Table(
    name = "MATERIAL_MASTER",
    uniqueConstraints = [
        UniqueConstraint(
            name = "UNIQUE_SITE_COMP_SYSMTRLID",
            columnNames = ["SITE", "COMP_CD", "SYSTEM_MATERIAL_ID"]
        )
    ]
)
class MaterialMaster (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ", nullable = false)
    var seq: Int? = null,

    @Column(name = "SITE", nullable = false, length = 50)
    var site: String? = null,

    @Column(name = "COMP_CD", nullable = false, length = 50)
    var compCd: String? = null,

    @Column(name = "SYSTEM_MATERIAL_ID", nullable = false, length = 50)
    var systemMaterialId: String? = null,

    @Column(name = "MATERIAL_TYPE", length = 50)
    var materialType: String? = null,

    @Column(name = "MATERIAL_CATEGORY", length = 50)
    var materialCategory: String? = null,

    @Column(name = "USER_MATERIAL_ID", length = 20)
    var userMaterialId: String? = null,

    @Column(name = "MATERIAL_NAME", length = 100)
    var materialName: String? = null,

    @Column(name = "MATERIAL_STANDARD", length = 100)
    var materialStandard: String? = null,

    @Column(name = "UNIT", length = 20)
    var unit: String? = null,

    @Column(name = "MIN_QUANTITY")
    var minQuantity: Double? = null,

    @Column(name = "MAX_QUANTITY")
    var maxQuantity: Double? = null,

    @Column(name = "BASE_QUANTITY")
    var baseQuantity: Double? = null,

    @Column(name = "MANUFACTURER_NAME", length = 100)
    var manufacturerName: String? = null,

    @Column(name = "SUPPLIER_ID", length = 100)
    var supplierId: String? = null,

    @Column(name = "SUPPLIER_NAME", length = 100)
    var supplierName: String? = null,

//    @Column(name = "MATERIAL_STORAGE", length = 100)
//    var materialStorage: String? = null,
): CommonCol()