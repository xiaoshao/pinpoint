/*
 * Copyright 2018 NAVER Corp.
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

package com.navercorp.pinpoint.plugin.kafka.interceptor;

import com.navercorp.pinpoint.bootstrap.context.Header;
import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.SpanEventRecorder;
import com.navercorp.pinpoint.bootstrap.context.SpanRecorder;
import com.navercorp.pinpoint.bootstrap.context.Trace;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.context.TraceId;
import com.navercorp.pinpoint.bootstrap.context.scope.TraceScope;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.sampler.SamplingFlagUtils;
import com.navercorp.pinpoint.bootstrap.util.NumberUtils;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.common.util.StringUtils;
import com.navercorp.pinpoint.plugin.kafka.KafkaConstants;
import com.navercorp.pinpoint.plugin.kafka.descriptor.EntryPointMethodDescriptor;
import com.navercorp.pinpoint.plugin.kafka.field.accessor.RemoteAddressFieldAccessor;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Taejin Koo
 */
public class ConsumerRecordEntryPointInterceptor implements AroundInterceptor {

    private static final String SCOPE_NAME = "##KAFKA_ENTRY_POINT_START_TRACE";

    private static final EntryPointMethodDescriptor ENTRY_POINT_METHOD_DESCRIPTOR = new EntryPointMethodDescriptor();

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private final TraceContext traceContext;
    private final MethodDescriptor descriptor;

    private final AtomicReference<TraceFactoryProvider.TraceFactory> tracyFactoryReference = new AtomicReference<TraceFactoryProvider.TraceFactory>();

    public ConsumerRecordEntryPointInterceptor(TraceContext traceContext, MethodDescriptor descriptor) {
        this.traceContext = traceContext;
        this.descriptor = descriptor;
        this.traceContext.cacheApi(ENTRY_POINT_METHOD_DESCRIPTOR);
    }

    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }

        ConsumerRecord consumerRecord = getConsumerRecord(args);
        if (consumerRecord == null) {
            return;
        }

        Trace trace = getTrace(consumerRecord);
        if (trace == null) {
            return;
        }

        enterScope(trace);
        if (!trace.canSampled()) {
            if (isDebug) {
                logger.debug("before() canSampled is false - skip trace");
            }
            return;
        }

        SpanEventRecorder recorder = trace.traceBlockBegin();
        recorder.recordServiceType(KafkaConstants.KAFKA_CLIENT_INTERNAL);
    }

    private ConsumerRecord getConsumerRecord(Object[] args) {
        if (args == null) {
            return null;
        }

        for (Object arg : args) {
            if (arg instanceof ConsumerRecord) {
                return (ConsumerRecord) arg;
            }
        }
        return null;
    }

    private Trace getTrace(ConsumerRecord consumerRecord) {
        final Trace currentTrace = traceContext.currentRawTraceObject();
        if (currentTrace == null) {
            Trace newTrace = createTrace(consumerRecord);
            if (newTrace != null) {
                newTrace.addScope(SCOPE_NAME);
            }
            return newTrace;
        } else {
            if (isDebug) {
                logger.debug("already has trace()");
            }
            return currentTrace;
        }
    }

    private Trace createTrace(ConsumerRecord consumerRecord) {
        TraceFactoryProvider.TraceFactory createTrace = tracyFactoryReference.get();
        if (createTrace == null) {
            createTrace = TraceFactoryProvider.get(consumerRecord);
            tracyFactoryReference.compareAndSet(null, createTrace);
        }
        return createTrace.createTrace(traceContext, consumerRecord);
    }

    private void enterScope(final Trace trace) {
        final TraceScope scope = trace.getScope(SCOPE_NAME);
        if (scope != null) {
            scope.tryEnter();
            if (isDebug) {
                logger.debug("try enter trace scope={}", scope);
            }
        } else {
            if (isDebug) {
                logger.debug("can't find scope");
            }
        }
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args, result, throwable);
        }

        ConsumerRecord consumerRecord = getConsumerRecord(args);
        if (consumerRecord == null) {
            return;
        }

        final Trace trace = traceContext.currentRawTraceObject();
        if (trace == null) {
            return;
        }

        leaveScope(trace);
        if (!trace.canSampled()) {
            if (isDebug) {
                logger.debug("after() canSampled is false - skip trace");
            }
            if (isCompletedTrace(trace)) {
                deleteTrace(trace);
            }
            return;
        }

        try {
            final SpanEventRecorder recorder = trace.currentSpanEventRecorder();
            recorder.recordApi(descriptor);
            if (throwable != null) {
                recorder.recordException(throwable);
            }

        } catch (Throwable t) {
            if (logger.isWarnEnabled()) {
                logger.warn("AFTER. Caused:{}", t.getMessage(), t);
            }
        } finally {
            trace.traceBlockEnd();
            if (isCompletedTrace(trace)) {
                deleteTrace(trace);
            }
        }
    }

    private void leaveScope(final Trace trace) {
        final TraceScope scope = trace.getScope(SCOPE_NAME);
        if (scope != null) {
            if (scope.canLeave()) {
                if (isDebug) {
                    logger.debug("leave trace scope={}", scope);
                }
                scope.leave();
                return;
            } else {
                if (isDebug) {
                    logger.debug("failed to leave trace scope={}", scope);
                }
                return;
            }
        } else {
            if (isDebug) {
                logger.debug("can't find scope");
            }
            return;
        }
    }

    private boolean isCompletedTrace(final Trace trace) {
        TraceScope scope = trace.getScope(SCOPE_NAME);
        if (scope == null) {
            return false;
        }

        return !scope.isActive();
    }

    private void deleteTrace(final Trace trace) {
        if (isDebug) {
            logger.debug("delete trace={}, sampled={}", trace, trace.canSampled());
        }

        traceContext.removeTraceObject();
        trace.close();
    }

    private static class TraceFactoryProvider {

        private static TraceFactory get(Object object) {
            try {
                final Class<?> aClass = object.getClass();
                final Method method = aClass.getMethod("headers");

                if (method != null) {
                    return new SupportContinueTraceFactory();
                }
            } catch (NoSuchMethodException e) {
                // ignore
            }
            return new DefaultTraceFactory();
        }

        private interface TraceFactory {

            Trace createTrace(TraceContext traceContext, ConsumerRecord consumerRecord);

        }

        private static class DefaultTraceFactory implements TraceFactory {

            final PLogger logger = PLoggerFactory.getLogger(this.getClass());
            final boolean isDebug = logger.isDebugEnabled();

            @Override
            public Trace createTrace(TraceContext traceContext, ConsumerRecord consumerRecord) {
                return createTrace0(traceContext, consumerRecord);
            }

            Trace createTrace0(TraceContext traceContext, ConsumerRecord consumerRecord) {
                final Trace trace = traceContext.newTraceObject("");
                if (trace.canSampled()) {
                    final SpanRecorder recorder = trace.getSpanRecorder();
                    recordRootSpan(recorder, consumerRecord);
                    if (isDebug) {
                        logger.debug("TraceID not exist. start new trace.");
                    }
                    return trace;
                } else {
                    if (isDebug) {
                        logger.debug("TraceID not exist. camSampled is false. skip trace.");
                    }
                    return null;
                }
            }

            void recordRootSpan(SpanRecorder recorder, ConsumerRecord consumerRecord) {
                recordRootSpan(recorder, consumerRecord, null, null);
            }

            void recordRootSpan(SpanRecorder recorder, ConsumerRecord consumerRecord, String parentApplicationName, String parentApplicationType) {
                recorder.recordServiceType(KafkaConstants.KAFKA_CLIENT);
                recorder.recordApi(ConsumerRecordEntryPointInterceptor.ENTRY_POINT_METHOD_DESCRIPTOR);

                String remoteAddress = getRemoteAddress(consumerRecord);
                recorder.recordEndPoint(remoteAddress);
                recorder.recordRemoteAddress(remoteAddress);

                String topic = consumerRecord.topic();
                recorder.recordRpcName(createRpcName(consumerRecord));
                recorder.recordAcceptorHost("topic:" + topic);
                recorder.recordAttribute(KafkaConstants.KAFKA_TOPIC_ANNOTATION_KEY, topic);

                recorder.recordAttribute(KafkaConstants.KAFKA_PARTITION_ANNOTATION_KEY, consumerRecord.partition());
                recorder.recordAttribute(KafkaConstants.KAFKA_OFFSET_ANNOTATION_KEY, consumerRecord.offset());

                if (StringUtils.hasText(parentApplicationName) && StringUtils.hasText(parentApplicationType)) {
                    recorder.recordParentApplication(parentApplicationName, NumberUtils.parseShort(parentApplicationType, ServiceType.UNDEFINED.getCode()));
                }
            }

            private String getRemoteAddress(Object remoteAddressFieldAccessor) {
                String remoteAddress = null;
                if (remoteAddressFieldAccessor instanceof RemoteAddressFieldAccessor) {
                    remoteAddress = ((RemoteAddressFieldAccessor) remoteAddressFieldAccessor)._$PINPOINT$_getRemoteAddress();
                }

                if (StringUtils.isEmpty(remoteAddress)) {
                    return KafkaConstants.UNKNOWN;
                } else {
                    return remoteAddress;
                }
            }

            private String createRpcName(ConsumerRecord consumerRecord) {
                StringBuilder rpcName = new StringBuilder("kafka://");
                rpcName.append("topic=").append(consumerRecord.topic());
                rpcName.append("?partition=").append(consumerRecord.partition());
                rpcName.append("&offset=").append(consumerRecord.offset());

                return rpcName.toString();
            }

        }

        private static class SupportContinueTraceFactory extends DefaultTraceFactory {

            @Override
            public Trace createTrace(TraceContext traceContext, ConsumerRecord consumerRecord) {
                org.apache.kafka.common.header.Headers headers = consumerRecord.headers();
                if (headers == null) {
                    return null;
                }

                if (!isSampled(headers)) {
                    // Even if this transaction is not a sampling target, we have to create Trace object to mark 'not sampling'.
                    // For example, if this transaction invokes rpc call, we can add parameter to tell remote node 'don't sample this transaction'
                    final Trace trace = traceContext.disableSampling();
                    if (isDebug) {
                        logger.debug("remotecall sampling flag found. skip trace");
                    }
                    return trace;
                }

                TraceId traceId = populateTraceIdFromHeaders(traceContext, headers);
                if (traceId != null) {
                    return createContinueTrace(traceContext, consumerRecord, traceId);
                } else {
                    return createTrace0(traceContext, consumerRecord);
                }
            }

            private boolean isSampled(org.apache.kafka.common.header.Headers headers) {
                org.apache.kafka.common.header.Header sampledHeader = headers.lastHeader(Header.HTTP_SAMPLED.toString());
                if (sampledHeader == null) {
                    return true;
                }

                String sampledFlag = new String(sampledHeader.value());
                return SamplingFlagUtils.isSamplingFlag(sampledFlag);
            }

            private TraceId populateTraceIdFromHeaders(TraceContext traceContext, org.apache.kafka.common.header.Headers headers) {
                String transactionId = null;
                String spanID = null;
                String parentSpanID = null;
                String flags = null;
                for (org.apache.kafka.common.header.Header header : headers.toArray()) {
                    if (header.key().equals(Header.HTTP_TRACE_ID.toString())) {
                        transactionId = new String(header.value());
                    } else if (header.key().equals(Header.HTTP_PARENT_SPAN_ID.toString())) {
                        parentSpanID = new String(header.value());
                    } else if (header.key().equals(Header.HTTP_SPAN_ID.toString())) {
                        spanID = new String(header.value());
                    } else if (header.key().equals(Header.HTTP_FLAGS.toString())) {
                        flags = new String(header.value());
                    }
                }

                if (transactionId == null || spanID == null || parentSpanID == null || flags == null) {
                    return null;
                }

                TraceId traceId = traceContext.createTraceId(transactionId, Long.parseLong(parentSpanID), Long.parseLong(spanID), Short.parseShort(flags));
                return traceId;
            }

            private Trace createContinueTrace(TraceContext traceContext, ConsumerRecord consumerRecord, TraceId traceId) {
                if (isDebug) {
                    logger.debug("TraceID exist. continue trace. traceId:{}", traceId);
                }

                Trace trace = traceContext.continueTraceObject(traceId);

                String parentApplicationName = null;
                String parentApplicationType = null;

                org.apache.kafka.common.header.Headers headers = consumerRecord.headers();
                for (org.apache.kafka.common.header.Header header : headers.toArray()) {
                    if (header.key().equals(Header.HTTP_PARENT_APPLICATION_NAME.toString())) {
                        parentApplicationName = new String(header.value());
                    } else if (header.key().equals(Header.HTTP_PARENT_APPLICATION_TYPE.toString())) {
                        parentApplicationType = new String(header.value());
                    }
                }

                if (trace.canSampled()) {
                    final SpanRecorder recorder = trace.getSpanRecorder();
                    recordRootSpan(recorder, consumerRecord, parentApplicationName, parentApplicationType);
                }
                return trace;
            }

        }

    }

}
