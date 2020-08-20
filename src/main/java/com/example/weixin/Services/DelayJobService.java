package com.example.weixin.Services;

import com.example.weixin.Listeners.APPContextListener;
import com.example.weixin.Utils.SpringContextUtil;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.text.SimpleDateFormat;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;


@Service
public class DelayJobService   {

    @Autowired
    public void initJms(JmsMessagingTemplate jmsMessagingTemplate){
        AutoFinishOrderDelayed.jmsMessagingTemplate=jmsMessagingTemplate;
    }

    private DelayQueue<AutoFinishOrderDelayed> delayElems;

    private DelayJobService(){

        this.delayElems=new DelayQueue<>();
        this.startService();
    }

    public void add(AutoFinishOrderDelayed delayed){

        delayElems.add(delayed);

    }

    private boolean startService(){
        try{
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true){
                        try {
                            AutoFinishOrderDelayed autoFinishOrderDelayed=delayElems.take();
                            autoFinishOrderDelayed.run();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
        catch (Exception e ){
            return false;

        }
        return true;


    }




    public static class AutoFinishOrderDelayed implements Delayed,Runnable {
        Long expire;
        String oid;
        static JmsMessagingTemplate jmsMessagingTemplate;

        public AutoFinishOrderDelayed(Long expire, TimeUnit timeUnit, String oid){
            this.expire=TimeUnit.MILLISECONDS.convert(expire,timeUnit)+System.currentTimeMillis();
            this.oid=oid;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(expire-System.currentTimeMillis(),TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return (int)(this.getDelay(TimeUnit.MILLISECONDS)-o.getDelay(TimeUnit.MILLISECONDS));
        }

        @Override
        public void run() {
            jmsMessagingTemplate.convertAndSend("autoFinish",oid);
        }
    }



}
