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

/**
 * ClassLoaderInvokerFilter
 *
 * 主要的工作是：切换当前工作线程的类加载器到接口的类加载器，以便和接口的类加载的上下文一起工作。
 */
@Activate(group = Constants.PROVIDER, order = -30000)
public class ClassLoaderFilter implements Filter {

    //需要理解java的类加载机制和双亲委派模型。
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        //保存当前线程的类加载器
        ClassLoader ocl = Thread.currentThread().getContextClassLoader();
        //把当前线程的上下文类加载器设置为接口的类加载器
        Thread.currentThread().setContextClassLoader(invoker.getInterface().getClassLoader());
        try {
            //继续过滤器链的下一个节点
            return invoker.invoke(invocation);
        } finally {
            //把当前线程的上下文类加载器还原回去
            Thread.currentThread().setContextClassLoader(ocl);
        }
    }

}
