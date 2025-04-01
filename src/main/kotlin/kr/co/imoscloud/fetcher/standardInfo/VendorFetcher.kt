package kr.co.imoscloud.fetcher.standardInfo

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.service.standardInfo.VendorResponse
import kr.co.imoscloud.service.standardInfo.VendorService

@DgsComponent
class VendorFetcher(
    val vendorService: VendorService
) {

    @DgsQuery
    fun getVendors(@InputArgument("filter") filter:VendorFilter ): List<VendorResponse?> {
        return vendorService.getVendors(filter)
    }

    @DgsMutation
    fun saveVendor(
        @InputArgument("createdRows") createdRows: List<VendorInput?>,
        @InputArgument("updatedRows") updatedRows:List<VendorUpdate?>
    ): Boolean {
        vendorService.saveVendor(createdRows,updatedRows)
        return true
    }

    @DgsMutation
    fun deleteVendor(@InputArgument("vendorId") vendorId: String): Boolean {
        return vendorService.deleteVendor(vendorId)
    }

}

data class VendorFilter(
    val vendorId:String,
    val vendorName:String,
    val ceoName:String,
    val businessType:String,
    val flagActive:String? = null
)

data class VendorInput(
    val vendorName: String,
    val vendorType: String,
    val businessRegNo: String,
    val ceoName: String? = null,
    val businessType: String? = null,
    val address: String? = null,
    val telNo: String? = null,
    val flagActive: String? = null
)

data class VendorUpdate(
    val vendorId: String,
    val vendorName: String,
    val vendorType: String,
    val businessRegNo: String,
    val ceoName: String? = null,
    val businessType: String? = null,
    val address: String? = null,
    val telNo: String? = null,
    val flagActive: String? = null
)