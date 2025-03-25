package com.example.imosbackend.entity

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "FACTORY")
class Factory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ", nullable = false)
    var id: Int? = null

    @Column(name = "SITE", nullable = false, length = 20)
    var site: String? = null

    @Column(name = "COMP_CD", nullable = false, length = 20)
    var compCd: String? = null

    @Column(name = "FACTORY_ID", nullable = false, length = 100)
    var factoryId: String? = null

    @Column(name = "FACTORY_NAME", length = 100)
    var factoryName: String? = null

    @Column(name = "FACTORY_CODE", length = 20)
    var factoryCode: String? = null

    @Column(name = "ADDRESS", length = 200)
    var address: String? = null

    @Column(name = "TEL_NO", length = 100)
    var telNo: String? = null

    @Column(name = "OFFICER_NAME", length = 100)
    var officerName: String? = null

    @Column(name = "FLAG_ACTIVE")
    var flagActive: Boolean? = null

    @Column(name = "CREATE_USER", length = 100)
    var createUser: String? = null

    @Column(name = "CREATE_DATE")
    var createDate: LocalDate? = null

    @Column(name = "UPDATE_USER", length = 100)
    var updateUser: String? = null

    @Column(name = "UPDATE_DATE")
    var updateDate: LocalDate? = null
}

data class FactoryFilter(
    var factoryId: String,
    var factoryName: String,
    var factoryCode: String,
    var flagActive: Boolean? = null,
)