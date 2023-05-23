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

package com.alipay.antchain.bridge.pluginserver.pluginmanager;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.plugins.manager.AntChainBridgePluginManagerFactory;
import com.alipay.antchain.bridge.plugins.manager.core.IAntChainBridgePlugin;
import com.alipay.antchain.bridge.plugins.manager.core.IAntChainBridgePluginManager;
import com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.ClassLoadingStrategy;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class PluginManagerWrapperImpl implements IPluginManagerWrapper {

    private final IAntChainBridgePluginManager manager;

    public PluginManagerWrapperImpl(String path, Map<ClassLoadingStrategy.Source, Set<String>> pathPrefixBannedMap) {
        log.info("plugins path: {}", Paths.get(path).toAbsolutePath());
        manager = AntChainBridgePluginManagerFactory.createPluginManager(path, ObjectUtil.defaultIfNull(pathPrefixBannedMap, new HashMap<>()));
        loadPlugins();
        startPlugins();
    }

    @Override
    public void loadPlugins() {
        manager.loadPlugins();
    }

    @Override
    public void startPlugins() {
        manager.startPlugins();
    }

    @Override
    public void loadPlugin(String path) {
        manager.loadPlugin(Paths.get(path));
    }

    @Override
    public void startPlugin(String path) {
        manager.startPlugin(Paths.get(path));
    }

    @Override
    public void stopPlugin(String product) {
        manager.stopPlugin(product);
    }

    @Override
    public void startPluginFromStop(String product) {
        manager.startPluginFromStop(product);
    }

    @Override
    public void reloadPlugin(String product) {
        manager.reloadPlugin(product);
    }

    @Override
    public void reloadPlugin(String product, String path) {
        manager.reloadPlugin(product, Paths.get(path));
    }

    @Override
    public IAntChainBridgePlugin getPlugin(String product) {
        return manager.getPlugin(product);
    }

    @Override
    public boolean hasPlugin(String product) {
        return manager.hasPlugin(product);
    }

    @Override
    public List<String> allSupportProducts() {
        return manager.allSupportProducts();
    }

    @Override
    public IBBCService createBBCService(String product, String domain) {
        return manager.createBBCService(product, new CrossChainDomain(domain));
    }

    @Override
    public IBBCService getBBCService(String product, String domain) {
        return manager.getBBCService(product, new CrossChainDomain(domain));
    }

    @Override
    public boolean hasDomain(String domain) {
        return manager.hasDomain(new CrossChainDomain(domain));
    }

    @Override
    public List<String> allRunningDomains() {
        return manager.allRunningDomains().stream().map(CrossChainDomain::toString).collect(Collectors.toList());
    }
}
