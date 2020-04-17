package com.biao.shop.business.conf;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.internal.Platform;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.remoting.exchange.ResponseCallback;
import com.alibaba.dubbo.rpc.protocol.dubbo.FutureAdapter;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * @ClassName TracingFilter
 * @Description: TODO
 * @Author Biao
 * @Date 2020/4/16
 * @Version V1.0
 **/
//@Activate(group = {Constants.PROVIDER, Constants.CONSUMER}, value = "tracing")
public class TracingFilter implements Filter {

    Tracer tracer;
    TraceContext.Extractor<Map<String, String>> extractor;
    TraceContext.Injector<Map<String, String>> injector;
    volatile boolean isInit = false;

    public void setTracing(Tracing tracing) {
        tracer = tracing.tracer();
        extractor = tracing.propagation().extractor(GETTER);
        injector = tracing.propagation().injector(SETTER);
        isInit = true;
    }

    static void parseRemoteAddress(RpcContext rpcContext, Span span) {
        InetSocketAddress remoteAddress = rpcContext.getRemoteAddress();
        if (remoteAddress == null) {
            return;
        }
        span.remoteIpAndPort(Platform.get().getHostString(remoteAddress), remoteAddress.getPort());
    }

    static void onError(Throwable error, Span span) {
        span.error(error);
        if (error instanceof RpcException) {
            span.tag("userinfo.error_code", Integer.toString(((RpcException) error).getCode()));
        }
    }

    static final Propagation.Getter<Map<String, String>, String> GETTER =
            new Propagation.Getter<Map<String, String>, String>() {
                @Override
                public String get(Map<String, String> carrier, String key) {
                    return carrier.get(key);
                }

                @Override
                public String toString() {
                    return "Map::get";
                }
            };

    static final Propagation.Setter<Map<String, String>, String> SETTER =
            new Propagation.Setter<Map<String, String>, String>() {
                @Override
                public void put(Map<String, String> carrier, String key, String value) {
                    carrier.put(key, value);
                }

                @Override
                public String toString() {
                    return "Map::set";
                }
            };

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (isInit == false) {
            return invoker.invoke(invocation);
        }

        RpcContext rpcContext = RpcContext.getContext();
        Span.Kind kind = rpcContext.isProviderSide() ? Span.Kind.SERVER : Span.Kind.CLIENT;
        final Span span;
        if (kind.equals(Span.Kind.CLIENT)) {
            span = tracer.nextSpan();
            injector.inject(span.context(), invocation.getAttachments());
        } else {
            TraceContextOrSamplingFlags extracted = extractor.extract(invocation.getAttachments());
            span = extracted.context() != null
                    ? tracer.joinSpan(extracted.context())
                    : tracer.nextSpan(extracted);
        }

        if (!span.isNoop()) {
            span.kind(kind);
            String service = invoker.getInterface().getSimpleName();
            String method =RpcUtils.getMethodName(invocation);
            span.name(service + "/" + method);
            parseRemoteAddress(rpcContext, span);
            span.start();
        }

        boolean isOneway = false, deferFinish = false;
        try (Tracer.SpanInScope scope = tracer.withSpanInScope(span)) {
            Result result = invoker.invoke(invocation);
            if (result.hasException()) {
                onError(result.getException(), span);
            }
            isOneway = RpcUtils.isOneway(invoker.getUrl(), invocation);
            // the case on async client invocation
            Future<Object> future = rpcContext.getFuture();
            if (future instanceof FutureAdapter) {
                deferFinish = true;
                ((FutureAdapter) future).getFuture().setCallback(new TracingFilter.FinishSpanCallback(span));
            }
            return result;
        } catch (Error | RuntimeException e) {
            onError(e, span);
            throw e;
        } finally {
            if (isOneway) {
                span.flush();
            } else if (!deferFinish) {
                span.finish();
            }
        }
    }

    static final class FinishSpanCallback implements ResponseCallback {
        final Span span;

        FinishSpanCallback(Span span) {
            this.span = span;
        }

        @Override public void done(Object response) {
            span.finish();
        }

        @Override public void caught(Throwable exception) {
            onError(exception, span);
            span.finish();
        }
    }
}
