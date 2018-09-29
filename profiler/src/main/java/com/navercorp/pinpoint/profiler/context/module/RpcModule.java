/*
 * Copyright 2018 NAVER Corp.
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

import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import com.navercorp.pinpoint.profiler.context.provider.*;
import com.navercorp.pinpoint.profiler.receiver.CommandDispatcher;
import com.navercorp.pinpoint.profiler.sender.DataSender;
import com.navercorp.pinpoint.profiler.sender.EnhancedDataSender;
import com.navercorp.pinpoint.profiler.sender.LoggingDataSender;
import com.navercorp.pinpoint.rpc.client.ConnectionFactoryProvider;
import com.navercorp.pinpoint.rpc.client.PinpointClientFactory;
import com.navercorp.pinpoint.thrift.io.HeaderTBaseSerializer;

/**
 * @author Woonduk Kang(emeroad)
 */
public class RpcModule extends PrivateModule {
    @Override
    protected void configure() {
        bind(CommandDispatcher.class).toProvider(CommandDispatcherProvider.class).in(Scopes.SINGLETON);

        bind(ConnectionFactoryProvider.class).toProvider(ConnectionFactoryProviderProvider.class).in(Scopes.SINGLETON);

        Key<PinpointClientFactory> pinpointClientFactory = Key.get(PinpointClientFactory.class, DefaultClientFactory.class);
        bind(pinpointClientFactory).toProvider(PinpointClientFactoryProvider.class).in(Scopes.SINGLETON);
        expose(pinpointClientFactory);

        bind(HeaderTBaseSerializer.class).toProvider(HeaderTBaseSerializerProvider.class).in(Scopes.SINGLETON);

        bind(EnhancedDataSender.class).toProvider(LoggingDataSenderProvider.class).in(Scopes.SINGLETON);
        expose(EnhancedDataSender.class);

        Key<PinpointClientFactory> pinpointStatClientFactory = Key.get(PinpointClientFactory.class, SpanStatClientFactory.class);
        bind(pinpointStatClientFactory).toProvider(SpanStatClientFactoryProvider.class).in(Scopes.SINGLETON);
        expose(pinpointStatClientFactory);


        Key<DataSender> spanDataSender = Key.get(DataSender.class, SpanDataSender.class);
        bind(spanDataSender).toProvider(SpanDataSenderProvider.class).in(Scopes.SINGLETON);
        expose(spanDataSender);

        Key<DataSender> statDataSender = Key.get(DataSender.class, StatDataSender.class);
        bind(DataSender.class).annotatedWith(StatDataSender.class)
                .toProvider(StatDataSenderProvider.class).in(Scopes.SINGLETON);
        expose(statDataSender);
    }

}
