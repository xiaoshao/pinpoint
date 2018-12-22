package com.huawei.txtype;

import com.huawei.txtype.impl.CustomerDefinitionRegistry;
import com.huawei.txtype.impl.MVCRegistryMapping;
import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;

import java.util.Collection;

public class RegistryMaping implements IMappingRegistry {
    private CustomerDefinitionRegistry customerDefinitionRegistry;

    private MVCRegistryMapping mvcRegistryMapping;

    private final RequestMappingInfo DEFAULT_REQUEST_MAPPING_INFO = new RequestMappingInfo("/**", "ALL");

    public RegistryMaping(ProfilerConfig profilerConfig) {
        this.customerDefinitionRegistry = new CustomerDefinitionRegistry(profilerConfig);
        this.mvcRegistryMapping = new MVCRegistryMapping();
    }

    public RequestMappingInfo match(String requestURI, String method) {
        RequestMappingInfo requestMappingInfo = customerDefinitionRegistry.match(requestURI, method);
        if (requestMappingInfo != null) {
            return requestMappingInfo;
        }

        requestMappingInfo = mvcRegistryMapping.match(requestURI, method);
        if (requestMappingInfo != null) {
            return requestMappingInfo;
        }

        return DEFAULT_REQUEST_MAPPING_INFO;
    }

    @Override
    public void register(RequestMappingInfo requestMappingInfo, int level) {
        mvcRegistryMapping.register(requestMappingInfo, level);
    }

    @Override
    public void register(Collection<RequestMappingInfo> requestMappingInfos, int level) {
        mvcRegistryMapping.register(requestMappingInfos, level);
    }
}
