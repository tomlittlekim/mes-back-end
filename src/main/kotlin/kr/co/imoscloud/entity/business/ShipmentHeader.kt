package kr.co.imoscloud.entity.business

import jakarta.persistence.*

@Entity
@Table(name = "SHIPMENT_HEADER")
class ShipmentHeader {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}