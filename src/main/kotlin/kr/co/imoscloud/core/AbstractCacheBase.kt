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

    protected fun  <K, V> getFromMemory(index: K): V? {
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

    fun <K, V> upsertByEntry(key: K, value: V): Unit {
        val mutableMap = if (isInspectEnabled()) getUpsertMap<K, V?>()
        else getCashMap<K, V?>()

        mutableMap[key] = value
    }

    fun <K, V> deleteByKey(key: K): Unit {
        getCashMap<K, V?>().remove(key)
    }

    fun <K, V> existsByKey(key: K): Boolean {
        return getAllFromMemory<K, V?>(listOf(key))[key] != null
    }
}