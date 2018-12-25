package com.huawei.txtype.impl.springmvc;

import com.navercorp.pinpoint.bootstrap.context.huawei.IMappingRegistry;

import java.util.ArrayList;
import java.util.List;

public class SpringMVCMappingRegistryFactory {

    public static List<IMappingRegistry> createMappingRegistries(){
        List<IMappingRegistry> registries = new ArrayList<IMappingRegistry>();

        registries.add(new MethodHandlerRegistryMapping());
        registries.add(new UrlHandlerRegistryMapping());

        return registries;
    }
}
