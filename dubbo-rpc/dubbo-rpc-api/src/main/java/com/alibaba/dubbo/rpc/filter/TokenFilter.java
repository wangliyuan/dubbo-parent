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
package com.alibaba.dubbo.rpc.filter;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;

import java.util.Map;

/**
 * TokenInvokerFilter
 *
 * 在dubbo中，如果某些服务提供者不想让消费者绕过注册中心直连自己，则可以使用令牌验证。
 * 总体的工作原理是：服务提供者在发布自己的服务时会生成令牌，与服务一起注册到注册中心。消费者必须通过注册中心才能
 * 获取有令牌的服务提供者的URL.TokenFilter是在服务提供者端生效的过滤器，它的工作就是对请求的令牌做校验。
 *
 * 开启令牌校验的配置方式：
 * <!-- 随机Token令牌，使用UUID生成--> 全局设置开启令牌验证
 * <dubbo:provider interface="com.foo.BarService" token="true"></dubbo:provider>
 * <!-- 固定Token令牌，相当于密码-->
 * <dubbo:provider interface="com.foo.BarService" token="123456"></dubbo:provider>
 *
 *  * <!-- 随机Token令牌，使用UUID生成--> 服务级别设置
 *  * <dubbo:service interface="com.foo.BarService" token="true"></dubbo:service>
 *  * <!-- 固定Token令牌，相当于密码-->
 *  * <dubbo:service interface="com.foo.BarService" token="123456"></dubbo:service>
 *
 *   * <!-- 随机Token令牌，使用UUID生成--> 协议级别设置
 *  * <dubbo:protocol interface="com.foo.BarService" token="true"></dubbo:protocol>
 *  * <!-- 固定Token令牌，相当于密码-->
 *  * <dubbo:protocol interface="com.foo.BarService" token="123456"></dubbo:protocol>
 *
 *
 *  工作原理：
 *  收到请求后，首先检查这个暴露出去的服务是否有令牌信息。如果有，则获取请求中的令牌，如果和接口的令牌匹配，
 *  则认证通过，否则认证失败并抛出异常。
 */
@Activate(group = Constants.PROVIDER, value = Constants.TOKEN_KEY)
public class TokenFilter implements Filter {

    /**
     * 工作流程：
     * 1.消费者从注册中心获得服务提供者包含令牌的URL
     * 2.消费者RPC调用设置令牌。具体是在RpcInvocation的构造方法中，把服务提供者的令牌设置到附件(attachments)中
     * 一起请求服务提供者
     * 3.服务提供者认证令牌
     *
     *
     *
     * @param invoker    service
     * @param inv
     * @return
     * @throws RpcException
     */
    @Override
    public Result invoke(Invoker<?> invoker, Invocation inv)
            throws RpcException {
        String token = invoker.getUrl().getParameter(Constants.TOKEN_KEY);
        if (ConfigUtils.isNotEmpty(token)) {
            Class<?> serviceType = invoker.getInterface();
            Map<String, String> attachments = inv.getAttachments();
            String remoteToken = attachments == null ? null : attachments.get(Constants.TOKEN_KEY);
            if (!token.equals(remoteToken)) {
                throw new RpcException("Invalid token! Forbid invoke remote service " + serviceType + " method " + inv.getMethodName() + "() from consumer " + RpcContext.getContext().getRemoteHost() + " to provider " + RpcContext.getContext().getLocalHost());
            }
        }
        return invoker.invoke(inv);
    }

}
