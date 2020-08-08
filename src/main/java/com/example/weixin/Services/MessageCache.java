package com.example.weixin.Services;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Date;
import java.util.Map;

public interface MessageCache {

    JSONObject getCachedMsg(String openid);
    void pushNewMsg(String oid_to, JSONObject newMsg);
    @Cacheable("hasNewMsg")
    boolean hasNewMsg(String openid);
    @CachePut("hasNewMsg")
    boolean flushNewMsgStatus(String openid);
}
