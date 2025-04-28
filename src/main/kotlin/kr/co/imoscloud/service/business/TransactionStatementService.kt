package kr.co.imoscloud.service.business

import kr.co.imoscloud.entity.business.OrderHeader
import kr.co.imoscloud.entity.business.TransactionStatement
import kr.co.imoscloud.repository.business.TransactionStatementRepository
import org.springframework.stereotype.Service

@Service
class TransactionStatementService(
    val headerRepo: TransactionStatementRepository,
) {

    fun generateTransactionHeader(orderHeader: OrderHeader): TransactionStatement = TransactionStatement(
        site = orderHeader.site,
        compCd = orderHeader.compCd,
        orderNo = orderHeader.orderNo,
    ).apply {
        createUser = orderHeader.createUser
        createDate = orderHeader.createDate
        updateUser = orderHeader.updateUser
        updateDate = orderHeader.updateDate
    }
}