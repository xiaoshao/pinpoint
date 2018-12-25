package com.huawei.txtype.impl;

import com.huawei.txtype.IMappingRegistry;
import com.huawei.txtype.RequestMappingInfo;
import com.huawei.txtype.impl.springmvc.SpringMVCMappingRegistryFactory;

import java.util.Collection;
import java.util.List;

public class MVCRegistryMapping extends DefaultRegistryMapping implements IMappingRegistry {

    private List<IMappingRegistry> registries;

    public MVCRegistryMapping() {
        this.registries = SpringMVCMappingRegistryFactory.createMappingRegistries();
    }


    @Override
    public RequestMappingInfo match(String uri, String method) {
        if (registries != null) {

            for (IMappingRegistry registry : registries) {
                RequestMappingInfo requestMappingInfo = registry.match(uri, method);
                if (requestMappingInfo != null) {
                    return requestMappingInfo;
                }
            }
        }

        return null;
    }


    @Override
    public void register(RequestMappingInfo requestMappingInfo, int level) {
        if (level <= 0) {
            return;
        }

        if (this.registries.size() >= level) {
            registries.get(level - 1).register(requestMappingInfo, level);
        }

    }

    @Override
    public void register(Collection<RequestMappingInfo> requestMappingInfos, int level) {
        if (level <= 0) {
            return;
        }

        if (this.registries.size() >= level) {
            registries.get(level - 1).register(requestMappingInfos, level);
        }
    }
}
