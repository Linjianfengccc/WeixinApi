package com.example.weixin.Configurations;
import javax.jms.Destination;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

@Configuration
public class DAOConfig implements WebMvcConfigurer {
    @Bean(name = "LocksofOrderIDs")
    public LinkedHashMap<String, Object> linkedHashMap(){
        return new LinkedHashMap<String,Object>();
    }

//    @Bean
//    public SimpleDateFormat simpleDateFormat(){
//        return new SimpleDateFormat();
//    }

    @Bean("flushDes")
    Destination destination(){
        return new ActiveMQQueue("flushOrders");
    }



}
