package tech.aiflowy.ai.tinyflow.repository;

import com.alicp.jetcache.Cache;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

public class BaseRepository {

    @Resource(name = "defaultCache")
    private Cache<String, Object> cache;

    /**
     * chain 的相关状态缓存三天
     */
    protected void putCache(String key, Object value) {
        cache.put(key, value, 3, TimeUnit.DAYS);
    }

    protected <T> T getCache(String key, Class<T> clazz) {
        Object value = cache.get(key);
        if (value == null) {
            return null;
        }
        return clazz.cast(value);
    }
}
