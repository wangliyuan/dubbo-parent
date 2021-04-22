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
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;

import java.util.HashMap;
import java.util.Map;

/**
 * ContextInvokerFilter
 * ContextInvokerFilter主要记录每个请求的调用上下文。每个调用都有可能产生很多中间临时信息，我们不可能要求在每个接口
 * 中都加一个上下文的参数，然后一路往下传。通常做法是放在ThreadLocal中，作为一个全局参数，当前线程中的任何一个地方都可以
 * 直接读写上下文信息。
 *
 * ContextFilter就是统一在过滤器中处理请求的上下文信息，它为每个请求维护一个RpcContext对象，该对象中维护两个
 * InternalThreadLocal,分别记录local和server的上下文。
 */
@Activate(group = Constants.PROVIDER, order = -10000)
public class ContextFilter implements Filter {

    /**
     * 主要逻辑如下：
     * 1.消除异步属性。防止异步属性传到过滤器链的下一个环节
     * 2.设置当前请求的上下文，如Invoker信息，地址信息，端口信息等。如果前面的过滤器已经对上下文设置了一些附加信息
     * （attachments 是一个map,里面可以保存各种key-value数据），则和Invoker的附加信息合并。
     * 3.调用过滤器链的一个节点。
     * 4.消除上下文信息。对于异步调用的场景，即使是同一个线程，处理不同的请求也会创建一个新的RpcContext对象。因此
     * 调用完成后，需要清理对应的上下文信息。
     * @param invoker    service
     * @param invocation invocation.
     * @return
     * @throws RpcException
     */
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        Map<String, String> attachments = invocation.getAttachments();
        if (attachments != null) {
            attachments = new HashMap<String, String>(attachments);
            attachments.remove(Constants.PATH_KEY);
            attachments.remove(Constants.GROUP_KEY);
            attachments.remove(Constants.VERSION_KEY);
            attachments.remove(Constants.DUBBO_VERSION_KEY);
            attachments.remove(Constants.TOKEN_KEY);
            attachments.remove(Constants.TIMEOUT_KEY);
            attachments.remove(Constants.ASYNC_KEY);// Remove async property to avoid being passed to the following invoke chain.
        }
        RpcContext.getContext()
                .setInvoker(invoker)
                .setInvocation(invocation)
//                .setAttachments(attachments)  // merged from dubbox
                .setLocalAddress(invoker.getUrl().getHost(),
                        invoker.getUrl().getPort());

        // mreged from dubbox
        // we may already added some attachments into RpcContext before this filter (e.g. in rest protocol)
        if (attachments != null) {
            if (RpcContext.getContext().getAttachments() != null) {
                RpcContext.getContext().getAttachments().putAll(attachments);
            } else {
                RpcContext.getContext().setAttachments(attachments);
            }
        }

        if (invocation instanceof RpcInvocation) {
            ((RpcInvocation) invocation).setInvoker(invoker);
        }
        try {
            RpcResult result = (RpcResult) invoker.invoke(invocation);
            // pass attachments to result
            result.addAttachments(RpcContext.getServerContext().getAttachments());
            return result;
        } finally {
            RpcContext.removeContext();
            RpcContext.getServerContext().clearAttachments();
        }
    }
}
