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
package com.alibaba.dubbo.common.extension.factory;

import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.ExtensionFactory;
import com.alibaba.dubbo.common.extension.ExtensionLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AdaptiveExtensionFactory
 */
@Adaptive
public class AdaptiveExtensionFactory implements ExtensionFactory {

    //用来缓存所有工厂实现，包括SpiExtensionFactory,SpringExtensionFactory
    private final List<ExtensionFactory> factories;

    public AdaptiveExtensionFactory() {
        //工厂列表也是通过SPI实现的，因此可以在这里获取所有工厂的扩展点加载器
        ExtensionLoader<ExtensionFactory> loader = ExtensionLoader.getExtensionLoader(ExtensionFactory.class);
        List<ExtensionFactory> list = new ArrayList<ExtensionFactory>();
        for (String name : loader.getSupportedExtensions()) {
            //遍历所有的工厂名称，获取对应的工厂，并保存到factories列表中
            list.add(loader.getExtension(name));
        }
        factories = Collections.unmodifiableList(list);
    }

    /**
     * 被缓存的工厂会通过TreeSet进行排序，SPI排在前面，spring排在后面
     * @param type object type.
     * @param name object name.
     * @param <T>
     * @return
     */
    @Override
    public <T> T getExtension(Class<T> type, String name) {
        for (ExtensionFactory factory : factories) {
            //遍历所有工厂进行查找，顺序是SPI->Spring
            T extension = factory.getExtension(type, name);
            if (extension != null) {
                return extension;
            }
        }
        return null;
    }

}
