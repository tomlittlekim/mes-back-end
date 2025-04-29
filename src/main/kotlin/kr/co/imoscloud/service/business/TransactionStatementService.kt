package kr.co.imoscloud.service.business

import kr.co.imoscloud.constants.CoreEnum
import kr.co.imoscloud.core.Core
import kr.co.imoscloud.entity.business.OrderDetail
import kr.co.imoscloud.entity.drive.FileManagement
import kr.co.imoscloud.repository.business.TransactionStatementRepository
import kr.co.imoscloud.repository.drive.DriveRepository
import kr.co.imoscloud.service.drive.FileConvertService
import kr.co.imoscloud.util.AbstractPrint
import kr.co.imoscloud.util.PrintDto
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class TransactionStatementService(
    val core: Core,
    private val headerRepo: TransactionStatementRepository,
    private val driveRepo: DriveRepository,
    private val orderService: OrderService,
    private val convertService: FileConvertService
): AbstractPrint(core, convertService) {

    private val MENU_ID = ""

    override fun getFodsFile(): FileManagement {
        return driveRepo.findByMenuIdAndFlagActiveIsTrue(MENU_ID)
            ?: throw IllegalArgumentException("해당 메뉴는 출력 기능을 지원하지 않습니다. ")
    }

    override fun <B> entityToPrintDto(body: B): PrintDto {
        return if (body is OrderDetailNullableDto) {
            PrintDto(
                body.materialName,
                body.materialStandard,
                body.quantity.toString(),
                body.unitPrice.toString(),
                body.supplyPrice.toString(),
                body.vatPrice.toString()
            )
        } else {
            throw IllegalArgumentException("지원하지 않는 객체입니다. ")
        }
    }

    override fun <H> extractAdditionalHeaderFields(header: H?): MutableMap<String, String>? {
        val result = mutableMapOf<String, String>()

        if (header != null && header is TransactionStatementPrintRequest) {
            result["no"] = "1"
            result["localDate"] = formattedDate(header.transactionDate, CoreEnum.DateTimeFormat.YEAR_MONTH_DAY_KOR)
            result["customer"] = header.customerName
        }

        return if (result.isEmpty()) null else result
    }

    override fun <B> extractAdditionalBodyFields(bodies: List<B>): MutableMap<String, String>? {
        val result = mutableMapOf<String, String>()
        val details = bodies.filterIsInstance<OrderDetail>()
        if (details.isEmpty()) return null

        var totalQty: Double = 0.0
        var totalPrice: Int = 0
        var totalAmount: Int = 0
        var totalVat: Int = 0
        var finalPrice: Int = 0

        for (detail in details) {
            totalQty += detail.quantity ?: 0.0
            totalPrice += detail.unitPrice ?: 0
            totalAmount += detail.supplyPrice ?: 0
            totalVat += detail.vatPrice ?: 0
            finalPrice += ((detail.supplyPrice ?: 0)+(detail.vatPrice ?: 0))
        }

        result["totalQty"] = totalQty.toString()
        result["totalPrice"] = totalPrice.toString()
        result["totalAmount"] = totalAmount.toString()
        result["totalVat"] = totalVat.toString()
        result["finalPrice"] = finalPrice.toString()
        return result
    }
}

data class TransactionStatementPrintRequest(
    val orderNo: String,
    val customerName: String,
    val transactionDate: LocalDate,
    val detailIds: List<Long>
)