/*
 * Copyright 2017 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.profiler.context.module;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.navercorp.pinpoint.bootstrap.context.ServerMetaDataHolder;
import com.navercorp.pinpoint.bootstrap.context.Trace;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.context.huawei.IMappingRegistry;
import com.navercorp.pinpoint.bootstrap.instrument.DynamicTransformTrigger;
import com.navercorp.pinpoint.bootstrap.plugin.jdbc.JdbcContext;
import com.navercorp.pinpoint.bootstrap.sampler.Sampler;
import com.navercorp.pinpoint.common.plugin.PluginLoader;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.profiler.AgentInfoSender;
import com.navercorp.pinpoint.profiler.AgentInformation;
import com.navercorp.pinpoint.profiler.DefaultDynamicTransformerRegistry;
import com.navercorp.pinpoint.profiler.DynamicTransformerRegistry;
import com.navercorp.pinpoint.profiler.JvmInformation;
import com.navercorp.pinpoint.profiler.context.AsyncContextFactory;
import com.navercorp.pinpoint.profiler.context.AsyncTraceContext;
import com.navercorp.pinpoint.profiler.context.BaseTraceFactory;
import com.navercorp.pinpoint.profiler.context.Binder;
import com.navercorp.pinpoint.profiler.context.CallStackFactory;
import com.navercorp.pinpoint.profiler.context.DefaultSpanFactory;
import com.navercorp.pinpoint.profiler.context.ServerMetaDataRegistryService;
import com.navercorp.pinpoint.profiler.context.SpanChunkFactory;
import com.navercorp.pinpoint.profiler.context.SpanFactory;
import com.navercorp.pinpoint.profiler.context.SpanPostProcessor;
import com.navercorp.pinpoint.profiler.context.ThreadLocalBinder;
import com.navercorp.pinpoint.profiler.context.TraceFactory;
import com.navercorp.pinpoint.profiler.context.active.ActiveTraceRepository;
import com.navercorp.pinpoint.profiler.context.id.AsyncIdGenerator;
import com.navercorp.pinpoint.profiler.context.id.AtomicIdGenerator;
import com.navercorp.pinpoint.profiler.context.id.DefaultAsyncIdGenerator;
import com.navercorp.pinpoint.profiler.context.id.DefaultTraceIdFactory;
import com.navercorp.pinpoint.profiler.context.id.DefaultTraceRootFactory;
import com.navercorp.pinpoint.profiler.context.id.DefaultTransactionCounter;
import com.navercorp.pinpoint.profiler.context.id.DefaultTransactionIdEncoder;
import com.navercorp.pinpoint.profiler.context.id.IdGenerator;
import com.navercorp.pinpoint.profiler.context.id.TraceIdFactory;
import com.navercorp.pinpoint.profiler.context.id.TraceRootFactory;
import com.navercorp.pinpoint.profiler.context.id.TransactionCounter;
import com.navercorp.pinpoint.profiler.context.id.TransactionIdEncoder;
import com.navercorp.pinpoint.profiler.context.method.DefaultPredefinedMethodDescriptorRegistry;
import com.navercorp.pinpoint.profiler.context.method.PredefinedMethodDescriptorRegistry;
import com.navercorp.pinpoint.profiler.context.monitor.DataSourceMonitorRegistryService;
import com.navercorp.pinpoint.profiler.context.monitor.DefaultJdbcContext;
import com.navercorp.pinpoint.profiler.context.monitor.JdbcUrlParsingService;
import com.navercorp.pinpoint.profiler.context.provider.*;
import com.navercorp.pinpoint.profiler.context.provider.plugin.PluginLoaderProvider;
import com.navercorp.pinpoint.profiler.context.recorder.DefaultRecorderFactory;
import com.navercorp.pinpoint.profiler.context.recorder.RecorderFactory;
import com.navercorp.pinpoint.profiler.context.storage.StorageFactory;
import com.navercorp.pinpoint.profiler.instrument.InstrumentEngine;
import com.navercorp.pinpoint.profiler.interceptor.factory.ExceptionHandlerFactory;
import com.navercorp.pinpoint.profiler.metadata.ApiMetaDataService;
import com.navercorp.pinpoint.profiler.metadata.DefaultJsonMetaDataService;
import com.navercorp.pinpoint.profiler.metadata.DefaultSqlMetaDataService;
import com.navercorp.pinpoint.profiler.metadata.DefaultStringMetaDataService;
import com.navercorp.pinpoint.profiler.metadata.JsonMetaDataService;
import com.navercorp.pinpoint.profiler.metadata.SqlMetaDataService;
import com.navercorp.pinpoint.profiler.metadata.StringMetaDataService;
import com.navercorp.pinpoint.profiler.monitor.AgentStatMonitor;
import com.navercorp.pinpoint.profiler.monitor.DeadlockMonitor;
import com.navercorp.pinpoint.profiler.monitor.DeadlockThreadRegistry;
import com.navercorp.pinpoint.profiler.monitor.DefaultAgentStatMonitor;
import com.navercorp.pinpoint.profiler.monitor.metric.response.ResponseTimeCollector;
import com.navercorp.pinpoint.profiler.monitor.metric.response.ReuseResponseTimeCollector;
import com.navercorp.pinpoint.profiler.objectfactory.ObjectBinderFactory;
import com.navercorp.pinpoint.profiler.plugin.PluginContextLoadResult;
import com.navercorp.pinpoint.profiler.util.AgentInfoFactory;

import java.lang.instrument.ClassFileTransformer;


/**
 * @author Woonduk Kang(emeroad)
 */
public class ApplicationContextModule extends AbstractModule {

    public ApplicationContextModule() {
    }

    @Override
    protected void configure() {
        binder().requireExplicitBindings();
        binder().requireAtInjectOnConstructors();
        binder().disableCircularProxies();

        bind(ServiceType.class).annotatedWith(ApplicationServerType.class).toProvider(ApplicationServerTypeProvider.class).in(Scopes.SINGLETON);
        bind(IMappingRegistry.class).toProvider(MappingRegistryProvider.class).in(Scopes.SINGLETON);

        bind(ServerMetaDataRegistryService.class).toProvider(ServerMetaDataRegistryServiceProvider.class).in(Scopes.SINGLETON);
        bind(ServerMetaDataHolder.class).toProvider(ServerMetaDataHolderProvider.class).in(Scopes.SINGLETON);
        bind(StorageFactory.class).toProvider(StorageFactoryProvider.class).in(Scopes.SINGLETON);

        bindServiceComponent();

        bind(DataSourceMonitorRegistryService.class).toProvider(DataSourceMonitorRegistryServiceProvider.class).in(Scopes.SINGLETON);

        bind(IdGenerator.class).to(AtomicIdGenerator.class).in(Scopes.SINGLETON);
        bind(AsyncIdGenerator.class).to(DefaultAsyncIdGenerator.class).in(Scopes.SINGLETON);
        bind(TransactionCounter.class).to(DefaultTransactionCounter.class).in(Scopes.SINGLETON);
        bind(TransactionIdEncoder.class).to(DefaultTransactionIdEncoder.class).in(Scopes.SINGLETON);

        bind(Sampler.class).toProvider(SamplerProvider.class).in(Scopes.SINGLETON);


        final TypeLiteral<Binder<Trace>> binder = new TypeLiteral<Binder<Trace>>() {};
        final TypeLiteral<ThreadLocalBinder<Trace>> threadLocalBinder = new TypeLiteral<ThreadLocalBinder<Trace>>() {};
        bind(binder).to(threadLocalBinder).in(Scopes.SINGLETON);
        bind(TraceContext.class).toProvider(TraceContextProvider.class).in(Scopes.SINGLETON);
        bind(AsyncTraceContext.class).toProvider(AsyncTraceContextProvider.class).in(Scopes.SINGLETON);
        bind(AsyncContextFactory.class).toProvider(AsyncContextFactoryProvider.class).in(Scopes.SINGLETON);

        bind(DeadlockThreadRegistry.class).toProvider(DeadlockThreadRegistryProvider.class).in(Scopes.SINGLETON);

        bindTraceComponent();

        bind(ResponseTimeCollector.class).to(ReuseResponseTimeCollector.class).in(Scopes.SINGLETON);
        bind(ActiveTraceRepository.class).toProvider(ActiveTraceRepositoryProvider.class).in(Scopes.SINGLETON);

        bind(PluginContextLoadResult.class).toProvider(PluginContextLoadResultProvider.class).in(Scopes.SINGLETON);
        bind(PluginLoader.class).toProvider(PluginLoaderProvider.class).in(Scopes.SINGLETON);

        bind(JdbcContext.class).to(DefaultJdbcContext.class).in(Scopes.SINGLETON);
        bind(JdbcUrlParsingService.class).toProvider(JdbcUrlParsingServiceProvider.class).in(Scopes.SINGLETON);

        bind(AgentInformation.class).toProvider(AgentInformationProvider.class).in(Scopes.SINGLETON);

        bind(InstrumentEngine.class).toProvider(InstrumentEngineProvider.class).in(Scopes.SINGLETON);
        bind(ExceptionHandlerFactory.class).toProvider(ExceptionHandlerFactoryProvider.class).in(Scopes.SINGLETON);
        bind(ObjectBinderFactory.class).toProvider(ObjectBinderFactoryProvider.class).in(Scopes.SINGLETON);
        bind(ClassFileTransformer.class).toProvider(ClassFileTransformerProvider.class).in(Scopes.SINGLETON);
        bind(DynamicTransformerRegistry.class).to(DefaultDynamicTransformerRegistry.class).in(Scopes.SINGLETON);
        bind(DynamicTransformTrigger.class).toProvider(DynamicTransformTriggerProvider.class).in(Scopes.SINGLETON);
//        bind(ClassFileTransformer.class).toProvider(ClassFileTransformerWrapProvider.class).in(Scopes.SINGLETON);

        bind(JvmInformation.class).toProvider(JvmInformationProvider.class).in(Scopes.SINGLETON);
        bind(AgentInfoFactory.class).toProvider(AgentInfoFactoryProvider.class).in(Scopes.SINGLETON);
        bind(DeadlockMonitor.class).toProvider(DeadlockMonitorProvider.class).in(Scopes.SINGLETON);
        bind(AgentInfoSender.class).toProvider(AgentInfoSenderProvider.class).in(Scopes.SINGLETON);
        bind(AgentStatMonitor.class).to(DefaultAgentStatMonitor.class).in(Scopes.SINGLETON);
    }

    private void bindTraceComponent() {
        bind(TraceRootFactory.class).to(DefaultTraceRootFactory.class).in(Scopes.SINGLETON);
        bind(TraceIdFactory.class).to(DefaultTraceIdFactory.class).in(Scopes.SINGLETON);
        bind(CallStackFactory.class).toProvider(CallStackFactoryProvider.class).in(Scopes.SINGLETON);

        bind(SpanFactory.class).to(DefaultSpanFactory.class).in(Scopes.SINGLETON);
        bind(SpanPostProcessor.class).toProvider(SpanPostProcessorProvider.class).in(Scopes.SINGLETON);
        bind(SpanChunkFactory.class).toProvider(SpanChunkFactoryProvider.class).in(Scopes.SINGLETON);

        bind(RecorderFactory.class).to(DefaultRecorderFactory.class).in(Scopes.SINGLETON);

        bind(BaseTraceFactory.class).toProvider(BaseTraceFactoryProvider.class).in(Scopes.SINGLETON);
        bind(TraceFactory.class).toProvider(TraceFactoryProvider.class).in(Scopes.SINGLETON);
    }


    private void bindServiceComponent() {

        bind(StringMetaDataService.class).to(DefaultStringMetaDataService.class).in(Scopes.SINGLETON);
        bind(ApiMetaDataService.class).toProvider(ApiMetaDataServiceProvider.class).in(Scopes.SINGLETON);
        bind(SqlMetaDataService.class).to(DefaultSqlMetaDataService.class).in(Scopes.SINGLETON);
        bind(JsonMetaDataService.class).to(DefaultJsonMetaDataService.class).in(Scopes.SINGLETON);
        bind(PredefinedMethodDescriptorRegistry.class).to(DefaultPredefinedMethodDescriptorRegistry.class).in(Scopes.SINGLETON);
    }


}