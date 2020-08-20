package com.example.weixin.Services;

import com.alibaba.fastjson.JSONObject;
import com.example.weixin.POJO.UserCustomizedInfo;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public interface UserFunction {
    void uptateUserInfo(String openid, String  name,String nickName,String sex,String id,String sid,String sArea,String phone,String sign,String age);
    UserCustomizedInfo getUserCustomizedInfo(String openid);
    @Cacheable("NickCache")
    String getNickByOpenid(String openid);
    @CachePut("NickCache")
    String flushNickByOpenid(String openid);

    String getRealName(String openid);

};
