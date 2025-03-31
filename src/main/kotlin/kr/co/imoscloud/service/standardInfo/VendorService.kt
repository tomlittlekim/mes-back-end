package kr.co.imoscloud.service.standardInfo

import kr.co.imoscloud.entity.standardInfo.Vendor
import kr.co.imoscloud.fetcher.standardInfo.VendorFilter
import kr.co.imoscloud.fetcher.standardInfo.VendorInput
import kr.co.imoscloud.fetcher.standardInfo.VendorUpdate
import kr.co.imoscloud.repository.VendorRep
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class VendorService(
    val vendorRep: VendorRep
) {

    fun getVendors(vendorFilter: VendorFilter): List<VendorResponse?> {
        val vendorList = vendorRep.getVendorList(
            site = "imos",
            compCd = "eightPin",
            vendorId = vendorFilter.vendorId,
            vendorName = vendorFilter.vendorName,
            ceoName = vendorFilter.ceoName,
            businessType = vendorFilter.businessType,
            flagActive = vendorFilter.flagActive?.let { it == "Y" }
        )

        return vendorList.map {
            VendorResponse(
                vendorId = it?.vendorId ?: throw Exception("vendorId가 존재하지 않습니다."),
                vendorName = it.vendorName?: throw Exception("거래처명이 존재하지 않습니다."),
                vendorType = it.vendorType,
                businessType = it.businessType,
                ceoName = it.ceoName,
                businessRegNo = it.businessRegNo ?: throw Exception(" 사업자 번호가 존재하지 않습니다."),
                address = it.address,
                telNo = it.telNo,
                flagActive = if (it.flagActive == true) "Y" else "N",
                createUser = it.createUser,
                createDate = it.createDate.toString(),
                updateUser = it.updateUser,
                updateDate = it.updateDate.toString(),
            )
        }
    }

    fun saveVendor(createdRows:List<VendorInput?>, updatedRows:List<VendorUpdate?>){
        createdRows.filterNotNull().takeIf { it.isNotEmpty() }?.let { createVendor(it) }
        updatedRows.filterNotNull().takeIf { it.isNotEmpty() }?.let { updateVendor(it) }
    }

    fun createVendor(createdRows:List<VendorInput>){
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

        val vendorList = createdRows.map{
            Vendor(
                site = "imos",
                compCd = "eightPin",
                vendorId = "V" + LocalDateTime.now().format(formatter) +
                        System.nanoTime().toString().takeLast(3),
                vendorName = it.vendorName,
                vendorType = it.vendorType,
                businessRegNo = it.businessRegNo,
                ceoName = it.ceoName,
                businessType = it.businessType,
                address = it.address,
                telNo = it.telNo,
                flagActive = it.flagActive.equals("Y" )
            )
        }

        vendorRep.saveAll(vendorList)

    }

    fun updateVendor(updatedRows:List<VendorUpdate>){
        val vendorListIds = updatedRows.map {
            it.vendorId
        }

        val vendorList = vendorRep.getVendorListByIds(
            site = "imos",
            compCd = "eightPin",
            vendorIds = vendorListIds
        )

        val updateList = vendorList.associateBy { it?.vendorId }

        updatedRows.forEach{ x->
            val vendorId = x.vendorId
            val vendor = updateList[vendorId]

            vendor?.let{
                it.vendorName = x.vendorName
                it.vendorType = x.vendorType
                it.businessRegNo = x.businessRegNo
                it.ceoName = x.ceoName
                it.businessType = x.businessType
                it.address = x.address
                it.telNo = x.telNo
                it.flagActive = x.flagActive.equals("Y" )
            }
        }

        vendorRep.saveAll(vendorList)
    }

}

data class VendorResponse(
    val vendorId: String,
    val vendorName: String,
    val vendorType: String? = null,
    val businessRegNo: String,
    val ceoName: String ?= null,
    val businessType: String ?= null,
    val address: String ?= null,
    val telNo: String ?= null,
    val flagActive:String,
    val createUser:String ?= null,
    val createDate:String ?= null,
    val updateUser:String ?= null,
    val updateDate:String ?= null,
)