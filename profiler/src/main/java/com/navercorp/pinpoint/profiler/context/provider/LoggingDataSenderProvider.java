package com.navercorp.pinpoint.profiler.context.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.navercorp.pinpoint.profiler.sender.EnhancedDataSender;
import com.navercorp.pinpoint.profiler.sender.LoggingDataSender;


public class LoggingDataSenderProvider implements Provider<EnhancedDataSender> {

    @Inject
    public LoggingDataSenderProvider() {
    }

    @Override
    public EnhancedDataSender get() {
        return new LoggingDataSender();
    }
}
