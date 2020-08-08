package com.example.weixin.Configurations;

import com.example.weixin.Listeners.RedisKeyExpiredListener;
import com.example.weixin.Services.ChatHandler;
import com.example.weixin.Services.DAOService;
import com.example.weixin.Services.MessageCache;
import com.example.weixin.Services.MessageCacheImp;
import com.example.weixin.Utils.SpringContextUtil;
import com.example.weixin.Utils.UserInfoUtil;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableWebSocket
public class WebSocketConfig  implements WebSocketConfigurer {
    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer() {
        RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
        redisMessageListenerContainer.setConnectionFactory(redisConnectionFactory);
        return redisMessageListenerContainer;
    }

    @Bean
    RedisKeyExpiredListener redisKeyExpiredListener(){
        return new RedisKeyExpiredListener(redisMessageListenerContainer());
    }
    @Bean
    MessageCache messageCache(){
        return new MessageCacheImp();
    }
    @Autowired
    JmsMessagingTemplate jmsMessagingTemplate;

    @Bean
    public SimpleDateFormat simpleDateFormat(){
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    @Autowired
    UserInfoUtil userInfoUtil;
    @Resource
    DAOService daoService;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
        ChatHandler.setUserInfoUtil(userInfoUtil);
        ChatHandler.setJmsMessagingTemplate(jmsMessagingTemplate);
        ChatHandler.setMessageCache(messageCache());
        ChatHandler.setOnlineUsers(new ConcurrentHashMap<String, WebSocketSession>());
        ChatHandler.setSimpleDateFormat(simpleDateFormat());
        ChatHandler.setDaoService(daoService);
        ChatHandler.setStringRedisTemplate(stringRedisTemplate);
        webSocketHandlerRegistry.addHandler(new ChatHandler(),"/chat").setAllowedOrigins("*").addInterceptors(new ChatHandShakeInterceptor());
    }

    class ChatHandShakeInterceptor implements HandshakeInterceptor {


        @Override
        public boolean beforeHandshake(ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse, WebSocketHandler webSocketHandler, Map<String, Object> map) throws Exception {
            if(serverHttpRequest.getHeaders()==null||serverHttpRequest.getHeaders().get("ge_session")==null) return false;
            String session=serverHttpRequest.getHeaders().get("ge_session").get(0);
            UserInfoUtil userInfoUtil= SpringContextUtil.getAppContext().getBean(UserInfoUtil.class);
            if(userInfoUtil.isSessionValid(session)) return true;
            return false;

        }

        @Override
        public void afterHandshake(ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse, WebSocketHandler webSocketHandler, Exception e) {

        }

    }




}
