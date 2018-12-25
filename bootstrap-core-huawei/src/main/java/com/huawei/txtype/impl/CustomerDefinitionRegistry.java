package com.huawei.txtype.impl;

import com.huawei.txtype.config.TxTypeConfiguration;
import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.context.huawei.IRequestMappingInfo;

public class CustomerDefinitionRegistry extends DefaultRegistryMapping {

    private TxTypeConfiguration txTypeConfiguration;

    public CustomerDefinitionRegistry(ProfilerConfig profilerConfig) {
        this.txTypeConfiguration = new TxTypeConfiguration(profilerConfig);
    }


    @Override
    public IRequestMappingInfo match(String uri, String method) {
        return super.match(txTypeConfiguration.rules(), uri, method);
    }
}
