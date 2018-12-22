package com.navercorp.pinpoint.plugin.spring.mvc.interceptor;

import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;

public class RegisterInterceptor implements AroundInterceptor {

    @Override
    public void before(Object target, Object[] args) {
        System.out.println(target.getClass());
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {

    }
}
