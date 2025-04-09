package kr.co.imoscloud.model.common

import java.time.LocalDateTime

/**
 * 공통 DTO 인터페이스
 * - 모든 DTO가 공통으로 가져야 할 속성을 정의
 */
interface BaseDto {
    val id: Int?
    val createDate: String?
    val updateDate: String?
    val createUser: String?
    val updateUser: String?
    val flagActive: Boolean?
}

/**
 * 기본 DTO 추상 클래스
 * - BaseDto 인터페이스를 구현한 공통 추상 클래스
 */
abstract class AbstractBaseDto(
    override val id: Int? = null,
    override val createDate: String? = null,
    override val updateDate: String? = null,
    override val createUser: String? = null,
    override val updateUser: String? = null,
    override val flagActive: Boolean? = true
) : BaseDto

/**
 * 기본 필터 인터페이스
 * - 모든 조회 필터가 공통으로 가져야 할 속성을 정의
 */
interface BaseFilter {
    val fromDate: String?
    val toDate: String?
    val flagActive: Boolean?
}

/**
 * 기본 필터 추상 클래스
 * - BaseFilter 인터페이스를 구현한 공통 추상 클래스
 */
abstract class AbstractBaseFilter(
    override val fromDate: String? = null,
    override val toDate: String? = null,
    override val flagActive: Boolean? = true
) : BaseFilter

/**
 * 기본 입력 DTO 인터페이스
 * - 모든 입력 DTO가 공통으로 가져야 할 속성을 정의
 */
interface BaseInputDto {
    val flagActive: Boolean?
}

/**
 * 기본 응답 인터페이스
 * - 모든 응답이 공통으로 가져야 할 속성 정의
 */
interface BaseResponse<T> {
    val success: Boolean
    val message: String?
    val data: T?
    val timestamp: LocalDateTime
}

/**
 * 기본 응답 구현 클래스
 */
data class GenericResponse<T>(
    override val success: Boolean,
    override val message: String? = null,
    override val data: T? = null,
    override val timestamp: LocalDateTime = LocalDateTime.now()
) : BaseResponse<T>