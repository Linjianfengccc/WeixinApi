package com.example.weixin.Intereptors;

import com.example.weixin.Utils.MsgUtil;
import com.example.weixin.Utils.UserInfoUtil;
import org.apache.catalina.User;
import org.apache.ibatis.jdbc.Null;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
public class RealityInterceptor implements HandlerInterceptor {
    @Autowired
    UserInfoUtil userInfoUtil;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ge_session=request.getHeader("ge_session");
        String openid;
        try{
            openid=userInfoUtil.getUserInfo(ge_session, UserInfoUtil.INFO.OPENID);
        }
        catch (NullPointerException e){
            response.getWriter().write(new MsgUtil("errMsg","no authorization").toString());
            return false;
        }
        if(!userInfoUtil.hasSignUpReality(openid)){
            response.getWriter().write(new MsgUtil("errMsg","not complete real info yet!").toString());
            return false;
        }
        return true;
    }
}
