package com.example.weixin.Utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.weixin.POJO.Order;
import com.example.weixin.POJO.UserCustomizedInfo;
import com.example.weixin.Services.DAOService;
import com.example.weixin.Services.UserFunction;
import jdk.jshell.spi.ExecutionControl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.*;


@Service
public class UserInfoUtil {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    DAOService daoService;
    @Autowired
    UserFunction userFunction;
    @Autowired
    SimpleDateFormat simpleDateFormat;

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

    public JSONArray mapOrderFields(List<Order> orderList,Set<String> fieldName){
        JSONArray res=new JSONArray();
        for(Order o : orderList){
            JSONObject oo=new JSONObject();
            for(Field f:o.getClass().getDeclaredFields()){
                if (!fieldName.contains(f.getName())|| Modifier.isStatic(f.getModifiers())) continue;
                if(!f.trySetAccessible()){
                    f.setAccessible(true);
                }
                try{
                    if(!f.getType().getSimpleName().equals("Date"))oo.put(f.getName(),f.get(o));
                    else{
                        oo.put(f.getName(),simpleDateFormat.format((Date)f.get(o)));
                    }

                }
                catch (NullPointerException | IllegalAccessException e){}
            }
            res.add(oo);
        }
        return res;
    }



}
