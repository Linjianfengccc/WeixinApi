package com.example.weixin.Services;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.example.weixin.POJO.ChatMsg;
import com.example.weixin.Utils.SpringContextUtil;
import com.example.weixin.Utils.UserInfoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.web.socket.*;
import org.springframework.web.socket.adapter.standard.StandardWebSocketSession;

import javax.annotation.Resource;
import java.util.List;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public class ChatHandler implements WebSocketHandler {
    private static Map<String, WebSocketSession> onlineUsers;
    private static MessageCache messageCache;
    private static JmsMessagingTemplate jmsMessagingTemplate;
    private static SimpleDateFormat simpleDateFormat;
    private static UserInfoUtil userInfoUtil;
    private static StringRedisTemplate stringRedisTemplate;
    private static DAOService daoService;
    @Value("${spring.redis.msgCachePrefix}")
    String msgCachePrefix;

    public static void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        ChatHandler.stringRedisTemplate = stringRedisTemplate;
    }

    public static void setDaoService(DAOService daoService) {
        ChatHandler.daoService = daoService;
    }

    public static void setUserInfoUtil(UserInfoUtil userInfoUtil) {
        ChatHandler.userInfoUtil = userInfoUtil;
    }

    public static void setSimpleDateFormat(SimpleDateFormat simpleDateFormat) {
        ChatHandler.simpleDateFormat = simpleDateFormat;
    }

    public static void setJmsMessagingTemplate(JmsMessagingTemplate jmsMessagingTemplate) {
        ChatHandler.jmsMessagingTemplate = jmsMessagingTemplate;
    }

    public static void setMessageCache(MessageCache messageCache) {
        ChatHandler.messageCache = messageCache;
    }

    public static void setOnlineUsers(Map<String, WebSocketSession> onlineUsers) {
        ChatHandler.onlineUsers = onlineUsers;
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession webSocketSession) {

        String ge_session = webSocketSession.getHandshakeHeaders().get("ge_session").get(0);
        String openid = SpringContextUtil.getAppContext().getBean(UserInfoUtil.class).getUserInfo(ge_session, UserInfoUtil.INFO.OPENID);
        onlineUsers.put(openid, webSocketSession);
//        for (Map.Entry<String,ChatHandler> e:onlineUsers.entrySet()){
//            System.out.println(e.getKey()+":"+e.getValue().openid+" ge_session:"+e.getValue().ge_session+"   id:"+e.getValue());
//        }
//        if (onlineUsers.size()==2){
//            System.out.println(onlineUsers.get("ohHTIszRnH64cf2XVn3fLbpSpqzI")==onlineUsers.get("ohHTIs3w8NKGsoZZvA50e-xd5zrA"));
//        }
        System.out.println("Online User: " + onlineUsers.size());
        if (messageCache.hasNewMsg(openid)) {
            JSONObject newMsgs = messageCache.getCachedMsg(openid);
            try{
                System.out.println(webSocketSession==null);
                webSocketSession.sendMessage(new TextMessage(newMsgs.toJSONString()));
                daoService.flushNewMsgStatus(openid);
                messageCache.flushNewMsgStatus(openid);
            }
            catch (IOException e){
                //stringRedisTemplate.opsForValue().set(msgCachePrefix+openid,newMsgs.toJSONString());
            }
        }
        else{

            List<ChatMsg> newMsgOnDisk=daoService.searchNewMsgFromDisk(openid);
            //System.out.println(newMsgOnDisk.toString());
            if (newMsgOnDisk.size()!=0){
                JSONObject newMsg=new JSONObject();
                for(ChatMsg msg:newMsgOnDisk){
                    JSONObject key=new JSONObject();
                    String fromOpenid=msg.getFrom();
                    key.put("nickName",userInfoUtil.getNickByOpenid(fromOpenid));
                    key.put("openid",fromOpenid);
                    String _Key=key.toJSONString();
                    if(newMsg.get(_Key)==null){
                        newMsg.put(_Key,new JSONArray());
                    }
                    JSONObject fromOne=new JSONObject();
                    fromOne.put("date",simpleDateFormat.format(msg.getTime()));
                    fromOne.put("content",msg.getContent());
                    ((JSONArray)newMsg.get(_Key)).add(fromOne);

                }
                try{
                    webSocketSession.sendMessage(new TextMessage(newMsg.toJSONString()));
                    daoService.flushNewMsgStatus(openid);
                }
                catch (Exception E){}
            }

        }
    }


    @Override
    public void handleMessage(WebSocketSession webSocketSession, WebSocketMessage<?> webSocketMessage) {

        String ge_session = webSocketSession.getHandshakeHeaders().get("ge_session").get(0);
        JSONObject message = null;
        String to = null, content = null,fromOpenid=userInfoUtil.getUserInfo(ge_session, UserInfoUtil.INFO.OPENID);
        try {
            message = JSONObject.parseObject(webSocketMessage.getPayload().toString());
            to = message.getString("to");
            content = message.getString("content");
            if (to == null || content == null) throw new JSONException();
            if (to.equals("") || content.equals("")) throw new JSONException();
        } catch (Exception e) {
            try {
                webSocketSession.sendMessage(new TextMessage("{\"errMsg\":\"wrong msg format\"}"));

            } catch (Exception ec) {
            }
            e.printStackTrace();
            return;
        }
        WebSocketSession des = onlineUsers.get(to);
        JSONObject fromInfo=new JSONObject();
        fromInfo.put("nickName",userInfoUtil.getNickByOpenid(fromOpenid));
        fromInfo.put("openid",fromOpenid);
        message.put("from", fromInfo);
        message.put("date", simpleDateFormat.format(new Date()));
        if (des == null) {
            //TODO: 消息写入缓存
            messageCache.pushNewMsg(to, message);
            messageCache.flushNewMsgStatus(to);
            message.put("accepted", false);
        } else {
            JSONObject dealt_mes = new JSONObject();
            JSONArray msgs = new JSONArray();
            JSONObject info = new JSONObject();
            info.put("content", message.get("content"));
            info.put("date", message.get("date"));
            msgs.add(info);
            JSONObject from=new JSONObject();
            from.put("nickName",userInfoUtil.getNickByOpenid(fromOpenid));
            from.put("openid",fromOpenid);
            dealt_mes.put(from.toJSONString(), msgs);
            try {

                des.sendMessage(new TextMessage(dealt_mes.toJSONString()));
                //System.out.println("send to "+des.openid+" to:"+to);
                message.put("accepted", true);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        //TODO: 消息持久化
        message.put("from",fromInfo.getString("openid"));
        message.put("to",to);
        jmsMessagingTemplate.convertAndSend("pushNewMsg", message);


    }

    @Override
    public void handleTransportError(WebSocketSession webSocketSession, Throwable throwable) {

    }

    @Override
    public void afterConnectionClosed(WebSocketSession webSocketSession, CloseStatus closeStatus) {
        String ge_session = webSocketSession.getHandshakeHeaders().get("ge_session").get(0);
        try{
            String openid = SpringContextUtil.getAppContext().getBean(UserInfoUtil.class).getUserInfo(ge_session, UserInfoUtil.INFO.OPENID);
            onlineUsers.remove(openid);
        }
        catch (NullPointerException e){}


    }

    @Override
    public boolean supportsPartialMessages() {
        return true;
    }


}