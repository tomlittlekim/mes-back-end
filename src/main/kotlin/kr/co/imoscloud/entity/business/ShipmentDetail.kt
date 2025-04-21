package kr.co.imoscloud.entity.business

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol
import kr.co.imoscloud.iface.DtoCompCdBase
import java.time.LocalDate

@Entity
@Table(name = "SHIPMENT_DETAIL")
class ShipmentDetail(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    var id: Long? = null,

    @Column(name = "SITE", length = 20)
    val site: String,

    @Column(name = "COMP_CD", length = 20)
    override val compCd: String,

    @Column(name = "ORDER_NO", nullable = false, length = 100)
    var orderNo: String,

    @Column(name = "ORDER_SUB_NO", nullable = false, length = 100)
    var orderSubNo: String,

    @Column(name = "PRODUCT_WAREHOUSE_ID", length = 100)
    var productWarehouseId: String? = null,

    @Column(name = "SHIPMENT_ID", nullable = false)
    var shipmentId: Long,

    @Column(name = "SHIPMENT_DATE", nullable = false)
    var shipmentDate: LocalDate? = null,

    @Column(name = "SHIPPED_QUANTITY")
    var shippedQuantity: Int? = null,

    @Column(name = "UNSHIPPED_QUANTITY")
    var unshippedQuantity: Int? = null,

    @Column(name = "STOCK_QUANTITY")
    var stockQuantity: Int? = null,

    @Column(name = "CUMULATIVE_SHIPMENT_QUANTITY")
    var cumulativeShipmentQuantity: Int? = null,

    @Column(name = "SHIPMENT_WAREHOUSE")
    var shipmentWarehouse: String? = null,

    @Column(name = "SHIPMENT_HANDLER")
    var shipmentHandler: String? = null,

    @Column(name = "REMARK")
    var remark: String? = null

) : CommonCol(), DtoCompCdBase