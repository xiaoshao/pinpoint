/*
 * Copyright 2014 NAVER Corp.
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

package com.navercorp.pinpoint.bootstrap.context;

import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.context.huawei.IMappingRegistry;
import com.navercorp.pinpoint.bootstrap.plugin.jdbc.JdbcContext;
import com.navercorp.pinpoint.common.annotations.InterfaceAudience;
import com.navercorp.pinpoint.common.annotations.InterfaceStability;

/**
 * @author emeroad
 * @author hyungil.jeong
 * @author Taejin Koo
 */
public interface TraceContext {

    Trace currentTraceObject();

    /**
     * return a trace whose sampling rate should be further verified
     * 
     * @return
     */
    Trace currentRawTraceObject();

    Trace continueTraceObject(TraceId traceId);

    Trace continueTraceObject(Trace trace);

    Trace newTraceObject(String txtype);

    /**
     * internal experimental api
     */
    @InterfaceStability.Evolving
    @InterfaceAudience.LimitedPrivate("vert.x")
    Trace newAsyncTraceObject(String txtype);

    /**
     * internal experimental api
     */
    @InterfaceStability.Evolving
    @InterfaceAudience.LimitedPrivate("vert.x")
    Trace continueAsyncTraceObject(TraceId traceId);

    /**
     *
     * @deprecated Since 1.7.0
     */
    @Deprecated
    Trace continueAsyncTraceObject(AsyncTraceId traceId, int asyncId, long startTime);

    Trace removeTraceObject();

    /**
     *
     * @param closeDisableTrace true
     * @return
     * @since 1.7.0
     */
    Trace removeTraceObject(boolean closeDisableTrace);

    // ActiveThreadCounter getActiveThreadCounter();

    String getAgentId();

    String getApplicationName();

    long getAgentStartTime();

    short getServerTypeCode();

    String getServerType();

    int cacheApi(MethodDescriptor methodDescriptor);

    int cacheString(String value);

    // TODO extract jdbc related methods
    ParsingResult parseSql(String sql);

    boolean cacheSql(ParsingResult parsingResult);

    ParsingResult parseJson(String json);

    TraceId createTraceId(String transactionId, long parentSpanId, long spanId, short flags);

    Trace disableSampling();

    ProfilerConfig getProfilerConfig();

    ServerMetaDataHolder getServerMetaDataHolder();

    /**
     * internal api
     * @deprecated Since 1.7.0
     */
    @Deprecated
    int getAsyncId();

    JdbcContext getJdbcContext();

    IMappingRegistry getMappingRegistry();

}
