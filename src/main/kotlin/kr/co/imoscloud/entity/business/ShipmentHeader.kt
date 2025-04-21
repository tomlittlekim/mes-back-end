package kr.co.imoscloud.entity.business

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol
import kr.co.imoscloud.iface.DtoCompCdBase

@Entity
@Table(name = "SHIPMENT_HEADER")
class ShipmentHeader(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SHIPMENT_ID")
    var shipmentId: Long? = null,

    @Column(name = "SITE", length = 20)
    val site: String,

    @Column(name = "COMP_CD", length = 20)
    override val compCd: String,

    @Column(name = "ORDER_NO", unique = true, nullable = false)
    var orderNo: String,

    @Column(name = "CUSTOMER_ID", length = 100, nullable = false)
    var customerId: String? = null,

    @Column(name = "SHIPMENT_STATUS")
    var shipmentStatus: String? = null,

    @Column(name = "SHIPPED_QUANTITY")
    var shippedQuantity: Int? = null,

    @Column(name = "UNSHIPPED_QUANTITY")
    var unshippedQuantity: Int? = null,

    @Column(name = "REMARK")
    var remark: String? = null,

) : CommonCol(), DtoCompCdBase