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

package com.alipay.antchain.bridge.pluginserver.server.exception;

import lombok.Getter;

@Getter
public enum ServerErrorCodeEnum {
    // TODO: add element when you need

    SUCCESS(0, "success"),

    UNSUPPORT_BBC_REQUEST_ERROR(200, "unsupport bbc request type"),

    BBC_GET_SERVICE_ERROR(201, "[bbc] get service failed"),

    BBC_CREATE_ERROR(202, "[bbc] create service failed"),

    BBC_STARTUP_ERROR(203, "[bbc] start up failed"),

    BBC_SHUTDOWN_ERROR(204, "[bbc] shut down failed"),

    BBC_GETCONTEXT_ERROR(205, "[bbc] get context failed"),

    BBC_SETUPSDPMESSAGECONTRACT_ERROR(206, "[bbc] set up sdp contract failed"),

    BBC_SETUPAUTHMESSAGECONTRACT_ERROR(207, "[bbc] set up am contract failed"),

    BBC_SETPROTOCOL_ERROR(208, "[bbc] set protocol failed"),

    BBC_SETAMCONTRACT_ERROR(209, "[bbc] set am contract failed"),

    BBC_ADDVALIDRELAYER_ERROR(210, "[bbc] add valid relayer failed"),

    BBC_RELAYAUTHMESSAGE_ERROR(211, "[bbc] forward relayer auth msg failed"),

    BBC_READ_CCMSG_RET_ERROR(212, "[bbc] read cross chain msg receipt failed"),

    BBC_READCROSSCHAINMESSAGESBYHEIGHT_ERROR(213, "[bbc] read cross chain msg by height failed"),

    BBC_QUERYSDPMESSAGESEQ_ERROR(214, "[bbc] query sdp msg sequence failed"),

    BBC_QUERYLATESTHEIGHT_ERROR(215, "[bbc] query latest height failed"),

    BBC_SETLOCALDOMAIN_ERROR(216, "[bbc] set local domain failed"),

    BBC_OBJECT_NOT_STARTED(217, "[bbc] none bbc object started"),

    BBC_PLUGIN_NOT_SUPPORT(218, "[bbc] none plugin found"),

    UNSUPPORT_MANAGE_REQUEST_ERROR(300, "unsupport manage request type"),

    MANAGE_LOAD_PLUGINS_ERROR(301, "[manage] load plugins failed"),

    MANAGE_START_PLUGINS_ERROR(302, "[manage] start plugins failed"),

    MANAGE_LOAD_PLUGIN_ERROR(303, "[manage] load plugin in the specified path failed"),

    MANAGE_START_PLUGIN_ERROR(304, "[manage] start plugin in the specified path failed"),

    MANAGE_STOP_PLUGIN_ERROR(305, "[manage] stop plugin of specified product failed"),

    MANAGE_START_PLUGIN_FROM_STOP_ERROR(306, "[manage] start plugin of specified product from stop failed"),

    MANAGE_RELOAD_PLUGIN_ERROR(307, "[manage] reload plugin failed"),

    MANAGE_RELOAD_PLUGIN_IN_NEW_PATH_ERROR(308, "[manage] reload plugin in new path failed"),

    MANAGE_RESTART_BBC_ERROR(309, "[manage] restart bbc failed"),

    UNKNOWN_ERROR(100, "unknow error");

    /**
     * Error code for errors happened in project {@code antchain-bridge-pluginserver}
     */
    private final int errorCode;

    /**
     * Every code has a short message to describe the error stuff
     */
    private final String shortMsg;

    ServerErrorCodeEnum(int errorCode, String shortMsg) {
        this.errorCode = errorCode;
        this.shortMsg = shortMsg;
    }
}
