package com.navercorp.pinpoint.plugin.spring.mvc;

import com.navercorp.pinpoint.common.trace.TraceMetadataProvider;
import com.navercorp.pinpoint.common.trace.TraceMetadataSetupContext;

public class SpringMVCTraceMetadataProvider implements TraceMetadataProvider {
    @Override
    public void setup(TraceMetadataSetupContext context) {
        context.addServiceType(SpringMVCConstants.SERVICE_TYPE);
    }
}
