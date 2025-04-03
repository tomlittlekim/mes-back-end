package kr.co.imoscloud.entity.standardInfo

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol
import java.time.LocalDate

@Entity
@Table(name = "CODE_CLASS")
class CodeClass (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ", nullable = false)
    var id: Int? = null,

    @Column(name = "SITE", nullable = false, length = 20)
    var site: String? = null,

    @Column(name = "COMP_CD", nullable = false, length = 20)
    var compCd: String? = null,

    @Column(name = "CODE_CLASS_ID", nullable = false, length = 100)
    var codeClassId: String? = null,

    @Column(name = "CODE_CLASS_NAME", length = 100)
    var codeClassName: String? = null,

    @Column(name = "CODE_CLASS_DESC", length = 200)
    var codeClassDesc: String? = null,
):CommonCol()