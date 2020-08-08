package com.example.weixin.Utils;


import com.example.weixin.Services.DAOService;
import org.checkerframework.checker.units.qual.A;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.UUID;

@Component
public class OrderIdGenerator {
    LinkedHashSet<String> usedIDs=new LinkedHashSet<>();
    @Resource
    DAOService daoService;
    SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyyMMddHHmm");

    private boolean inited=false;
    public void initusedIDs(){
        if(!inited){
            inited=true;
            List<String> ids=daoService.getusedIDs();
            for(String id:ids){
                usedIDs.add(id);
            }
        }

    }
    public String generate(){
        String oid = null;
        do{
            oid=simpleDateFormat.format(new Date());
            for(int i=0;i<16;i++){
                oid+=(int)(Math.random()*10);
            }
        }
        while(usedIDs.contains(oid));
        return oid;
    }

}
