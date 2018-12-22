package com.huawei.txtype;

import com.navercorp.pinpoint.bootstrap.config.DefaultProfilerConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.huawei.txtype.config.TxTypeConfiguration.CUSTOMER_TXTYPE;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RegistryMapingTest {

    @Mock
    DefaultProfilerConfig profilerConfig;

    @Test
    public void should_custom_first() {
        when(profilerConfig.readString(CUSTOMER_TXTYPE, "")).thenReturn("GET,POST_/name/{custom}");

        IMappingRegistry registryMaping = new RegistryMaping(profilerConfig);
        registryMaping.register(new RequestMappingInfo("/name/{mvc}", "GET", "POST"), 1);

        RequestMappingInfo requestMappingInfo = registryMaping.match("/name/hi", "GET");

        assertThat(requestMappingInfo, is(new RequestMappingInfo("/name/{custom}", "POST", "GET")));
    }

    @Test
    public void should_match_with_mvc_registry_when_no_match_in_custom_registry() {
        when(profilerConfig.readString(CUSTOMER_TXTYPE, "")).thenReturn("GET,POST_/name");

        IMappingRegistry registryMaping = new RegistryMaping(profilerConfig);
        registryMaping.register(new RequestMappingInfo("/name/{mvc}", "GET", "POST"), 1);

        RequestMappingInfo requestMappingInfo = registryMaping.match("/name/hi", "POST");

        assertThat(requestMappingInfo, is(new RequestMappingInfo("/name/{mvc}", "POST", "GET")));
    }

    @Test
    public void should_match_with_mvc_registry_when_cutom_registry_is_empty() {
        IMappingRegistry registryMaping = new RegistryMaping(profilerConfig);
        registryMaping.register(new RequestMappingInfo("/name/{mvc}", "GET", "POST"), 1);

        RequestMappingInfo requestMappingInfo = registryMaping.match("/name/hi", "POST");

        assertThat(requestMappingInfo, is(new RequestMappingInfo("/name/{mvc}", "POST", "GET")));
    }

}