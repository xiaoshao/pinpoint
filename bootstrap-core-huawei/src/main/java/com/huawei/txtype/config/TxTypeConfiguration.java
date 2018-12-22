package com.huawei.txtype.config;

import com.huawei.txtype.RequestMappingInfo;
import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.common.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TxTypeConfiguration {

    private ProfilerConfig profilerConfig;
    private List<RequestMappingInfo> requestMappingInfos;
    public static final String CUSTOMER_TXTYPE = "profiler.txtype";

    public TxTypeConfiguration(ProfilerConfig profilerConfig) {
        this.profilerConfig = profilerConfig;
        this.requestMappingInfos = initRequestMappingInfos();
    }

    private List<RequestMappingInfo> initRequestMappingInfos(){

        String customerDefinition = this.profilerConfig.readString(CUSTOMER_TXTYPE, "");
        if(StringUtils.isEmpty(customerDefinition)){
            return null;
        }else {
            return parseTxTypeItems(customerDefinition.split(";"));
        }
    }

    private List<RequestMappingInfo> parseTxTypeItems(String[] txtypes) {

        List<RequestMappingInfo> requestMappingInfos = new ArrayList<RequestMappingInfo>();

        for(String txtype : txtypes){

            int separatorIndex = txtype.indexOf("_");
            if(separatorIndex != -1){
                Set<String> methods = parseMethods(txtype.substring(0, separatorIndex));
                String pattern = parsePattern(txtype.substring(separatorIndex + 1));

                requestMappingInfos.add(new RequestMappingInfo(pattern, methods));
            }
        }


        return requestMappingInfos;
    }

    private Set<String> parseMethods(String methodsString) {
        Set<String> methods = new HashSet<String>();
        String[] methodItems = methodsString.split(",");

        for(String method : methodItems){
            methods.add(method.trim());
        }
        return methods;
    }

    private String parsePattern(String patternString) {
        return patternString;
    }

    public List<RequestMappingInfo> rules(){
        return requestMappingInfos;
    }
}
