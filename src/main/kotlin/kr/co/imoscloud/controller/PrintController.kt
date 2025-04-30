package kr.co.imoscloud.controller

import jakarta.servlet.http.HttpServletResponse
import kr.co.imoscloud.service.business.TransactionStatementPrintRequest
import kr.co.imoscloud.service.business.TransactionStatementService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/api/print"])
class PrintController(
    private val transactionStatementService: TransactionStatementService
) {

    @PostMapping("/ts", produces = ["application/pdf"])
    fun tsToPrint(@RequestBody req: TransactionStatementPrintRequest, response: HttpServletResponse) =
        transactionStatementService.printProcess(req, response)
}