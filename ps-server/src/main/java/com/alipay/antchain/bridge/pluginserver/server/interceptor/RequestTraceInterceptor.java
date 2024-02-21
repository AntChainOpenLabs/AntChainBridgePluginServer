package com.alipay.antchain.bridge.pluginserver.server.interceptor;

import java.net.InetSocketAddress;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "req-trace")
public class RequestTraceInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        InetSocketAddress clientAddr = (InetSocketAddress) call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
        if (StrUtil.equalsIgnoreCase(call.getMethodDescriptor().getBareMethodName(), "heartbeat")) {
            if (ObjectUtil.isNull(clientAddr)) {
                log.info("heartbeat from relayer without address found");
            } else {
                log.info("heartbeat from relayer {}:{}", clientAddr.getHostString(), clientAddr.getPort());
            }
        } else if (StrUtil.equalsIgnoreCase(call.getMethodDescriptor().getBareMethodName(), "bbcCall")) {
            if (ObjectUtil.isNull(clientAddr)) {
                log.debug("bbc call from relayer without address found");
            } else {
                log.debug("bbc call from relayer {}:{}", clientAddr.getHostString(), clientAddr.getPort());
            }
        }

        return next.startCall(call, headers);
    }
}
