package kr.co.imoscloud.entity.standardInfo

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol

@Entity
@Table(name = "CODE")
class Code (
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

    @Column(name = "CODE_ID", nullable = false, length = 100)
    var codeId: String? = null,

    @Column(name = "CODE_NAME", length = 100)
    var codeName: String? = null,

    @Column(name = "CODE_DESC", length = 200)
    var codeDesc: String? = null,

    @Column(name = "SORT_ORDER")
    var sortOrder: Int? = null,
):CommonCol()