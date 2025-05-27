package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.DefectInfo
import org.springframework.data.jpa.repository.JpaRepository

interface DefectInfoRepository : JpaRepository<DefectInfo, Long>, DefectInfoRepositoryCustom