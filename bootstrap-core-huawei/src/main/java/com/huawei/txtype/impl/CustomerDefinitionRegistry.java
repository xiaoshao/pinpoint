package com.huawei.txtype.impl;

import com.huawei.txtype.RequestMappingInfo;
import com.huawei.txtype.config.TxTypeConfiguration;
import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;

public class CustomerDefinitionRegistry extends DefaultRegistryMapping {

    public static final int CUSTOMER_REGISTRY_LEVEL = 0;
    private TxTypeConfiguration txTypeConfiguration;

    public CustomerDefinitionRegistry(ProfilerConfig profilerConfig) {
        this.txTypeConfiguration = new TxTypeConfiguration(profilerConfig);
    }


    @Override
    public RequestMappingInfo match(String uri, String method) {
        return super.match(txTypeConfiguration.rules(), uri, method);
    }
}
