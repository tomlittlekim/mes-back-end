package kr.co.imoscloud.iface

interface DtoUserIdBase { val userId: Long } //TODO: User 의 AutoIncrement ID
interface DtoRoleIdBase { val roleId: Long }
interface DtoCompCdBase { val compCd: String }
interface DtoAllInOneBase : DtoUserIdBase, DtoRoleIdBase, DtoCompCdBase