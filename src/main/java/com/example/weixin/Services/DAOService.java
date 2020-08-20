package com.example.weixin.Services;

import com.example.weixin.POJO.ChatMsg;
import com.example.weixin.POJO.Order;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;


import java.util.Date;
import java.util.List;

@Mapper
public interface DAOService {
    @Cacheable("orders")
    List<Order> getTakeableOrders();
    @CachePut("orders")
    List<Order> flushTakeableOrders();
    String exists(String openid);
    void signInNew(String openid);
    List<String> getOrderIDs();
    void takeOrder(String openId, String oid, String date);
    int getOrderStatus(String oid);
    boolean updateOrderStatus(String oid,int newStatus);
    List<String> getusedIDs();
    void createOrder(String oid,String title,String content,Double price);
    void submitOrder(String oid,String openid,String date);
    int submittedNumber(String openid);
    //@Cacheable("submittedOrders")
    List<Order> getSubmittedOrders(String openid);
    @CachePut("submittedOrders")
    List<Order>flushSubmittedOrders(String openid);
    @Cacheable("ensureAdmin")
    int ensureAdmin(String openid,String oid);
    void cancleOrder(String oid);
    String getOrderOwner(String oid);
    void updateUserInfo(String openid,String nickName,String province,String city);
    String getNickName(String openid);
    void flushNewMsgStatus(String openid);
    List<ChatMsg> searchNewMsgFromDisk(String openid);
    List<Order> getTakenOrdersByOpenid(String openid);
    Order checkSubmitAdmin(String openid,String oid);
    void setOrderStatusFinished(String oid);
    void setOrderFinishTime(String oid, String f_time);
    String checkTaker(String oid);
}
