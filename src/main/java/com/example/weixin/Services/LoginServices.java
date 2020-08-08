package com.example.weixin.Services;

import com.alibaba.fastjson.JSONObject;

public interface LoginServices {
    static final String secret="995349ceebbf10bf8a64abde3be4239d";
    static final String appid="wxbda85b4d946f3ecd";
    String signUpSession(String code,String encryptedData,String iv);

}
