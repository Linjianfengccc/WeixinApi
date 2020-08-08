package com.example.weixin.Utils;

import com.alibaba.fastjson.JSONObject;
import com.example.weixin.POJO.UserCustomizedInfo;
import com.example.weixin.Services.DAOService;
import com.example.weixin.Services.UserFunction;
import jdk.jshell.spi.ExecutionControl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.TimeUnit;


@Service
public class UserInfoUtil {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    DAOService daoService;
    @Autowired
    UserFunction userFunction;

    /**
     *
     * @param session ge_session in redis
     * @param INFO
     * @return
     */
    public String getUserInfo(String session,INFO INFO)throws NullPointerException{
        String s_o=stringRedisTemplate.opsForValue().get(session);
        if(s_o==null) throw new NullPointerException();
        switch (INFO){
        case SESSION_KEY:return s_o.split("\\.")[0];
        case OPENID:return s_o.split("\\.")[1];
    }
        return null;
}
    public enum INFO{
        SESSION_KEY,OPENID;
    }

    public boolean isSessionValid(String session){
        if(stringRedisTemplate.opsForValue().get(session)!=null) return true;
        return false;
    }

    public void updateUserInfo(String openid,String nickName,String province, String city){
        daoService.updateUserInfo(openid,nickName,province,city);
    }

    public void updateCustomizedUserInfo(String openid, JSONObject newInfo) throws UserInfoException {
        UserCustomizedInfo odlUCInfo=userFunction.getUserCustomizedInfo(openid);
        String nickName=newInfo.getString("nickName")
                ,sid=newInfo.getString("sid")
                ,name=newInfo.getString("name")
                ,sex=newInfo.getString("sex")
                ,sArea=newInfo.getString("sArea")
                ,phone=newInfo.getString("phone")
                ,id=newInfo.getString("id")
                ,sign=newInfo.getString("sign")
                ,age=newInfo.getString("age");
        try{
            userFunction.uptateUserInfo(openid,name,nickName,sex,id,sid,sArea,phone,sign,age);
            if(nickName!=null){
                flushNickByOpenid(openid);
            }
        }
        catch (Exception e){
            //e.printStackTrace();
            throw new UserInfoException(openid+"更新个人信息失败");

        }
        flushNickByOpenid(openid);
    }
    public class UserInfoException extends IOException {
        @Override
        public String getMessage() {
            return "{errMsg:\"Update Failed\"}";
        }

        public UserInfoException(String message) {
            super(message);
        }
    }

    public String getNickByOpenid(String openid){
        return userFunction.getNickByOpenid(openid);
    }
    public void flushNickByOpenid(String openid){
        userFunction.flushNickByOpenid(openid);
    }
    public UserCustomizedInfo getUserCustomizedInfo(String openid){
        return userFunction.getUserCustomizedInfo(openid);
    }





}
