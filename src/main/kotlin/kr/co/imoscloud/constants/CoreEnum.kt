package kr.co.imoscloud.constants

class CoreEnum {
    enum class MaterialType(val key: String, val value: String) {
        COMPLETE_PRODUCT("COMPLETE_PRODUCT", "완제품"),
        HALF_PRODUCT("HALF_PRODUCT", "반제품"),
        RAW_MATERIAL("RAW_MATERIAL", "원자재"),
        SUB_MATERIAL("SUB_MATERIAL", "부자재"),
    }
}