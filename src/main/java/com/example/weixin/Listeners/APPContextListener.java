package com.example.weixin.Listeners;

import com.example.weixin.Controllers.MainApi;
import com.example.weixin.Services.DAOService;
import com.example.weixin.Utils.OrderIdGenerator;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.annotation.AccessType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.io.*;

@WebListener
public class APPContextListener implements ServletContextListener {
    @Resource
    DAOService daoService;
    @Resource(name = "LocksofOrderIDs")
    LinkedHashMap<String,Object> locks;
    @Value("${spring.repository.HiconPath}")
    String userHionPath;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    OrderIdGenerator orderIdGenerator;


    @Override
    public void contextInitialized(ServletContextEvent sce) {
        //daoService.flushTakeableOrders();
        stringRedisTemplate.delete("orders::SimpleKey []");
        orderIdGenerator.initusedIDs();

        List<String> li=daoService.getOrderIDs();
        for(String orderID:li){
            locks.put(orderID,new ReentrantLock());
        }

        File userHiconPath=new File(userHionPath);
        if(!(userHiconPath.exists()&&userHiconPath.isDirectory())){
            userHiconPath.mkdir();
        }
    }
}
