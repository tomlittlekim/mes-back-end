package kr.co.imoscloud.entity.business

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol
import kr.co.imoscloud.iface.DtoCompCdBase
import java.time.LocalDate

@Entity
@Table(
    name = "ORDER_DETAIL",
    uniqueConstraints = [
        UniqueConstraint(
            name = "UK_ORDER_DETAIL_SITE_COMP_ORDER",
            columnNames = ["SITE", "COMP_CD", "ORDER_NO", "ORDER_SUB_NO"]
        )
    ]
)
data class OrderDetail(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    val id: Long? = null,

    @Column(name = "SITE", unique = true, nullable = false, length = 20)
    val site: String,

    @Column(name = "COMP_CD", unique = true, nullable = false, length = 20)
    override val compCd: String,

    @Column(name = "ORDER_NO", unique = true, nullable = false, length = 100)
    var orderNo: String,

    @Column(name = "ORDER_SUB_NO", unique = true, length = 100)
    var orderSubNo: String? = null,

    @Column(name = "SYSTEM_MATERIAL_ID", length = 50, nullable = false)
    var systemMaterialId: String? = null,

    @Column(name = "DELIVERY_DATE")
    var deliveryDate: LocalDate? = null,

    @Column(name = "QUANTITY", nullable = false)
    var quantity: Double,

    @Column(name = "UNIT_PRICE", nullable = false)
    var unitPrice: Int,

    @Column(name = "DISCOUNT_AMOUNT")
    var discountedAmount: Int? = null,

    @Column(name = "SUPPLY_PRICE")
    var supplyPrice: Int? = null,

    @Column(name = "VAT_PRICE")
    var vatPrice: Int? = null,

    @Column(name = "TOTAL_PRICE")
    var totalPrice: Int? = null,

    @Column(name = "REMARK", length = 100)
    var remark: String? = null

) : CommonCol(), DtoCompCdBase