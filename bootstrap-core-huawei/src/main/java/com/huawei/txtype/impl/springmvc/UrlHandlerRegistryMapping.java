package com.huawei.txtype.impl.springmvc;

import com.huawei.txtype.impl.DefaultRegistryMapping;
import com.huawei.txtype.IMappingRegistry;
import com.huawei.txtype.RequestMappingInfo;

import java.util.Collection;

public class UrlHandlerRegistryMapping extends DefaultRegistryMapping implements IMappingRegistry {
    public final static int URL_HANDLER_LEVEL = 2;

    @Override
    public void register(RequestMappingInfo requestMappingInfo, int level) {
        if(level == URL_HANDLER_LEVEL){
            super.register(requestMappingInfo);
        }
    }

    @Override
    public void register(Collection<RequestMappingInfo> requestMappingInfos, int level) {
        if (level == URL_HANDLER_LEVEL){
            super.register(requestMappingInfos);
        }
    }
}
