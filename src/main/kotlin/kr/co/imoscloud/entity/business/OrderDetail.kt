package kr.co.imoscloud.entity.business

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol
import java.time.LocalDate

@Entity
@Table(name = "ORDER_DETAIL")
data class OrderDetail(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    val id: Long? = null,

    @Column(name = "ORDER_NO", nullable = false, length = 100)
    var orderNo: String,

    @Column(name = "ORDER_SUB_NO", length = 100)
    var orderSubNo: String? = null,

    @Column(name = "SYSTEM_MATERIAL_ID", length = 50, nullable = false)
    var systemMaterialId: String? = null,

    @Column(name = "DELIVERY_DATE")
    var deliveryDate: LocalDate? = null,

    @Column(name = "QUANTITY", nullable = false)
    var quantity: Int,

    @Column(name = "UNIT_PRICE", nullable = false)
    var unitPrice: Int,

    @Column(name = "SUPPLY_PRICE")
    var supplyPrice: Int? = null,

    @Column(name = "VAT_PRICE")
    var vatPrice: Int? = null,

    @Column(name = "TOTAL_PRICE")
    var totalPrice: Int? = null,

    @Column(name = "REMARK", length = 100)
    var remark: String? = null

) : CommonCol()