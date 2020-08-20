package com.example.weixin.Intereptors;

import com.example.weixin.Utils.MsgUtil;
import com.example.weixin.Utils.UserInfoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class SessionInterceptor implements HandlerInterceptor {
    @Autowired
    UserInfoUtil userInfoUtil;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String session=request.getHeader("ge_session");
        if(session==null || (!userInfoUtil.isSessionValid(session))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write( new MsgUtil("errMsg","no authorization").toString());
            return false;
        }

        return true;
    }
}
