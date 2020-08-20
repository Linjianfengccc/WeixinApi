package com.example.weixin.Services;

import com.alibaba.fastjson.JSONObject;
import net.bytebuddy.asm.Advice;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

@Service
public class AsynchronizedQueueService {
    @Resource
    DAOService daoService;
    @Resource
    ChatMsgRepository chatMsgRepository;
    private Boolean flushing=false;
    @Autowired
    SimpleDateFormat simpleDateFormat;


    @JmsListener(destination = "flushOrders?consumer.prefetchSize=0")
    public void flushOrders_1()   {

        synchronized (flushing){
            if(flushing) {
                //System.out.println("flusher_1 does not flush");
                return;
            }
            else flushing=true;
        }
        //System.out.println("flusher_1 start flush");
        //long t1=new Date().getTime();
        daoService.flushTakeableOrders();
        //long t2=new Date().getTime();

        //System.out.println("flusher_1 flushed costs "+((double)(t2-t1))+" ms");

        flushing=false;

    }
    @JmsListener(destination = "flushOrders?consumer.prefetchSize=0")
    public void flushOrders_2() {

        synchronized (flushing){
            if(flushing) {
                //System.out.println("flusher_2 does not flush");
                return;
            }
            else flushing=true;
        }
        //System.out.println("flusher_2 start flush");
        //long t1=new Date().getTime();
        daoService.flushTakeableOrders();
        //long t2=new Date().getTime();

       // System.out.println("flusher_2 flushed costs "+((double)(t2-t1))+" ms");

        flushing=false;
    }

    @JmsListener(destination = "pushNewMsg")
    public void pushNewMsg(JSONObject msg)  {

        String from=msg.getString("from");
        String to=msg.getString("to");
        String content=msg.getString("content");
        boolean accepted=msg.getBoolean("accepted");
        String date=msg.getString("date");
        try{

            chatMsgRepository.pushNewMsg(from,to,date,content,accepted);
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }
    @JmsListener(destination = "t")
    public void t(JSONObject jsonObject){
        System.out.println(jsonObject.toJSONString());
    }


    @JmsListener(destination = "autoFinish")
    public void autoFinish(String oid){
        int status=daoService.getOrderStatus(oid);
        if(status==0) return;
        daoService.setOrderStatusFinished(oid);
        daoService.setOrderFinishTime(oid,simpleDateFormat.format(new Date()));
    }

}
