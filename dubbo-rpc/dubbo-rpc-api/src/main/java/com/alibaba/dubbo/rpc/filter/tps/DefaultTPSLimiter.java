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
package com.alibaba.dubbo.rpc.filter.tps;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DefaultTPSLimiter implements TPSLimiter {

    private final ConcurrentMap<String, StatItem> stats
            = new ConcurrentHashMap<String, StatItem>();

    /**
     * 1.获取URL中的参数，包含每次方法的令牌数，令牌刷新时间间隔
     * 2.如果设置了每次发放的令牌数则开始限流校验。用一个ConcurrentMap缓存每个接口的令牌数，
     * key是interface+group+version,value是一个StatItem对象，它包装了令牌刷新时间间隔，每次发放的令牌数等
     * 属性。首先判断上次发放令牌的时间点是否超过时间间隔了，如果超过了就重新发放令牌，之前没用完的不会叠加，而是直接覆盖。
     * 然后，通过CAS的方式-1令牌，减掉后令牌数如果小于0则会触发限流。
     * @param url        url
     * @param invocation invocation
     * @return
     */
    @Override
    public boolean isAllowable(URL url, Invocation invocation) {
        int rate = url.getParameter(Constants.TPS_LIMIT_RATE_KEY, -1);
        long interval = url.getParameter(Constants.TPS_LIMIT_INTERVAL_KEY,
                Constants.DEFAULT_TPS_LIMIT_INTERVAL);
        String serviceKey = url.getServiceKey();
        if (rate > 0) {
            StatItem statItem = stats.get(serviceKey);
            if (statItem == null) {
                stats.putIfAbsent(serviceKey,
                        new StatItem(serviceKey, rate, interval));
                statItem = stats.get(serviceKey);
            } else {
                //rate or interval has changed, rebuild
                if (statItem.getRate() != rate || statItem.getInterval() != interval) {
                    stats.put(serviceKey, new StatItem(serviceKey, rate, interval));
                    statItem = stats.get(serviceKey);
                }
            }
            return statItem.isAllowable();
        } else {
            StatItem statItem = stats.get(serviceKey);
            if (statItem != null) {
                stats.remove(serviceKey);
            }
        }

        return true;
    }

}
