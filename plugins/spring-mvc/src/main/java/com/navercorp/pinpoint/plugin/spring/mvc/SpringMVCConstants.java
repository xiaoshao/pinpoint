package com.navercorp.pinpoint.plugin.spring.mvc;

import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.common.trace.ServiceTypeFactory;

import static com.navercorp.pinpoint.common.trace.ServiceTypeProperty.RECORD_STATISTICS;

public class SpringMVCConstants {

    private SpringMVCConstants() {
    }

    public static final String NAME = "SPRING_MVC1";
    public static final ServiceType SERVICE_TYPE = ServiceTypeFactory.of(1215, NAME, RECORD_STATISTICS);

    public static final String ROOT_CONTEXT_KEY = "Spring MVC";
}
