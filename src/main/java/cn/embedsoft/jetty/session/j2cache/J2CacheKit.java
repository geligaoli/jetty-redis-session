/**
 * Copyright (c) 2011-2019.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.embedsoft.jetty.session.j2cache;

import java.io.Serializable;
import java.util.Collection;
import java.util.function.Function;

import net.oschina.j2cache.CacheChannel;
import net.oschina.j2cache.CacheObject;


/**
 * J2CacheKit. Useful tool box for J2Cache.
 */
public class J2CacheKit {

    private static CacheChannel cache;
    private static String cacheName;

    static void init(CacheChannel cache, String cacheName) {
        J2CacheKit.cache = cache;
        J2CacheKit.cacheName = cacheName;
    }

    public static CacheChannel getCacheChannel(){
        return cache;
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        CacheObject cacheObject = cache.get(cacheName, key);
        return cacheObject != null ? (T) cacheObject.getValue() : null;
    }

    public static void put(String key, Object value) {
        cache.set(cacheName, key, (Serializable) value, true);
    }

    public static void remove(String... keys) {
        cache.evict(cacheName, keys);
    }

    public static void removeAll() {
        cache.clear(cacheName);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String key, Function<String, Object> loader) {
        CacheObject cacheObject = cache.get(cacheName, key, loader);
        return cacheObject != null ? (T) cacheObject.getValue() : null;
    }

    public static Collection<String> getKeys() {
        return cache.keys(cacheName);
    }

    public static boolean exists(String key) {
        return cache.exists(cacheName, key);
    }

}


