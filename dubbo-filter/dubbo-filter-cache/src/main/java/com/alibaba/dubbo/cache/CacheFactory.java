/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.cache;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;
import com.alibaba.dubbo.rpc.Invocation;

/**
 * CacheFactory
 * 我们可以通过dubbo:method配置每个方法的调用返回值是否进行缓存，用于加速数据访问速度。
 * 已有的实现：
 * threadlocal=当前线程缓存，比如一个页面渲染，用到很多portal,每个portal都要去查用户信息，通过线程缓存可以减少这种多余访问。
 * lru=基于最近最少使用原则删除多于缓存，保存最新的数据被缓存
 * jcache=与JSR107集成，可以桥接各种缓存实现
 * expiring=实现了会过期的缓存，有一个守护线程会一直去检查缓存是否过期
 */
@SPI("lru")
public interface CacheFactory {

    @Adaptive("cache")
    Cache getCache(URL url, Invocation invocation);

}
