package kr.co.imoscloud.core

abstract class AbstractCacheBase {

    abstract fun initialSetting(): Unit
    abstract fun <K, V> getCashMap(): MutableMap<K, V?>
    abstract fun <K, V> getUpsertMap(): MutableMap<K, V?>
    abstract fun isInspectEnabled(): Boolean
    abstract fun <T, V> copyToValue(target: T?): V?
    abstract fun <K, V> getAllDuringInspection(indies: List<K>): MutableMap<K, V?>

    protected fun <K, V> getAllFromMemory(indies: List<K>): Map<K, V?> {
        return if (isInspectEnabled()) {
            getAllDuringInspection(indies)
        } else {
            getCashMap<K, V?>().mapValues { (_, v) -> copyToValue(v) }
        }
    }

    protected fun <K, V> getFromMemory(index: K): V? {
        return getAllFromMemory<K, V?>(listOf(index))[index]
    }

    protected fun <K, V> merge(): Unit {
        val cacheMap: MutableMap<K, V?> = getCashMap<K, V?>()
        val upsertMap = getUpsertMap<K, V?>()

        val (firstLock, secondLock) = if (System.identityHashCode(cacheMap) < System.identityHashCode(upsertMap)) {
            cacheMap to upsertMap
        } else {
            upsertMap to cacheMap
        }

        synchronized(firstLock) {
            synchronized(secondLock) {
                cacheMap.putAll(upsertMap)
                upsertMap.clear()
            }
        }
    }

    protected fun <K, V, E> saveAllAndSyncCache(
        entities: List<E>,
        saveFunction: (List<E>) -> List<E>,
        keySelector: (E) -> K,
        valueMapper: (E) -> V
    ): List<E> {
        return try {
            val saved = saveFunction(entities)
            saved.forEach { e ->
                val key = keySelector(e)
                val value = valueMapper(e)
                upsertByEntry(key, value)
            }
            saved
        } catch (e: Exception) {
            throw IllegalArgumentException("DB 저장 및 InMemory 동기화에 실패했습니다.")
        }
    }

    protected fun <K, V> deleteByKey(key: K): Unit {
        getCashMap<K, V?>().remove(key)
    }

    fun <K, V> existsByKey(key: K): Boolean {
        return getAllFromMemory<K, V?>(listOf(key))[key] != null
    }

    fun <K, V, F> groupByKeySelector(keySelector: (V) -> F?): Map<F?, List<V?>> {
        if (isInspectEnabled()) return emptyMap()

        val list: List<V> = getCashMap<K, V?>().values.mapNotNull { v -> copyToValue(v) }
        val map: Map<F?, List<V?>> = list.filterNotNull().groupBy { keySelector(it) }
        return map
    }

    fun <K, V, F> associateByKeySelector(keySelector: (V) -> F?): Map<F?, V?> {
        if (isInspectEnabled()) return emptyMap()

        val list: List<V> = getCashMap<K, V?>().values.mapNotNull { v -> copyToValue(v) }
        val map: Map<F?, V?> = list.filterNotNull().associateBy { keySelector(it) }
        return map
    }

    private fun <K, V> upsertByEntry(key: K, value: V): Unit {
        val mutableMap = if (isInspectEnabled()) getUpsertMap<K, V?>()
        else getCashMap<K, V?>()

        mutableMap[key] = value
    }
}