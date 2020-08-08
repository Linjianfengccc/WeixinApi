package com.example.weixin.Utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class SpringContextUtil implements ApplicationContextAware {
    private static ApplicationContext applicationContext;
    private static boolean contextSet=false;

    @Override
    public void setApplicationContext(ApplicationContext _applicationContext) throws BeansException {
        if(contextSet) return;
        applicationContext=_applicationContext;
        contextSet=true;
    }
    public static ApplicationContext getAppContext(){
        return applicationContext;
    }

}
