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
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcStatus;

/**
 * LimitInvokerFilter
 * 消费者端的过滤器，限制的是客户端的并发数。
 *
 * 配置：
 * <dubbo:service interface="com.foo.BarService" actives="10"></dubbo:service>
 * <dubbo:service interface="com.foo.BarService">
 *      <dubbo:method name="sayHello" actives="10"></dubbo:method>
 * </dubbo:service>
 * 消费者端如上
 *
 *
 */
@Activate(group = Constants.CONSUMER, value = Constants.ACTIVES_KEY)
public class ActiveLimitFilter implements Filter {

    /**
     * 1.获取参数。获取方法名，最大并发数等参数
     * 2.如果达到限流阀值，和服务提供者端的逻辑并不一样，并不是直接抛出异常，而是先等待直到超时，因为请求是有timeout
     * 属性的。当并发数达到阀值时，会先加锁抢占当前接口的RpcStatus对象，然后通过wait方法进行等待。此时会有两种结果：
     * 第一种是某个Invoker在调用结束后，并发把计数器原子-1并触发一个notify,会有一个在wait状态的线程被唤醒并继续
     * 执行逻辑。第二种是wait等待超时都没哟被唤醒，此时直接抛出异常。
     * 3.如果满足调用阀值，则直接调用，成功或失败都会原子-1对应并发计数。最后唤醒一个等待中的线程。
     * @param invoker    service
     * @param invocation invocation.
     * @return
     * @throws RpcException
     */
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        URL url = invoker.getUrl();
        String methodName = invocation.getMethodName();
        int max = invoker.getUrl().getMethodParameter(methodName, Constants.ACTIVES_KEY, 0);
        RpcStatus count = RpcStatus.getStatus(invoker.getUrl(), invocation.getMethodName());
        if (max > 0) {
            //获取超时时间
            long timeout = invoker.getUrl().getMethodParameter(invocation.getMethodName(), Constants.TIMEOUT_KEY, 0);
            long start = System.currentTimeMillis();
            long remain = timeout;
            //获取当前并发数
            int active = count.getActive();
            if (active >= max) {
                synchronized (count) {
                    //加锁，并循环获取当前并发数，如果大于限流阀值则等待
                    while ((active = count.getActive()) >= max) {
                        try {
                            count.wait(remain);
                        } catch (InterruptedException e) {
                        }
                        long elapsed = System.currentTimeMillis() - start;
                        //当被notify唤醒后，会先判断是否已经超时，然后继续执行while循环判断是否已经低于限流阀值
                        remain = timeout - elapsed;
                        if (remain <= 0) {
                            //超时，抛出异常
                            throw new RpcException("Waiting concurrent invoke timeout in client-side for service:  "
                                    + invoker.getInterface().getName() + ", method: "
                                    + invocation.getMethodName() + ", elapsed: " + elapsed
                                    + ", timeout: " + timeout + ". concurrent invokes: " + active
                                    + ". max concurrent invoke limit: " + max);
                        }
                    }
                }
            }
        }
        //当前并发数低于限流阀值，则会从上面的while循环跳出并来到这里
        try {
            long begin = System.currentTimeMillis();
            //并发计数器原子+1
            RpcStatus.beginCount(url, methodName);
            try {
                //执行Invoker调用
                Result result = invoker.invoke(invocation);
                //调用结束，并发计数器原子-1
                RpcStatus.endCount(url, methodName, System.currentTimeMillis() - begin, true);
                return result;
            } catch (RuntimeException t) {
                RpcStatus.endCount(url, methodName, System.currentTimeMillis() - begin, false);
                throw t;
            }
        } finally {
            //当前请求已经结束，通过notify唤醒另一个线程
            if (max > 0) {
                synchronized (count) {
                    count.notify();
                }
            }
        }
    }

}
