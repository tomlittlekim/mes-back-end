package kr.co.imoscloud.entity.standardInfo

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol

@Entity
@Table(name = "WAREHOUSE")
class Warehouse (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ", nullable = false)
    var id: Int? = null,

    @Column(name = "SITE", nullable = false, length = 20)
    var site: String? = null,

    @Column(name = "COMP_CD", nullable = false, length = 20)
    var compCd: String? = null,

    @Column(name = "FACTORY_ID", nullable = false, length = 50)
    var factoryId: String? = null,

    @Column(name = "WAREHOUSE_ID", nullable = false, length = 50)
    var warehouseId: String? = null,

    @Column(name = "WAREHOUSE_NAME", nullable = false, length = 50)
    var warehouseName: String? = null,

    @Column(name = "WAREHOUSE_TYPE", length = 20)
    var warehouseType: String? = null
):CommonCol()