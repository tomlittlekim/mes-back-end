package kr.co.imoscloud.repository.business

import kr.co.imoscloud.entity.business.TransactionStatement
import org.springframework.data.jpa.repository.JpaRepository

interface TransactionStatementRepository: JpaRepository<TransactionStatement, Long> {
}