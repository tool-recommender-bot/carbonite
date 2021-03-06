/*
 * Copyright 2013 Evelio Tarazona Cáceres <evelio@evelio.info>
 * Copyright 2013 Carbonite contributors <contributors@evelio.info>
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
package info.evelio.carbonite;

import android.content.Context;
import com.esotericsoftware.kryo.Kryo;
import info.evelio.carbonite.cache.Cache;
import info.evelio.carbonite.cache.CacheFactory;
import info.evelio.carbonite.cache.CacheOptions;
import info.evelio.carbonite.cache.CacheType;
import info.evelio.carbonite.cache.MemoryLruCache;
import info.evelio.carbonite.cache.StorageLruCache;
import info.evelio.carbonite.serialization.KryoSerializer;
import info.evelio.carbonite.serialization.Serializer;

import java.io.File;

import static info.evelio.carbonite.Carbonite.Defaults.LRU_SIZE;
import static info.evelio.carbonite.CarboniteApi.CacheBuilder;
import static info.evelio.carbonite.cache.StorageLruCache.MINIMAL_CAPACITY;
import static info.evelio.carbonite.cache.StorageLruCache.Options;
import static info.evelio.carbonite.util.Util.illegalState;
import static info.evelio.carbonite.util.Util.newCacheInstance;
import static info.evelio.carbonite.util.Util.notNull;
import static info.evelio.carbonite.util.Util.notNullArg;
import static info.evelio.carbonite.util.Util.obtainValidKey;

/*package*/ class CacheFactoryImp<T> implements CacheFactory<String, T> {
  /*package*/ static final CacheFactoryImp INSTANCE = new CacheFactoryImp();

  @Override
  public Cache<String, T> build(CacheBuilder cacheBuilder) {
    notNullArg(cacheBuilder, "Invalid cache builder given.");

    Cache<String, T> fromOpts = buildFromOpts(cacheBuilder);
    if (fromOpts != null) {
      return fromOpts;
    }
    // Generate defaults:
    return buildDefault(cacheBuilder);
  }

  @SuppressWarnings("unchecked")
  private Cache<String, T> buildDefault(CacheBuilder options) {
    final CacheType cacheType = options.cacheType();

    switch (cacheType) {
      case MEMORY:
        return new MemoryLruCache<String, T>(new MemoryLruCache.Options(LRU_SIZE));
      case STORAGE:
        // TODO yikes a builder or something, plus kryo needs more config
        final Class type = options.type();
        final File dir = buildCacheDir(options.context(), type);
        final Serializer<T> serializer = new KryoSerializer<T>(new Kryo(), type);
        final Options storageOpts = new Options(dir, MINIMAL_CAPACITY, type, serializer);
        return new StorageLruCache<T>(storageOpts);
      default:
        illegalState(true, "Not yet implemented cache type " + cacheType);
        return null;
    }
  }

  @SuppressWarnings("unchecked")
  private Cache<String, T> buildFromOpts(CacheBuilder builder) {
    final CacheOptions opts = builder.opts();

    if (opts == null) {
      return null;
    }

    final Class<? extends Cache> type = opts.imp();

    final Cache<String, T> instance = newCacheInstance(type, opts);

    illegalState(!type.isInstance(instance),
        "Unable to instantiate cache, make sure it has a public constructor with proper options type.");

    return instance;
  }

  private static File buildCacheDir(Context context, Class type) {
    File cacheDir = context.getCacheDir();
    notNull(cacheDir, "context.getCacheDir() returned null.");

    cacheDir = new File(cacheDir, Carbonite.Defaults.STORAGE_DIRECTORY_NAME);
    return new File(cacheDir, obtainValidKey(type));
  }
}
