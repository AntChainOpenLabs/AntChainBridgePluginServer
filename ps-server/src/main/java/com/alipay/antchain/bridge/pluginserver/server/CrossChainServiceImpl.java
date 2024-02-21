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

package com.alipay.antchain.bridge.pluginserver.server;

import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.DefaultBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.SDPContract;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
import com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService;
import com.alipay.antchain.bridge.pluginserver.pluginmanager.IPluginManagerWrapper;
import com.alipay.antchain.bridge.pluginserver.server.exception.ServerErrorCodeEnum;
import com.alipay.antchain.bridge.pluginserver.server.interceptor.RequestTraceInterceptor;
import com.alipay.antchain.bridge.pluginserver.service.*;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService(interceptors = RequestTraceInterceptor.class)
@Slf4j
public class CrossChainServiceImpl extends CrossChainServiceGrpc.CrossChainServiceImplBase {
    @Resource
    private IPluginManagerWrapper pluginManagerWrapper;

    @Override
    public void heartbeat(Empty request, StreamObserver<Response> responseObserver) {
        responseObserver.onNext(
                ResponseBuilder.buildHeartbeatSuccessResp(
                        HeartbeatResponse.newBuilder()
                                .addAllDomains(pluginManagerWrapper.allRunningDomains())
                                .addAllProducts(pluginManagerWrapper.allSupportProducts())
                )
        );
        responseObserver.onCompleted();
    }

    @Override
    public void ifProductSupport(IfProductSupportRequest request, StreamObserver<Response> responseObserver) {
        responseObserver.onNext(
                ResponseBuilder.buildIfProductSupportSuccessResp(
                        IfProductSupportResponse.newBuilder()
                                .putAllResults(request.getProductsList().stream().distinct().collect(Collectors.toMap(p -> p, p -> pluginManagerWrapper.hasPlugin(p))))
                )
        );
        responseObserver.onCompleted();
    }

    @Override
    public void ifDomainAlive(IfDomainAliveRequest request, StreamObserver<Response> responseObserver) {
        responseObserver.onNext(
                ResponseBuilder.buildIfDomainAliveSuccessResp(
                        IfDomainAliveResponse.newBuilder()
                                .putAllResults(request.getDomainsList().stream().distinct().collect(Collectors.toMap(d -> d, d -> pluginManagerWrapper.hasDomain(d))))
                )
        );
        responseObserver.onCompleted();
    }

    @Override
    public void bbcCall(CallBBCRequest request, StreamObserver<Response> responseObserver) {
        String product = request.getProduct();
        String domain = request.getDomain();
        Response resp;

        // 1. Startup request needs to be handled separately， because it may need create a service first.
        if (request.hasStartUpReq()) {
            responseObserver.onNext(handleStartUp(product, domain, request.getStartUpReq()));
            responseObserver.onCompleted();
            return;
        }

        if (!pluginManagerWrapper.hasPlugin(product)) {
            responseObserver.onNext(ResponseBuilder.buildFailResp(ServerErrorCodeEnum.BBC_PLUGIN_NOT_SUPPORT, "product not supported"));
            responseObserver.onCompleted();
            return;
        }

        if (!pluginManagerWrapper.hasDomain(domain)) {
            responseObserver.onNext(ResponseBuilder.buildFailResp(ServerErrorCodeEnum.BBC_OBJECT_NOT_STARTED, "call startup plz"));
            responseObserver.onCompleted();
            return;
        }

        // 2. Other bbc requests need to be processed based on an existing service.
        IBBCService bbcService;
        try {
            bbcService = pluginManagerWrapper.getBBCService(product, domain);
            if (ObjectUtil.isNull(bbcService)) {
                throw new RuntimeException("null bbc service object");
            }
        } catch (Exception e){
            log.error("BBCCall fail when getting the bbc object [product: {}, domain: {}, request: {}, errorCode: {}, errorMsg: {}]",
                    product, domain, request.getRequestCase(), ServerErrorCodeEnum.BBC_GET_SERVICE_ERROR.getErrorCode(), ServerErrorCodeEnum.BBC_GET_SERVICE_ERROR.getShortMsg(), e);
            responseObserver.onNext(ResponseBuilder.buildFailResp(ServerErrorCodeEnum.BBC_GET_SERVICE_ERROR, e.toString()));
            responseObserver.onCompleted();
            return;
        }

        // 3. Other bbc requests handler.
        switch (request.getRequestCase()){
            case SHUTDOWNREQ:
                resp = handleShutDown(bbcService, product, domain);
                break;
            case GETCONTEXTREQ:
                resp = handleGetContext(bbcService, product, domain);
                break;
            case SETUPSDPMESSAGECONTRACTREQ:
                resp = handleSetupSDPMessageContract(bbcService, product, domain);
                break;
            case SETUPAUTHMESSAGECONTRACTREQ:
                resp = handleSetupAuthMessageContract(bbcService, product, domain);
                break;
            case SETPROTOCOLREQ:
                resp = handleSetProtocol(bbcService, request.getSetProtocolReq(), product, domain);
                break;
            case SETAMCONTRACTREQ:
                resp = handleSetAmContract(bbcService, request.getSetAmContractReq(), product, domain);
                break;
            case RELAYAUTHMESSAGEREQ:
                resp = handleRelayAuthMessage(bbcService, request.getRelayAuthMessageReq(), product, domain);
                break;
            case READCROSSCHAINMESSAGERECEIPTREQ:
                resp = handleReadCrossChainMessageReceiptRequest(bbcService, request.getReadCrossChainMessageReceiptReq(), product, domain);
                break;
            case READCROSSCHAINMESSAGESBYHEIGHTREQ:
                resp = handleReadCrossChainMessagesByHeight(bbcService, request.getReadCrossChainMessagesByHeightReq(), product, domain);
                break;
            case QUERYSDPMESSAGESEQREQ:
                resp = handleQuerySDPMessageSeq(bbcService, request.getQuerySDPMessageSeqReq(), product, domain);
                break;
            case QUERYLATESTHEIGHTREQ:
                resp = handleQueryLatestHeight(bbcService, product, domain);
                break;
            case SETLOCALDOMAINREQ:
                resp = handleSetLocalDomain(bbcService, request.getSetLocalDomainReq(), product, domain);
                break;
            default:
                log.error("BBCCall fail [product: {}, domain: {}, request: {}, errorCode: {}, errorMsg: {}]", product, domain, request.getRequestCase(), ServerErrorCodeEnum.UNSUPPORT_BBC_REQUEST_ERROR.getErrorCode(), ServerErrorCodeEnum.UNSUPPORT_BBC_REQUEST_ERROR.getShortMsg());
                resp = ResponseBuilder.buildFailResp(ServerErrorCodeEnum.UNSUPPORT_BBC_REQUEST_ERROR);
                break;
        }

        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    private Response handleStartUp(String product, String domain, StartUpRequest request) {
        IBBCService bbcService;

        // 1. get service
        if(pluginManagerWrapper.hasDomain(domain)){
            log.info("get service for blockchain ( product: {} , domain: {} )", product, domain);
            try {
                bbcService = pluginManagerWrapper.getBBCService(product, domain);
            } catch (Exception e){
                log.error("BBCCall(handleStartUp) fail [product: {}, domain: {}, errorCode: {}, errorMsg: {}]", product, domain, ServerErrorCodeEnum.BBC_GET_SERVICE_ERROR.getErrorCode(), ServerErrorCodeEnum.BBC_GET_SERVICE_ERROR.getShortMsg(), e);
                return ResponseBuilder.buildFailResp(ServerErrorCodeEnum.BBC_GET_SERVICE_ERROR, e.toString());
            }
        } else {
            log.info("create service for blockchain ( product: {} , domain: {} )", product, domain);
            try{
                bbcService = pluginManagerWrapper.createBBCService(product, domain);
            } catch (Exception e){
                log.error("BBCCall(handleStartUp) fail [product: {}, domain: {}, errorCode: {}, errorMsg: {}]", product, domain, ServerErrorCodeEnum.BBC_CREATE_ERROR.getErrorCode(), ServerErrorCodeEnum.BBC_CREATE_ERROR.getShortMsg(), e);
                return ResponseBuilder.buildFailResp(ServerErrorCodeEnum.BBC_CREATE_ERROR, e.toString());
            }
        }

        log.info("startup service for blockchain ( product: {} , domain: {} )", product, domain);
        // 2. start service
        try{
            DefaultBBCContext ctx = new DefaultBBCContext();
            ctx.decodeFromBytes(request.getRawContext().toByteArray());
            bbcService.startup(ctx);

            return ResponseBuilder.buildBBCSuccessResp(CallBBCResponse.newBuilder());
        } catch (Exception e){
            log.error("BBCCall(handleStartUp) fail [product: {}, domain: {}, errorCode: {}, errorMsg: {}]", product, domain, ServerErrorCodeEnum.BBC_STARTUP_ERROR.getErrorCode(), ServerErrorCodeEnum.BBC_STARTUP_ERROR.getShortMsg(), e);
            return ResponseBuilder.buildFailResp(ServerErrorCodeEnum.BBC_STARTUP_ERROR, e.toString());
        }
    }

    private Response handleShutDown(IBBCService bbcService, String product, String domain) {
        try {
            bbcService.shutdown();
            return ResponseBuilder.buildBBCSuccessResp(CallBBCResponse.newBuilder());
        } catch (Exception e){
            log.error("BBCCall(handleShutDown) fail [product: {}, domain: {}, errorCode: {}, errorMsg: {}]", product, domain, ServerErrorCodeEnum.BBC_SHUTDOWN_ERROR.getErrorCode(), ServerErrorCodeEnum.BBC_SHUTDOWN_ERROR.getShortMsg(), e);
            return ResponseBuilder.buildFailResp(ServerErrorCodeEnum.BBC_SHUTDOWN_ERROR, e.toString());
        }
    }

    private Response handleGetContext(IBBCService bbcService, String product, String domain) {
        try {
            AbstractBBCContext ctx = bbcService.getContext();
            return ResponseBuilder.buildBBCSuccessResp(CallBBCResponse.newBuilder()
                    .setGetContextResp(GetContextResponse.newBuilder()
                            .setRawContext(ByteString.copyFrom(ctx.encodeToBytes()))
                    )
            );
        } catch (Exception e){
            log.error("BBCCall(handleGetContext) fail [product: {}, domain: {}, errorCode: {}, errorMsg: {}]", product, domain, ServerErrorCodeEnum.BBC_GETCONTEXT_ERROR.getErrorCode(), ServerErrorCodeEnum.BBC_GETCONTEXT_ERROR.getShortMsg(), e);
            return ResponseBuilder.buildFailResp(ServerErrorCodeEnum.BBC_GETCONTEXT_ERROR, e.toString());
        }
    }

    private Response handleSetupSDPMessageContract(IBBCService bbcService, String product, String domain) {
        try {
            bbcService.setupSDPMessageContract();
            SDPContract sdp = bbcService.getContext().getSdpContract();
            return ResponseBuilder.buildBBCSuccessResp(CallBBCResponse.newBuilder()
                    .setSetupSDPResp(SetupSDPMessageContractResponse.newBuilder()
                            .setSdpContract(
                                    SDPMessageContract.newBuilder()
                                            .setContractAddress(sdp.getContractAddress())
                                            .setStatusValue(sdp.getStatus().ordinal())
                            )
                    )
            );
        } catch (Exception e) {
            log.error("BBCCall(handleSetupSDPMessageContract) fail [product: {}, domain: {}, errorCode: {}, errorMsg: {}]", product, domain, ServerErrorCodeEnum.BBC_SETUPSDPMESSAGECONTRACT_ERROR.getErrorCode(), ServerErrorCodeEnum.BBC_SETUPSDPMESSAGECONTRACT_ERROR.getShortMsg(), e);
            return ResponseBuilder.buildFailResp(ServerErrorCodeEnum.BBC_SETUPSDPMESSAGECONTRACT_ERROR, e.toString());
        }
    }

    private Response handleSetupAuthMessageContract(IBBCService bbcService, String product, String domain) {
        try {
            bbcService.setupAuthMessageContract();
            return ResponseBuilder.buildBBCSuccessResp(CallBBCResponse.newBuilder()
                    .setSetupAMResp(SetupAuthMessageContractResponse.newBuilder()
                            .setAmContract(
                                    AuthMessageContract.newBuilder()
                                            .setContractAddress(bbcService.getContext().getAuthMessageContract().getContractAddress())
                                            .setStatusValue(bbcService.getContext().getAuthMessageContract().getStatus().ordinal())
                            )
                    )
            );
        } catch (Exception e) {
            log.error("BBCCall(handleSetupAuthMessageContract) fail [product: {}, domain: {}, errorCode: {}, errorMsg: {}]", product, domain, ServerErrorCodeEnum.BBC_SETUPAUTHMESSAGECONTRACT_ERROR.getErrorCode(), ServerErrorCodeEnum.BBC_SETUPAUTHMESSAGECONTRACT_ERROR.getShortMsg(), e);
            return ResponseBuilder.buildFailResp(ServerErrorCodeEnum.BBC_SETUPAUTHMESSAGECONTRACT_ERROR, e.toString());
        }
    }

    private Response handleSetProtocol(IBBCService bbcService, SetProtocolRequest request, String product, String domain) {
        try {
            bbcService.setProtocol(request.getProtocolAddress(), request.getProtocolType());
            return ResponseBuilder.buildBBCSuccessResp(CallBBCResponse.newBuilder());
        } catch (Exception e) {
            log.error("BBCCall(handleSetProtocol) fail [product: {}, domain: {}, errorCode: {}, errorMsg: {}]", product, domain, ServerErrorCodeEnum.BBC_SETPROTOCOL_ERROR.getErrorCode(), ServerErrorCodeEnum.BBC_SETPROTOCOL_ERROR.getShortMsg(), e);
            return ResponseBuilder.buildFailResp(ServerErrorCodeEnum.BBC_SETPROTOCOL_ERROR, e.toString());
        }
    }

    private Response handleSetAmContract(IBBCService bbcService, SetAmContractRequest request, String product, String domain) {
        try {
            bbcService.setAmContract(request.getContractAddress());
            return ResponseBuilder.buildBBCSuccessResp(CallBBCResponse.newBuilder());
        } catch (Exception e) {
            log.error("BBCCall(handleSetAmContract) fail [product: {}, domain: {}, errorCode: {}, errorMsg: {}]", product, domain, ServerErrorCodeEnum.BBC_SETAMCONTRACT_ERROR.getErrorCode(), ServerErrorCodeEnum.BBC_SETAMCONTRACT_ERROR.getShortMsg(), e);
            return ResponseBuilder.buildFailResp(ServerErrorCodeEnum.BBC_SETAMCONTRACT_ERROR, e.toString());
        }
    }

    private Response handleRelayAuthMessage(IBBCService bbcService, RelayAuthMessageRequest request, String product, String domain) {
        try {
            CrossChainMessageReceipt ret = bbcService.relayAuthMessage(request.getRawMessage().toByteArray());
            return ResponseBuilder.buildBBCSuccessResp(CallBBCResponse.newBuilder()
                    .setRelayAuthMessageResponse(RelayAuthMessageResponse.newBuilder()
                            .setReceipt(com.alipay.antchain.bridge.pluginserver.service.CrossChainMessageReceipt.newBuilder()
                                    .setTxhash(ObjectUtil.defaultIfNull(ret.getTxhash(), ""))
                                    .setConfirmed(ret.isConfirmed())
                                    .setSuccessful(ret.isSuccessful())
                                    .setErrorMsg(ObjectUtil.defaultIfNull(ret.getErrorMsg(), ""))
                            )
                    )
            );
        } catch (Exception e) {
            log.error("BBCCall(handleRelayAuthMessage) fail [product: {}, domain: {}, errorCode: {}, errorMsg: {}]", product, domain, ServerErrorCodeEnum.BBC_RELAYAUTHMESSAGE_ERROR.getErrorCode(), ServerErrorCodeEnum.BBC_RELAYAUTHMESSAGE_ERROR.getShortMsg(), e);
            return ResponseBuilder.buildFailResp(ServerErrorCodeEnum.BBC_RELAYAUTHMESSAGE_ERROR, e.toString());
        }
    }

    private Response handleReadCrossChainMessageReceiptRequest(IBBCService bbcService, ReadCrossChainMessageReceiptRequest request, String product, String domain) {
        try {
            CrossChainMessageReceipt receipt = bbcService.readCrossChainMessageReceipt(request.getTxhash());
            if (ObjectUtil.isNull(receipt)) {
                throw new RuntimeException("empty receipt for tx " + request.getTxhash());
            }

            return ResponseBuilder.buildBBCSuccessResp(
                    CallBBCResponse.newBuilder().setReadCrossChainMessageReceiptResp(
                            ReadCrossChainMessageReceiptResponse.newBuilder()
                                    .setReceipt(
                                            com.alipay.antchain.bridge.pluginserver.service.CrossChainMessageReceipt.newBuilder()
                                                    .setConfirmed(receipt.isConfirmed())
                                                    .setSuccessful(receipt.isSuccessful())
                                                    .setTxhash(StrUtil.nullToDefault(receipt.getTxhash(), ""))
                                                    .setErrorMsg(StrUtil.nullToDefault(receipt.getErrorMsg(), ""))
                                    )
                    )
            );
        } catch (Exception e) {
            log.error("BBCCall(handleIsCrossChainMessageConfirmed) fail [product: {}, domain: {}, errorCode: {}, errorMsg: {}]", product, domain, ServerErrorCodeEnum.BBC_READ_CCMSG_RET_ERROR.getErrorCode(), ServerErrorCodeEnum.BBC_READ_CCMSG_RET_ERROR.getShortMsg(), e);
            return ResponseBuilder.buildFailResp(ServerErrorCodeEnum.BBC_READ_CCMSG_RET_ERROR, e.toString());
        }
    }

    private Response handleReadCrossChainMessagesByHeight(IBBCService bbcService, ReadCrossChainMessagesByHeightRequest request, String product, String domain) {
        try {
            return ResponseBuilder.buildBBCSuccessResp(CallBBCResponse.newBuilder()
                    .setReadCrossChainMessagesByHeightResp(ReadCrossChainMessagesByHeightResponse.newBuilder()
                            .addAllMessageList(
                                    bbcService.readCrossChainMessagesByHeight(request.getHeight()).stream()
                                            .map(m -> CrossChainMessage.newBuilder()
                                                    .setType(CrossChainMessageType.forNumber(m.getType().ordinal()))
                                                    .setMessage(ByteString.copyFrom(m.getMessage()))
                                                    .setProvableData(ProvableLedgerData.newBuilder()
                                                            .setHeight(m.getProvableData().getHeight())
                                                            .setLedgerData(ByteString.copyFrom(
                                                                    ObjectUtil.defaultIfNull(m.getProvableData().getLedgerData(), new byte[]{})))
                                                            .setProof(ByteString.copyFrom(
                                                                    ObjectUtil.defaultIfNull(m.getProvableData().getProof(), new byte[]{})))
                                                            .setBlockHash(ByteString.copyFrom(
                                                                    ObjectUtil.defaultIfNull(m.getProvableData().getBlockHash(), new byte[]{})))
                                                            .setTimestamp(m.getProvableData().getTimestamp())
                                                            .setTxHash(ByteString.copyFrom(
                                                                    ObjectUtil.defaultIfNull(m.getProvableData().getTxHash(), new byte[]{})))
                                                    ).build()
                                            ).collect(Collectors.toList())
                            )
                    )
            );
        } catch (Exception e) {
            log.error("BBCCall(handleReadCrossChainMessagesByHeight) fail [product: {}, domain: {}, errorCode: {}, errorMsg: {}]", product, domain, ServerErrorCodeEnum.BBC_READCROSSCHAINMESSAGESBYHEIGHT_ERROR.getErrorCode(), ServerErrorCodeEnum.BBC_READCROSSCHAINMESSAGESBYHEIGHT_ERROR.getShortMsg(), e);
            return ResponseBuilder.buildFailResp(ServerErrorCodeEnum.BBC_READCROSSCHAINMESSAGESBYHEIGHT_ERROR, e.toString());
        }
    }

    private Response handleQuerySDPMessageSeq(IBBCService bbcService, QuerySDPMessageSeqRequest request, String product, String domain) {
        try {
            return ResponseBuilder.buildBBCSuccessResp(CallBBCResponse.newBuilder()
                    .setQuerySDPMsgSeqResp(QuerySDPMessageSeqResponse.newBuilder()
                            .setSequence(
                                    bbcService.querySDPMessageSeq(
                                            request.getSenderDomain(),
                                            request.getFromAddress(),
                                            request.getReceiverDomain(),
                                            request.getToAddress()
                                    )
                            )
                    )
            );
        } catch (Exception e) {
            log.error("BBCCall(handleQuerySDPMessageSeq) fail [product: {}, domain: {}, errorCode: {}, errorMsg: {}]", product, domain, ServerErrorCodeEnum.BBC_QUERYSDPMESSAGESEQ_ERROR.getErrorCode(), ServerErrorCodeEnum.BBC_QUERYSDPMESSAGESEQ_ERROR.getShortMsg(), e);
            return ResponseBuilder.buildFailResp(ServerErrorCodeEnum.BBC_QUERYSDPMESSAGESEQ_ERROR, e.toString());
        }
    }

    private Response handleQueryLatestHeight(IBBCService bbcService, String product, String domain) {
        try {
            return ResponseBuilder.buildBBCSuccessResp(CallBBCResponse.newBuilder()
                    .setQueryLatestHeightResponse(QueryLatestHeightResponse.newBuilder()
                            .setHeight(bbcService.queryLatestHeight())
                    )
            );
        } catch (Exception e) {
            log.error(
                    "BBCCall(handleQueryLatestHeight) fail [product: {}, domain: {}, errorCode: {}, errorMsg: {}]",
                    product, domain,
                    ServerErrorCodeEnum.BBC_QUERYLATESTHEIGHT_ERROR.getErrorCode(),
                    ServerErrorCodeEnum.BBC_QUERYLATESTHEIGHT_ERROR.getShortMsg(),
                    e
            );
            return ResponseBuilder.buildFailResp(ServerErrorCodeEnum.BBC_QUERYLATESTHEIGHT_ERROR, e.toString());
        }
    }

    private Response handleSetLocalDomain(IBBCService bbcService, SetLocalDomainRequest request, String product, String domain) {
        try {
            bbcService.setLocalDomain(request.getDomain());
            return ResponseBuilder.buildBBCSuccessResp(CallBBCResponse.newBuilder());
        } catch (Exception e) {
            log.error(
                    "BBCCall(handleSetLocalDomain) fail [product: {}, domain: {}, errorCode: {}, errorMsg: {}]",
                    product, domain,
                    ServerErrorCodeEnum.BBC_SETLOCALDOMAIN_ERROR.getErrorCode(),
                    ServerErrorCodeEnum.BBC_SETLOCALDOMAIN_ERROR.getShortMsg(),
                    e
            );
            return ResponseBuilder.buildFailResp(ServerErrorCodeEnum.BBC_SETLOCALDOMAIN_ERROR, e.toString());
        }
    }
}
