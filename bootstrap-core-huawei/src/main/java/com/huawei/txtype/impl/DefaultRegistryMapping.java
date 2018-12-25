package com.huawei.txtype.impl;

import com.huawei.txtype.RequestMappingInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DefaultRegistryMapping {

    private List<RequestMappingInfo> requestMappingInfos = new ArrayList<RequestMappingInfo>();


    protected void register(RequestMappingInfo requestMappingInfo) {
        requestMappingInfos.add(requestMappingInfo);
    }

    protected void register(Collection<RequestMappingInfo> requestMappingInfos){
        this.requestMappingInfos.addAll(requestMappingInfos);;
    }

    public RequestMappingInfo match(String uri, String method) {
        return this.match(requestMappingInfos, uri, method);
    }

    protected RequestMappingInfo match(List<RequestMappingInfo> requestMappingInfos, String uri, String method){

        if(requestMappingInfos == null){
            return null;
        }

        for (RequestMappingInfo requestMappingInfo : requestMappingInfos) {
            if (requestMappingInfo.match(uri, method)) {
                return requestMappingInfo;
            }
        }

        return null;
    }


}
