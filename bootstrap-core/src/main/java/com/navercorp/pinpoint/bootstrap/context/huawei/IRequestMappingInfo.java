package com.navercorp.pinpoint.bootstrap.context.huawei;

public interface IRequestMappingInfo {
    boolean match(String uri, String method);
    String getTxtype();
}
