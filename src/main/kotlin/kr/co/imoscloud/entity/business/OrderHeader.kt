package kr.co.imoscloud.entity.business

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "ORDER_HEADER")
class OrderHeader(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    val id: Long? = null, // AutoIncrement PK

    @Column(name = "ORDER_NO", unique = true, nullable = false, length = 100)
    var orderNo: String,

    @Column(name = "ORDER_DATE", nullable = false)
    var orderDate: LocalDate? = null,

    @Column(name = "CUSTOMER_ID", length = 100, nullable = false)
    var customerId: String? = null,

    @Column(name = "TOTAL_AMOUNT")
    var totalAmount: Int? = null,

    @Column(name = "VAT_AMOUNT", length = 100)
    var vatAmount: String? = null,

    @Column(name = "FLAG_VAT_AMOUNT")
    var flagVatAmount: Boolean = false,  // 부가세 여부

    @Column(name = "FINAL_AMOUNT")
    var finalAmount: Int? = null,

    @Column(name = "DELIVERY_DATE")
    var deliveryDate: LocalDateTime? = null,

    @Column(name = "PAYMENT_METHOD", length = 100)
    var paymentMethod: String? = null,

    @Column(name = "DELIVERY_ADDR", length = 100)
    var deliveryAddr: String? = null,

    @Column(name = "REMARK", length = 100)
    var remark: String? = null
): CommonCol()