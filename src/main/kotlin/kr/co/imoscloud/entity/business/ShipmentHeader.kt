package kr.co.imoscloud.entity.business

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol
import kr.co.imoscloud.iface.DtoCompCdBase

@Entity
@Table(
    name = "SHIPMENT_HEADER",
    uniqueConstraints = [
        UniqueConstraint(
            name = "UK_ORDER_DETAIL_SITE_COMP_ORDER",
            columnNames = ["SITE", "COMP_CD", "ORDER_NO"]
        )
    ]
)
class ShipmentHeader(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SHIPMENT_ID")
    var shipmentId: Long? = null,

    @Column(name = "SITE", unique = true, length = 20)
    val site: String,

    @Column(name = "COMP_CD", unique = true, length = 20)
    override val compCd: String,

    @Column(name = "ORDER_NO", unique = true, nullable = false)
    var orderNo: String,

    @Column(name = "SHIPMENT_STATUS")
    var shipmentStatus: String? = null,

    @Column(name = "SHIPPED_QUANTITY")
    var shippedQuantity: Double? = null,

    @Column(name = "UNSHIPPED_QUANTITY")
    var unshippedQuantity: Double? = null,

    @Column(name = "REMARK")
    var remark: String? = null,

) : CommonCol(), DtoCompCdBase