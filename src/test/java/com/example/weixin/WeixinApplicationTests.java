package com.example.weixin;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.weixin.Controllers.MainApi;

import com.example.weixin.Services.ChatMsgRepository;
import com.example.weixin.Services.DAOService;

import com.example.weixin.Utils.OrderIdGenerator;
import com.example.weixin.Utils.UserInfoUtil;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.jms.core.JmsTemplate;


import javax.annotation.Resource;
import javax.jms.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
    @Test
    void contextLoads() throws InterruptedException, JMSException, IOException {
        ClassPathResource classPathResource=new ClassPathResource("/UserImgs");

        String path=classPathResource.getURL().getPath();
        System.out.println(path);
        File f=new File(path+"/test");
        if(!f.exists()){
            new FileOutputStream(f).write("fuck".getBytes());
        }





    }

}
