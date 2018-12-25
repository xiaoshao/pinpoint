package com.huawei.txtype.impl.springmvc;

import com.huawei.txtype.impl.DefaultRegistryMapping;
import com.navercorp.pinpoint.bootstrap.context.huawei.IMappingRegistry;
import com.navercorp.pinpoint.bootstrap.context.huawei.IRequestMappingInfo;

import java.util.Collection;

public class UrlHandlerRegistryMapping extends DefaultRegistryMapping implements IMappingRegistry {
    public final static int URL_HANDLER_LEVEL = 2;

    @Override
    public void register(IRequestMappingInfo requestMappingInfo, int level) {
        if(level == URL_HANDLER_LEVEL){
            super.register(requestMappingInfo);
        }
    }

    @Override
    public void register(Collection<IRequestMappingInfo> requestMappingInfos, int level) {
        if (level == URL_HANDLER_LEVEL){
            super.register(requestMappingInfos);
        }
    }
}
