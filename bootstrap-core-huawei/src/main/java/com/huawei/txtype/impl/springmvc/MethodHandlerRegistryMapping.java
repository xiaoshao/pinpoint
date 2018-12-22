package com.huawei.txtype.impl.springmvc;

import com.huawei.txtype.DefaultRegistryMapping;
import com.huawei.txtype.IMappingRegistry;
import com.huawei.txtype.RequestMappingInfo;

import java.util.Collection;

public class MethodHandlerRegistryMapping extends DefaultRegistryMapping implements IMappingRegistry {

    public final static int METHOD_HANDLER_LEVEL = 1;

    @Override
    public void register(RequestMappingInfo requestMappingInfo, int level) {
        if(level == METHOD_HANDLER_LEVEL){
            super.register(requestMappingInfo);
        }
    }

    @Override
    public void register(Collection<RequestMappingInfo> requestMappingInfos, int level) {
        if (level == METHOD_HANDLER_LEVEL){
            super.register(requestMappingInfos);
        }
    }
}
