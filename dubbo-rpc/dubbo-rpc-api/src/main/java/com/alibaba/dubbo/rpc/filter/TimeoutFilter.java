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
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

import java.util.Arrays;

/**
 * Log any invocation timeout, but don't stop server from running
 * 日志类型的过滤器，它会记录每个Invoker的调用时间，如果超过了接口设置的timeout值，则会打印一条告警日志，并
 * 不会干扰业务的正常运行。
 */
@Activate(group = Constants.PROVIDER)
public class TimeoutFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(TimeoutFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        //获取开始时间
        long start = System.currentTimeMillis();
        //继续调用
        Result result = invoker.invoke(invocation);
        //获取调用持续时间
        long elapsed = System.currentTimeMillis() - start;
        //如果超时了
        if (invoker.getUrl() != null
                && elapsed > invoker.getUrl().getMethodParameter(invocation.getMethodName(),
                "timeout", Integer.MAX_VALUE)) {
            if (logger.isWarnEnabled()) {
                //打印一条告警日志
                logger.warn("invoke time out. method: " + invocation.getMethodName()
                        + " arguments: " + Arrays.toString(invocation.getArguments()) + " , url is "
                        + invoker.getUrl() + ", invoke elapsed " + elapsed + " ms.");
            }
        }
        return result;
    }

}
