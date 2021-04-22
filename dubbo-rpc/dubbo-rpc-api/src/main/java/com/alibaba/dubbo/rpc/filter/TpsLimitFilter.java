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
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.filter.tps.DefaultTPSLimiter;
import com.alibaba.dubbo.rpc.filter.tps.TPSLimiter;

/**
 * Limit TPS for either service or service's particular method
 * 主要用于服务提供者端的限流
 *
 * TpsLimitFilter的限流是基于令牌的，即一个时间段内只分配N个令牌，每个请求过来都会消耗一个令牌，耗完为止，后面再来的
 * 请求都会被拒绝。限流对象的维度支持分组，版本和接口级别，默认通过interface+group+version作为唯一标识来判断是否
 * 超过最大值。
 *
 * 每次发放1000个令牌
 * <dubbo:parameter key="tps" value="1000"></dubbo:parameter>
 * 令牌刷新的间隔是1s，如果不配置，则默认是60秒
 * <dubbo:parameter key="tps.interval" value="1000"></dubbo:parameter>
 */
@Activate(group = Constants.PROVIDER, value = Constants.TPS_LIMIT_RATE_KEY)
public class TpsLimitFilter implements Filter {

    private final TPSLimiter tpsLimiter = new DefaultTPSLimiter();

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {

        if (!tpsLimiter.isAllowable(invoker.getUrl(), invocation)) {
            throw new RpcException(
                    "Failed to invoke service " +
                            invoker.getInterface().getName() +
                            "." +
                            invocation.getMethodName() +
                            " because exceed max service tps.");
        }

        return invoker.invoke(invocation);
    }

}
