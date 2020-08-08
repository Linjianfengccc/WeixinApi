package com.example.weixin.Services;

import com.alibaba.fastjson.JSONObject;
import com.example.weixin.Utils.MsgUtil;
import com.example.weixin.Utils.UserInfoUtil;
import net.bytebuddy.utility.RandomString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class LoginServicesImp implements LoginServices {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private UserInfoUtil userInfoUtil;
    @Resource
    DAOService daoService;
    @Resource(name = "LocksofOrderIDs")
    LinkedHashMap<String,Object> locks;

    @Value("${spring.redis.keep_hour}")
    int hour;
    @Value("${spring.redis.keep_minute}")
    int minute;
    @Value("${spring.redis.keep_second}")
    int second;
    @Value("${spring.redis.whether_timeout}")
    boolean timeout;

    String Hash_OidToSession="Oid-Session";

    Log log= LogFactory.getLog(this.getClass());



    public JSONObject get_SessionKey_and_Openid(String code) {
        RestTemplate restTemplate=new RestTemplate();
        //HttpHeaders httpHeaders=new HttpHeaders();
        HashMap hashMap=new HashMap<String,String>();

        hashMap.put("secret",secret);
        hashMap.put("appid",appid);
        hashMap.put("js_code",code);
//        LinkedMultiValueMap linkedMultiValueMap=new LinkedMultiValueMap<String,String>();
//        linkedMultiValueMap.add("secret",secret);
//        linkedMultiValueMap.add("appid",appid);
//        linkedMultiValueMap.add("js_code",code);
//
//        HttpEntity<String> httpEntity=new HttpEntity<String>(null,linkedMultiValueMap);


        ResponseEntity<String> responseEntity=restTemplate.exchange("https://api.weixin.qq.com/sns/jscode2session?secret={secret}&appid={appid}&js_code={js_code}",HttpMethod.GET,null,String.class,hashMap);
        String resp=responseEntity.getBody();
        return JSONObject.parseObject(resp);
    }


    JSONObject decrypt(String encryptedData,String iv,String session_key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher=Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(Base64.getDecoder().decode(session_key),"AES"),new IvParameterSpec(Base64.getDecoder().decode(iv)));

        byte[] res=cipher.doFinal(Base64.getDecoder().decode(encryptedData));

        JSONObject jsonObject=JSONObject.parseObject(new String(res));

        return jsonObject;
    }

    @Override
    public String signUpSession(String code,String encryptedData,String iv) {
        JSONObject sk_Oid=get_SessionKey_and_Openid(code);
        if(sk_Oid.get("errcode")!=null) return sk_Oid.toJSONString();

        String session_key= (String) sk_Oid.get("session_key"),openid= (String) sk_Oid.get("openid");
        if(daoService.exists(openid)==null){
            daoService.signInNew(openid);
            locks.put(openid,new Object());
        }
        JSONObject deJson = null;
        if(encryptedData!=null&&iv!=null){
            try {
            deJson=decrypt(encryptedData,iv,session_key);
            }catch (Exception e){
                deJson=null;
            }
            finally {
                StringBuilder sb=new StringBuilder();
                if(deJson!=null){

                    String user=deJson.getString("nickName"),
                    province=deJson.getString("province"),
                    city=deJson.getString("city");
                    userInfoUtil.updateUserInfo(openid,user,province,city);
                    sb.append(user).append(" from ").append(city).append(" ").append(province).append(" login successfully!");
                }
                else {
                    sb.append("user with Openid:").append(openid).append(" login successfully!");
                }
                log.info(sb);

            }
        }
        String session= RandomString.make(32);
        String oldSess=(String) redisTemplate.opsForHash().get(Hash_OidToSession,openid);

        if (oldSess!=null) redisTemplate.delete(oldSess);

        while(redisTemplate.opsForHash().get("sessions",session)!=null){session= RandomString.make(32);}

        redisTemplate.opsForValue().set(session,session_key+"."+openid);
        redisTemplate.opsForHash().put(Hash_OidToSession,openid,session);


        if(timeout){
            long time=hour*3600+minute*60+second;
            redisTemplate.expire(session,time, TimeUnit.SECONDS);

        }

        return "{\"Msg\":\"successfully login\",\"session\":\""+session+"\"}";
    }
}
