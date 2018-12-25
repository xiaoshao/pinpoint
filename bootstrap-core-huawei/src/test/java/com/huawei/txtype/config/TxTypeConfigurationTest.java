package com.huawei.txtype.config;

import com.huawei.txtype.RequestMappingInfo;
import com.navercorp.pinpoint.bootstrap.config.DefaultProfilerConfig;
import com.navercorp.pinpoint.bootstrap.context.huawei.IRequestMappingInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static com.huawei.txtype.config.TxTypeConfiguration.CUSTOMER_TXTYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TxTypeConfigurationTest {

    @Mock
    DefaultProfilerConfig profilerConfig;


    @Test
    public void should_parse_the_txtype_correctly() {
       when(profilerConfig.readString(CUSTOMER_TXTYPE, "")).thenReturn("GET,POST_/hi/{name}");

       TxTypeConfiguration txTypeConfiguration = new TxTypeConfiguration(profilerConfig);
       List<IRequestMappingInfo> requestMappingInfos =  txTypeConfiguration.rules();

       assertThat(requestMappingInfos, is(createRequestMappingInfos(new RequestMappingInfo("/hi/{name}", "GET", "POST"))));
    }

    @Test
    public void should_ignore_the_invalid_txtype() {
        when(profilerConfig.readString(CUSTOMER_TXTYPE, "")).thenReturn("/hi/{name}; GET,POST_/hi/{name}");

        TxTypeConfiguration txTypeConfiguration = new TxTypeConfiguration(profilerConfig);
        List<IRequestMappingInfo> requestMappingInfos =  txTypeConfiguration.rules();

        assertThat(requestMappingInfos, is(createRequestMappingInfos(new RequestMappingInfo("/hi/{name}", "GET", "POST"))));
    }

    private List<IRequestMappingInfo> createRequestMappingInfos(IRequestMappingInfo ... requestMappingInfos){
        return Arrays.asList(requestMappingInfos);
    }
}