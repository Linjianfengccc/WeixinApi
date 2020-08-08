package com.example.weixin.Services;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.weixin.Utils.UserInfoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Service
public class MessageCacheImp implements MessageCache {
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    SimpleDateFormat simpleDateFormat;
    @Autowired
    UserInfoUtil userInfoUtil;
    @Value("${spring.redis.chatMsg_timeout}")
    int timeout;
    @Value("${spring.redis.msgCachePrefix}")
    String msgCachePrefix;

    @Override
    public void pushNewMsg(String oid_to, JSONObject newMsg) {
        JSONObject oldMsgCache=(JSONObject)redisTemplate.opsForValue().get(msgCachePrefix+oid_to);
        oldMsgCache=(oldMsgCache==null?new JSONObject():oldMsgCache);
        //String from=((JSONObject) newMsg.get("from")).getString("openid");
        JSONObject from=(JSONObject)newMsg.get("from");

        JSONArray from_someone=(JSONArray)oldMsgCache.get(from.toJSONString());
        from_someone=from_someone==null?new JSONArray():from_someone;

        newMsg.remove("from");
        newMsg.remove("to");
        from_someone.add(newMsg);
        oldMsgCache.put(from.toJSONString(),from_someone);


        redisTemplate.opsForValue().set(msgCachePrefix+oid_to,oldMsgCache);
        redisTemplate.expire(msgCachePrefix+oid_to,timeout, TimeUnit.SECONDS);
        flushNewMsgStatus((String)from.get("openid"));


    }

    @Override
    public boolean hasNewMsg(String openid) {
        return !(redisTemplate.opsForValue().get(msgCachePrefix+openid)==null);
    }

    @Override
    public boolean flushNewMsgStatus(String openid) {
        return !(redisTemplate.opsForValue().get(msgCachePrefix+openid)==null);
    }

    @Override
    public JSONObject getCachedMsg(String openid) {
        JSONObject msgCache=(JSONObject)(redisTemplate.opsForValue().get(msgCachePrefix+openid));
        redisTemplate.delete(msgCachePrefix+openid);
        flushNewMsgStatus(openid);
        return msgCache;
    }
}
