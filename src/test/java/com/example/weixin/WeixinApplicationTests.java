package com.example.weixin;

import com.example.weixin.Controllers.MainApi;

import com.example.weixin.Services.ChatMsgRepository;
import com.example.weixin.Services.DAOService;

import com.example.weixin.Services.DelayJobService;
import com.example.weixin.Utils.OrderIdGenerator;
import com.example.weixin.Utils.UserInfoUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.jms.core.JmsTemplate;


import javax.annotation.Resource;
import javax.jms.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class WeixinApplicationTests {
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    JmsMessagingTemplate jmsMessagingTemplate;
    @Autowired
    JmsTemplate jmsTemplate;
    @Resource
    DAOService daoService;
    @Autowired
    MainApi mainApi;
    @Autowired
    OrderIdGenerator orderIdGenerator;
    @Autowired
    UserInfoUtil userInfoUtil;
    @Autowired
    ChatMsgRepository chatMsgRepository;
    @Autowired
    SimpleDateFormat simpleDateFormat;

    @Autowired
    DelayJobService delayJobService;



    @Test
    void contextLoads() throws InterruptedException, JMSException, IOException {

//        List<Order> l = daoService.getTakenOrdersByOpenid("ohHTIszRnH64cf2XVn3fLbpSpqzI");
//        Set<String> fieldNames=new HashSet<>();
//        fieldNames.add("title");
//        fieldNames.add("content");
//        JSONArray res=userInfoUtil.mapOrderFields(l,fieldNames);
//        System.out.println(res.toString());
        //int a=daoService.getOrderStatus("QWE");




    }

}
