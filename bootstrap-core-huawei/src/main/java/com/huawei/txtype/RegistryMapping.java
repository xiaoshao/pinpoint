package com.huawei.txtype;

import com.huawei.txtype.impl.CustomerDefinitionRegistry;
import com.huawei.txtype.impl.MVCRegistryMapping;
import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.context.huawei.IMappingRegistry;
import com.navercorp.pinpoint.bootstrap.context.huawei.IRequestMappingInfo;

import java.util.Collection;

public class RegistryMapping implements IMappingRegistry {
    private CustomerDefinitionRegistry customerDefinitionRegistry;

    private MVCRegistryMapping mvcRegistryMapping;

    private final RequestMappingInfo DEFAULT_REQUEST_MAPPING_INFO = new RequestMappingInfo("/**", "ALL");

    public RegistryMapping(ProfilerConfig profilerConfig) {
        this.customerDefinitionRegistry = new CustomerDefinitionRegistry(profilerConfig);
        this.mvcRegistryMapping = new MVCRegistryMapping();
    }

    public IRequestMappingInfo match(String requestURI, String method) {
        IRequestMappingInfo requestMappingInfo = customerDefinitionRegistry.match(requestURI, method);
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
    public void register(IRequestMappingInfo requestMappingInfo, int level) {
        if(requestMappingInfo == null){
            return;
        }
        mvcRegistryMapping.register(requestMappingInfo, level);
    }

    @Override
    public void register(Collection<IRequestMappingInfo> requestMappingInfos, int level) {
        if(requestMappingInfos==null || requestMappingInfos.size() == 0){
            return;
        }
        mvcRegistryMapping.register(requestMappingInfos, level);
    }
}
