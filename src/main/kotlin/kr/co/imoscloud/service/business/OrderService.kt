package kr.co.imoscloud.service.business

import kr.co.imoscloud.entity.business.OrderDetail
import kr.co.imoscloud.entity.business.OrderHeader
import kr.co.imoscloud.repository.business.OrderDetailRepository
import kr.co.imoscloud.repository.business.OrderHeaderRepository
import kr.co.imoscloud.util.DateUtils
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class OrderService(
    val headerRepo: OrderHeaderRepository,
    val detailRepo: OrderDetailRepository
) {

    // orderHeader 조회
    fun getHeadersBySearchRequestByCompCd(req: OrderHeaderSearchRequest): List<OrderHeader> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        val (from, to) = DateUtils.getSearchDateRange(req.fromDate, req.toDate)

        val headerList = req.materialId
            ?.let { detailRepo.findAllBySearchCondition(loginUser.compCd, req.orderNo, from, to, req.customerId, req.materialId) }
            ?:run { headerRepo.findAllBySearchCondition(loginUser.compCd, req.orderNo, from, to, req.customerId) }

        return headerList
    }

    fun addHeader(): OrderHeaderNullableDto {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        val latestNo = headerRepo.getLatestOrderNo(loginUser.compCd)

        val nextVersion = latestNo
            ?.takeLast(3)
            ?.trim()
            ?.toIntOrNull()
            ?.plus(1)
            ?.let { "%03d".format(it) }
            ?: "001"

        val prefix = "${loginUser.compCd}${LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"))}"

        return OrderHeaderNullableDto(
            site = loginUser.getSite(),
            compCd = loginUser.compCd,
            orderNo = "$prefix$nextVersion",
        )
    }





    fun getDetailsByOrderNo(orderNo: String): List<OrderDetail> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        return detailRepo.findAllByOrderNoAndCompCdAndFlagActiveIsTrue(loginUser.compCd, orderNo)
    }

    fun addDetail(header: OrderHeaderNullableDto): OrderDetailNullableDto {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        if (!header.compCd.equals(loginUser.compCd)) throw IllegalArgumentException("다른 회사의 정보를 조회할 수 없습니다. ")

        val latestSubOrderNo = detailRepo.getLatestOrderSubNo(header.compCd!!)
        val nextVersion = latestSubOrderNo
            ?.takeLast(3)
            ?.trim()
            ?.toIntOrNull()
            ?.plus(1)
            ?.let { "%03d".format(it) }
            ?: "001"

        return OrderDetailNullableDto(
            site = header.site,
            compCd = header.compCd,
            orderNo = header.orderNo,
            orderSubNo = nextVersion,
        )
    }

    data class OrderHeaderSearchRequest(
        val orderNo: String?=null,
        val fromDate: String?=null,
        val toDate: String?=null,
        val customerId: String?=null,
        val materialId: String?=null,
    )

    data class OrderResponse(
        val header: List<OrderHeader> = listOf(),
        val details: List<OrderDetail>? = emptyList()
    )

    data class OrderHeaderNullableDto(
        val id: Long? = null,
        val site: String? = null,
        val compCd: String? = null,
        val orderNo: String? = null,
        val orderDate: LocalDate? = null,
        val customerId: String? = null,
        val customerName: String? = null,
        val totalAmount: Int? = 0,
        val vatAmount: Int? = 0,
        val flagVatAmount: Boolean? = true,
        val finalAmount: Int? = 0,
        val deliveryDate: LocalDateTime? = null,
        val paymentMethod: String? = null,
        val deliveryAddr: String? = null,
        val remark: String? = null
    )

    data class OrderDetailNullableDto(
        val id: Long? = null,
        val site: String?=null,
        val compCd: String? = null,
        val orderNo: String? = null,
        val orderSubNo: String? = null,
        val systemMaterialId: String? = null,
        val materialName: String? = null,
        val materialSpec: String? = null,
        val materialUnit: String? = null,
        val deliveryDate: LocalDate? = null,
        val quantity: Int? = null,
        val unitPrice: Int? = null,
        val supplyPrice: Int? = null,
        val vatPrice: Int? = null,
        val totalPrice: Int? = null,
        val remark: String? = null
    )
}