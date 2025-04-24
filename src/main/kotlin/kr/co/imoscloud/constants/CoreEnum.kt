package kr.co.imoscloud.constants

class CoreEnum {
    enum class MaterialType(val key: String, val value: String) {
        COMPLETE_PRODUCT("COMPLETE_PRODUCT", "완제품"),
        HALF_PRODUCT("HALF_PRODUCT", "반제품"),
        RAW_MATERIAL("RAW_MATERIAL", "원자재"),
        SUB_MATERIAL("SUB_MATERIAL", "부자재"),
    }

    enum class DateTimeFormat(val value: String) {
        DATE_TIME("yyyyMMdd_HHmmss"),
        DATE("yyyyMMdd"),
        TIME("HHmmss"),
        SANITATION_SENSOR_DATE_TIME("yyyy-MM-dd HH:mm:ss.SSSSSS"),
        MOS_EVENT_TIME("yyyyMMddHHmmss"),
        DATE_TIME_VIEW("yyyy-MM-dd HH:mm:ss"),
        DATE_TIME_VIEW_SHORT("yyyy-MM-dd HH:mm"),
        DATE_TIME_SENSOR("yyyy-MM-dd HH:mm:ss.SSSSSS"),
        DATE_VIEW("yyyy-MM-dd"),
        TIME_VIEW("HH:mm:ss"),
        TIME_VIEW_SHORT("HH:mm"),
        YEAR("yyyy"),
        YEAR_MONTH("yyyy-MM"),
        YEAR_MONTH_KOR("yyyy년 MM월"),
        YEAR_MONTH_DAY_KOR("yyyy년 MM월 dd일"),
        DAY("dd"),
        MONTH_DAY_TIME_MIN("MM-dd HH:mm")
    }

    enum class SensorType(val key: String){
        POWER("POWER"),
        TEMPER("TEMPER"),
        VIBRA("VIBRA"),
        DOOR("DOOR"),
        RIDAR("RIDAR")
    }

    enum class DrivePath(val value: String) {
        HOME_PATH("/app"),
        ATTACHMENT_PATH("/attachments")
    }
}