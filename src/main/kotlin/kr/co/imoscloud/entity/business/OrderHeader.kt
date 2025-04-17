package kr.co.imoscloud.entity.business

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol
import kr.co.imoscloud.iface.DtoCompCdBase
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "ORDER_HEADER")
class OrderHeader(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    val id: Long? = null, // AutoIncrement PK,

    @Column(name = "SITE", length = 20)
    val site: String,

    @Column(name = "COMP_CD", length = 20)
    override val compCd: String,

    @Column(name = "ORDER_NO", unique = true, nullable = false, length = 100)
    var orderNo: String,

    @Column(name = "ORDER_DATE", nullable = false)
    var orderDate: LocalDate? = null,

    @Column(name = "CUSTOMER_ID", length = 100, nullable = false)
    var customerId: String? = null,

    @Column(name = "ORDERER_ID", length = 100, nullable = false)
    var ordererId: String? = null,

    @Column(name = "TOTAL_AMOUNT")
    var totalAmount: Int? = 0,

    @Column(name = "VAT_AMOUNT", length = 100)
    var vatAmount: Int? = 0,

    @Column(name = "FLAG_VAT_AMOUNT")
    var flagVatAmount: Boolean = false,  // 부가세 여부

    @Column(name = "FINAL_AMOUNT")
    var finalAmount: Int? = 0,

    @Column(name = "DELIVERY_DATE")
    var deliveryDate: LocalDateTime? = null,

    @Column(name = "PAYMENT_METHOD", length = 100)
    var paymentMethod: String? = null,

    @Column(name = "DELIVERY_ADDR", length = 100)
    var deliveryAddr: String? = null,

    @Column(name = "REMARK", length = 100)
    var remark: String? = null
): CommonCol(), DtoCompCdBase