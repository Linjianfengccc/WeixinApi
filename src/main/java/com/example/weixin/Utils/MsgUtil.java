package com.example.weixin.Utils;

import com.alibaba.fastjson.JSONObject;

public class MsgUtil {
    private JSONObject errJSON=new JSONObject();
    public MsgUtil(String key, String value){
        errJSON.put(key,value);
    }

    @Override
    public String toString() {
        return errJSON.toJSONString();
    }
}
