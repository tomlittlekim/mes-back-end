package kr.co.imoscloud.entity.business

import jakarta.persistence.*

@Entity
@Table(name = "SHIPMENT_DETAIL")
class ShipmentDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}