package kr.co.imoscloud.service.standardInfo

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.standardInfo.Vendor
import kr.co.imoscloud.exception.vendor.NotExistBusinessRegNoException
import kr.co.imoscloud.exception.vendor.NotExistVendorIdException
import kr.co.imoscloud.exception.vendor.NotExistVendorNameException
import kr.co.imoscloud.fetcher.standardInfo.VendorFilter
import kr.co.imoscloud.fetcher.standardInfo.VendorInput
import kr.co.imoscloud.fetcher.standardInfo.VendorUpdate
import kr.co.imoscloud.repository.VendorRep
import kr.co.imoscloud.security.UserPrincipal
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class VendorService(
    val vendorRep: VendorRep
) {

    fun getVendors(vendorFilter: VendorFilter): List<VendorResponse?> {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        val vendorList = vendorRep.getVendorList(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
            vendorId = vendorFilter.vendorId,
            vendorName = vendorFilter.vendorName,
            ceoName = vendorFilter.ceoName,
            businessRegNo = vendorFilter.businessRegNo
        )

        return vendorList.map {
            VendorResponse(
                vendorId = it?.vendorId ?: throw NotExistVendorIdException(),
                vendorName = it.vendorName?: throw NotExistVendorNameException(),
                vendorType = it.vendorType,
                businessType = it.businessType,
                ceoName = it.ceoName,
                businessRegNo = it.businessRegNo ?: throw NotExistBusinessRegNoException(),
                address = it.address,
                telNo = it.telNo,
//                flagActive = if (it.flagActive == true) "Y" else "N",
                createUser = it.createUser,
                createDate = it.createDate.toString().replace("T", " "),
                updateUser = it.updateUser,
                updateDate = it.updateDate.toString().replace("T", " "),
            )
        }
    }

    @Transactional
    fun saveVendor(createdRows:List<VendorInput?>, updatedRows:List<VendorUpdate?>){
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        createdRows.filterNotNull().takeIf { it.isNotEmpty() }?.let { createVendor(it, userPrincipal) }
        updatedRows.filterNotNull().takeIf { it.isNotEmpty() }?.let { updateVendor(it, userPrincipal) }
    }

    fun createVendor(createdRows:List<VendorInput>,userPrincipal: UserPrincipal){
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

        val vendorList = createdRows.map{
            Vendor(
                site = userPrincipal.getSite(),
                compCd = userPrincipal.compCd,
                vendorId = "V" + LocalDateTime.now().format(formatter) +
                        System.nanoTime().toString().takeLast(3),
                vendorName = it.vendorName,
                vendorType = it.vendorType,
                businessRegNo = it.businessRegNo,
                ceoName = it.ceoName,
                businessType = it.businessType,
                address = it.address,
                telNo = it.telNo,
            ).apply {
                createCommonCol(userPrincipal)
            }
        }

        vendorRep.saveAll(vendorList)

    }

    fun updateVendor(updatedRows:List<VendorUpdate>,userPrincipal: UserPrincipal){
        val vendorListIds = updatedRows.map {
            it.vendorId
        }

        val vendorList = vendorRep.getVendorListByIds(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
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
                it.updateCommonCol(userPrincipal)
            }
        }

        vendorRep.saveAll(vendorList)
    }

    fun deleteVendor(vendorIds: List<String>): Boolean {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        return vendorRep.deleteByVendorId(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
            vendorIds = vendorIds,
            updateUser = userPrincipal.loginId
        ) > 0
    }

    fun getVendorsBySameCompany(): List<Vendor> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        return vendorRep.findAllBySiteAndCompCd(loginUser.getSite(), loginUser.compCd);
    }

    fun getVendorsByType(vendorType: String): List<VendorDropdownResponse> {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()
        
        val vendorList = vendorRep.getVendorsByType(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
            vendorType = vendorType
        )

        return vendorList.map {
            VendorDropdownResponse(
                vendorId = it?.vendorId ?: throw NotExistVendorIdException(),
                vendorName = it.vendorName ?: throw NotExistVendorNameException(),
            )
        }
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
//    val flagActive:String,
    val createUser:String ?= null,
    val createDate:String ?= null,
    val updateUser:String ?= null,
    val updateDate:String ?= null,
)

data class VendorDropdownResponse(
    val vendorId: String,
    val vendorName: String
)