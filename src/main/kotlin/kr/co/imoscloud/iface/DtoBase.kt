package kr.co.imoscloud.iface

interface DtoLoginIdBase { val loginId: String } //TODO: User 의 AutoIncrement ID
interface DtoRoleIdBase { val roleId: Long }
interface DtoCompCdBase { val compCd: String }
interface DtoAllInOneBase : DtoLoginIdBase, DtoRoleIdBase, DtoCompCdBase