package kr.co.imoscloud.entity.business

import jakarta.persistence.*

@Entity
@Table(name = "ORDER_DETAIL")
class OrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}