package com.example.weixin.Listeners;

import com.example.weixin.Services.MessageCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;


import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RedisKeyExpiredListener extends KeyExpirationEventMessageListener {
    Pattern p=Pattern.compile(".*::to_(.*)::");
    Matcher m;
    @Autowired
    MessageCache messageCache;

    @Override
    public void onMessage(Message message, byte[] pattern) {

        String key=new String(message.getBody());
        m=p.matcher(key);
        if(!m.matches()) return;
        m=p.matcher(key);
        m.find();
        String openid=m.group(1);
        messageCache.flushNewMsgStatus(openid);

        super.onMessage(message, pattern);
    }

    /**
     * Creates new {@link MessageListener} for {@code __keyevent@*__:expired} messages.
     *
     * @param listenerContainer must not be {@literal null}.
     */
    public RedisKeyExpiredListener(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
    }
}
