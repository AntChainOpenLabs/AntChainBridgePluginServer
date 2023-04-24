/*
 * Copyright 2023 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.bridge.pluginserver.config;

import cn.hutool.core.collection.ListUtil;
import com.alipay.antchain.bridge.pluginserver.pluginmanager.IPluginManagerWrapper;
import com.alipay.antchain.bridge.pluginserver.pluginmanager.PluginManagerWrapperImpl;
import com.alipay.antchain.bridge.pluginserver.server.PluginManagementServiceImpl;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.ClassLoadingStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
@Slf4j
public class PluginManagerConfiguration {
    @Value("${pluginserver.plugin.repo}")
    private String pluginPath;

    @Value("${pluginserver.plugin.policy.classloader.resource.ban-with-prefix.APPLICATION}")
    private String[] resourceBannedPrefixOnAppLevel;

    @Bean
    public IPluginManagerWrapper pluginManagerWrapper() {
        return new PluginManagerWrapperImpl(
                pluginPath,
                convertPathPrefixBannedMap(resourceBannedPrefixOnAppLevel)
        );
    }

    private Map<ClassLoadingStrategy.Source, Set<String>> convertPathPrefixBannedMap(
            String[] resourceBannedPrefixOnAppLevel
    ) {
        Map<ClassLoadingStrategy.Source, Set<String>> result = new HashMap<>();

        Set<String> appSet = new HashSet<>(ListUtil.of(resourceBannedPrefixOnAppLevel));
        result.put(ClassLoadingStrategy.Source.APPLICATION, appSet);

        return result;
    }

    @Value("${pluginserver.managerserver.port}")
    private String pluginServerMgrPort;

    @Bean
    public Server pluginMgrServer() throws IOException {
        log.info("Starting plugin managing server on port " + pluginServerMgrPort);
        return NettyServerBuilder.forPort(Integer.parseInt(pluginServerMgrPort))
                .addService(new PluginManagementServiceImpl())
                .build()
                .start();
    }
}
