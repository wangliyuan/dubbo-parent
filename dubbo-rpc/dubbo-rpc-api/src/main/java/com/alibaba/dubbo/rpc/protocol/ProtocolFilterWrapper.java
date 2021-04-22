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
package com.alibaba.dubbo.rpc.protocol;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

import java.util.List;

/**
 * ListenerProtocol
 * filter入口类
 */
public class ProtocolFilterWrapper implements Protocol {

    private final Protocol protocol;

    public ProtocolFilterWrapper(Protocol protocol) {
        if (protocol == null) {
            throw new IllegalArgumentException("protocol == null");
        }
        this.protocol = protocol;
    }

    /**
     * 如何构造调用链的？
     * 1.获取并遍历所有过滤器。通过ExtensionLoader.getActivateExtension方法获取所有的过滤器并遍历
     * 2.使用装饰器模式，增强原有Invoker，组装过滤器链。使用装饰器模式，像俄罗斯套娃一样，把过滤器一个一个地
     * "套"在Invoker上。
     * @param invoker
     * @param key
     * @param group
     * @param <T>
     * @return
     */
    private static <T> Invoker<T> buildInvokerChain(final Invoker<T> invoker, String key, String group) {
        //保存引用，后续用于把真正的调用者保存到过滤器链的最后
        Invoker<T> last = invoker;
        //获取所有的过滤器，包括有@Activate注解，默认启动的和用户在XML中自定义配置的。
        List<Filter> filters = ExtensionLoader.getExtensionLoader(Filter.class).getActivateExtension(invoker.getUrl(), key, group);
        if (!filters.isEmpty()) {
            //对过滤器做倒排遍历，即从尾到头
            for (int i = filters.size() - 1; i >= 0; i--) {
                final Filter filter = filters.get(i);
                //注意这段逻辑，把last节点变成next节点，并放到filter链的next中
                final Invoker<T> next = last;
                last = new Invoker<T>() {

                    @Override
                    public Class<T> getInterface() {
                        return invoker.getInterface();
                    }

                    @Override
                    public URL getUrl() {
                        return invoker.getUrl();
                    }

                    @Override
                    public boolean isAvailable() {
                        return invoker.isAvailable();
                    }

                    @Override
                    public Result invoke(Invocation invocation) throws RpcException {
                        //设置过滤器链的下一个节点，不断循环形成过滤器链
                        return filter.invoke(next, invocation);
                    }

                    @Override
                    public void destroy() {
                        invoker.destroy();
                    }

                    @Override
                    public String toString() {
                        return invoker.toString();
                    }
                };
            }
        }
        return last;
    }

    @Override
    public int getDefaultPort() {
        return protocol.getDefaultPort();
    }


    //1.暴露服务的时候会调用buildInvokerChain
    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        //如果协议为registry，这直接export，不进行构建filter链
        if (Constants.REGISTRY_PROTOCOL.equals(invoker.getUrl().getProtocol())) {
            return protocol.export(invoker);
        }
        //此处也会传入Contants.PROVIDER,标识自己是服务提供者类型的调用链
        return protocol.export(buildInvokerChain(invoker, Constants.SERVICE_FILTER_KEY, Constants.PROVIDER));
    }

    //2.引用远程服务的时候也会调用buildInvokerChain
    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        //如果协议为registry，直接refer
        if (Constants.REGISTRY_PROTOCOL.equals(url.getProtocol())) {
            return protocol.refer(type, url);
        }
        //出入会传入Contants.CONSUMER,标识自己是消费类型的调用链
        return buildInvokerChain(protocol.refer(type, url), Constants.REFERENCE_FILTER_KEY, Constants.CONSUMER);
    }

    @Override
    public void destroy() {
        protocol.destroy();
    }

}
