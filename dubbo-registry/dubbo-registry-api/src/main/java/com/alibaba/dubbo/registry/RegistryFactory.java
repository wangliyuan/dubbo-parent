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
package com.alibaba.dubbo.registry;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;

/**
 * RegistryFactory. (SPI, Singleton, ThreadSafe)
 * 使用这个扩展点，还有一些需要遵循的"潜规则"
 * 1.如果URL中设置了check=false,则连接不会被检查。否则，需要在断开连接时抛出异常
 * 2.需要支持通过username:password格式在URL中传递鉴权
 * 3.需要支持设置backup参数来指定备选注册集群的地址
 * 4.需要支持设置file参数来指定本地文件缓存
 * 5.需要支持设置timeout参数来指定请求的超时时间
 * 6.需要支持设置session参数来指定连接的超时或过期时间
 *
 * AbstractRegistryFactory已经抽象了一些通用的逻辑，可以直接继承。
 * 已有的实现：
 * zookeeper=com.alibaba.dubbo.registry.zookeeper.ZookeeperRegistryFactory
 * redis=
 * multicast=
 * dubbo=
 *
 * @see com.alibaba.dubbo.registry.support.AbstractRegistryFactory
 */
@SPI("dubbo")
public interface RegistryFactory {

    /**
     * Connect to the registry
     * <p>
     * Connecting the registry needs to support the contract: <br>
     * 1. When the check=false is set, the connection is not checked, otherwise the exception is thrown when disconnection <br>
     * 2. Support username:password authority authentication on URL.<br>
     * 3. Support the backup=10.20.153.10 candidate registry cluster address.<br>
     * 4. Support file=registry.cache local disk file cache.<br>
     * 5. Support the timeout=1000 request timeout setting.<br>
     * 6. Support session=60000 session timeout or expiration settings.<br>
     *
     * 1.当check=false，连接不检查，其他情况连接不成功抛出异常
     * 2.支持username:password在url中设置
     *
     *
     * @param url Registry address, is not allowed to be empty
     * @return Registry reference, never return empty value
     */
    @Adaptive({"protocol"})
    Registry getRegistry(URL url);

}