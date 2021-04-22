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
package com.alibaba.dubbo.rpc;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;

/**
 * Protocol. (API/SPI, Singleton, ThreadSafe)
 *
 * Protocol的每个接口会有一些"潜规则"，在实现自定义协议的时候需要注意。
 * export方法：
 * 1.协议收到请求后应记录请求源IP地址。通过RpcContext.getContext().setRemoteAddress()方法存入RPC上下文
 * 2.export方法必须实现幂等，即无论调用多少次，返回的URL都是相同的
 * 3.Invoker实例由框架传入，无需关心协议层
 *
 * refer方法：
 * 1.当我们调用refer()方法返回Invoker对象的invoke()方法时，协议也需要相应地执行invoke()方法。这一点在
 * 设计自定义协议的Invoker时需要注意
 * 2.正常来说refer()方法返回的自定义Invoker需要继承Invoker接口
 * 3.当URL的参数有check=false时，自定义的协议实现必须不能抛出异常，而是在出现连接失败异常时尝试恢复连接。
 *
 * destroy方法；
 * 1.调用destory方法的时候，需要销毁所有本地协议暴露和引用的方法。
 * 2.需要释放所有占用的资源，如连接，端口等。
 * 3.自定义的协议可以在被销毁后继续导出和引用新服务
 *
 *
 * 整个Procotol的逻辑由Protocol,Exporter,Invoker三个接口串联起来。
 *
 * 已有的扩展实现：
 * inJvm
 * dubbo
 * rmi
 * http
 * hessian
 * rest
 * thrift
 * webservice
 * redis
 * memcached
 *
 *
 */
@SPI("dubbo")
public interface Protocol {

    /**
     * 获取缺省端口，当用户没有配置端口时使用
     * @return default port
     */
    int getDefaultPort();

    /**
     * Export service for remote invocation: <br>
     * 1. Protocol should record request source address after receive a request:
     * RpcContext.getContext().setRemoteAddress();<br>
     * 2. export() must be idempotent, that is, there's no difference between invoking once and invoking twice when
     * export the same URL<br>
     * 3. Invoker instance is passed in by the framework, protocol needs not to care <br>
     *
     * 暴露远程服务
     * 1.协议在接受请求时，应记录请求来源方地址信息：RpcContext.getContext().setRemoteAddress()
     * 2.export()必须是幂等的，也就是暴露同一个url的invoker两次和一次没有区别
     * 3.export()传入的invoker由框架实现传入，协议不需要关心
     * @param <T>     Service type
     * @param invoker Service invoker
     * @return exporter reference for exported service, useful for unexport the service later
     * @throws RpcException thrown when error occurs during export the service, for example: port is occupied
     */
    @Adaptive
    <T> Exporter<T> export(Invoker<T> invoker) throws RpcException;

    /**
     * Refer a remote service: <br>
     * 1. When user calls `invoke()` method of `Invoker` object which's returned from `refer()` call, the protocol
     * needs to correspondingly execute `invoke()` method of `Invoker` object <br>
     * 2. It's protocol's responsibility to implement `Invoker` which's returned from `refer()`. Generally speaking,
     * protocol sends remote request in the `Invoker` implementation. <br>
     * 3. When there's check=false set in URL, the implementation must not throw exception but try to recover when
     * connection fails.
     *
     * 引用远程服务：
     * 1.当用户调用refer()所返回的Invoker对象的invoke()方法时，协议需相应执行同URL远端export()传入的Invoker对象的invoke()方法
     * 2.refer()返回的Invoker由协议实现，协议通常需要在此Invoker中发送远程请求。
     * 3.当url中有设置check=false时，连接失败不能抛出异常，并内部自动恢复
     * @param <T>  Service type
     * @param type Service class
     * @param url  URL address for the remote service
     * @return invoker service's local proxy
     * @throws RpcException when there's any error while connecting to the service provider
     */
    @Adaptive
    <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException;

    /**
     * Destroy protocol: <br>
     * 1. Cancel all services this protocol exports and refers <br>
     * 2. Release all occupied resources, for example: connection, port, etc. <br>
     * 3. Protocol can continue to export and refer new service even after it's destroyed.
     * 释放协议
     * 1.取消该协议所有已经暴露和引用的服务
     * 2.释放协议所占用的所有资源，比如连接和端口
     * 3.协议在释放后，依然能暴露和引用新的服务
     */
    void destroy();

}