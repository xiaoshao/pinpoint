package com.huawei.txtype;

import java.util.Collection;

public interface IMappingRegistry {

    RequestMappingInfo match(String uri, String method);

    void register(RequestMappingInfo requestMappingInfo, int level);

    void register(Collection<RequestMappingInfo> requestMappingInfos, int level);
}
