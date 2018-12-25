package com.huawei.txtype.impl.springmvc;

import com.huawei.txtype.impl.DefaultRegistryMapping;
import com.navercorp.pinpoint.bootstrap.context.huawei.IMappingRegistry;
import com.navercorp.pinpoint.bootstrap.context.huawei.IRequestMappingInfo;

import java.util.Collection;

public class MethodHandlerRegistryMapping extends DefaultRegistryMapping implements IMappingRegistry {

    public final static int METHOD_HANDLER_LEVEL = 1;

    @Override
    public void register(IRequestMappingInfo requestMappingInfo, int level) {
        if(level == METHOD_HANDLER_LEVEL){
            super.register(requestMappingInfo);
        }
    }

    @Override
    public void register(Collection<IRequestMappingInfo> requestMappingInfos, int level) {
        if (level == METHOD_HANDLER_LEVEL){
            super.register(requestMappingInfos);
        }
    }
}
