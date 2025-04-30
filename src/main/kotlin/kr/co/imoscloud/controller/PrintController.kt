package kr.co.imoscloud.controller

import jakarta.servlet.http.HttpServletResponse
import kr.co.imoscloud.service.business.TransactionStatementPrintRequest
import kr.co.imoscloud.service.business.TransactionStatementService
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping(value = ["/api/print"])
class PrintController(
    private val transactionStatementService: TransactionStatementService
) {

    @PostMapping("/ts")
    fun tsToPrint(req: TransactionStatementPrintRequest, response: HttpServletResponse) =
        transactionStatementService.printProcess(req, response)
}