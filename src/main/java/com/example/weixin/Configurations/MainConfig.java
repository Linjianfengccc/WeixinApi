package com.example.weixin.Configurations;

import com.example.weixin.Intereptors.RealityInterceptor;
import com.example.weixin.Intereptors.SessionInterceptor;
import com.example.weixin.Services.LoginServices;
import com.example.weixin.Services.LoginServicesImp;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
public class MainConfig implements WebMvcConfigurer {
    private static final StringRedisSerializer STRING_SERIALIZER = new StringRedisSerializer();
    private static final GenericJackson2JsonRedisSerializer JACKSON__SERIALIZER = new GenericJackson2JsonRedisSerializer();
//    @Bean
//    public LoginServices loginServices(){
//        return new LoginServicesImp();
//    }


//    @Bean
//    public Tmp tmp(){
//        return new Tmp();
//    }
//    @Bean
//    public Need need(){
//        return new Need();
//    }

    /**
     *提前注册interceptor
     * 因为所注册的interceptor的有成员变量需要由IOC容器注入，而interceptor的注册在SpringContext之前
     */
    @Bean
    public HandlerInterceptor getSessionInterceptor(){
        return new SessionInterceptor();
    }
    @Bean
    public RealityInterceptor getRealityInterceptor(){
        return new RealityInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(getSessionInterceptor()).addPathPatterns("/**").excludePathPatterns("/WeixinApi/login","/WeixinApi/font","/WeixinApi/orders","/WeixinApi/hicon/**","/WeixinApi/t");
        registry.addInterceptor(getRealityInterceptor()).addPathPatterns("/WeixinApi/**").excludePathPatterns("/WeixinApi/login","/WeixinApi/hicon/**","/WeixinApi/orders","/WeixinApi/whetherComplete","WeixinApi/whetherComplete","/WeixinApi/updateUserInfo","/WeixinApi/t");
    }

    @Bean
    public LoginServices loginServices(){
        return new LoginServicesImp();
    }

    @Bean

    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory factory){
        // 配置redisTemplate
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        // key序列化
        redisTemplate.setKeySerializer(STRING_SERIALIZER);
        // value序列化
        redisTemplate.setValueSerializer(JACKSON__SERIALIZER);
        // Hash key序列化
        redisTemplate.setHashKeySerializer(STRING_SERIALIZER);
        // Hash value序列化
        redisTemplate.setHashValueSerializer(JACKSON__SERIALIZER);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        //设置缓存过期时间
        RedisCacheConfiguration redisCacheCfg=RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(STRING_SERIALIZER))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(JACKSON__SERIALIZER));
        return RedisCacheManager.builder(RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory))
                .cacheDefaults(redisCacheCfg)
                .build();
    }



}
