package com.example.weixin.Controllers;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;

import com.example.weixin.POJO.Order;
import com.example.weixin.POJO.UserCustomizedInfo;
import com.example.weixin.Services.DAOService;
import com.example.weixin.Services.DelayJobService;
import com.example.weixin.Services.LoginServices;
import com.example.weixin.Utils.MsgUtil;
import com.example.weixin.Utils.OrderIdGenerator;
import com.example.weixin.Utils.UserInfoUtil;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.jms.Destination;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@RestController()
@RequestMapping("/WeixinApi")
public class MainApi {
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    LoginServices loginServices;
    @Resource
    DAOService daoService;
    @Autowired
    UserInfoUtil userInfoUtil;
    @Autowired
    SimpleDateFormat simpleDateFormat;
    @Resource(name="LocksofOrderIDs")
    LinkedHashMap<String, Object> locks;
    @Resource(name = "flushDes")
    Destination destination;
    @Autowired
    JmsTemplate jmsTemplate;
    @Value("${spring.repository.HiconPath}")
    String userHiconPath;
    @Autowired
    OrderIdGenerator oidGenerator;
    private static final int maxSubmit=20;
    private static final int maxTake=10;



    @PostMapping("/login")
    public String login(@RequestBody String body, HttpServletResponse httpServletResponse){
        JSONObject jsonObject;
        try{
             jsonObject=JSONObject.parseObject(body);
        }
        catch (Exception e){
            return new MsgUtil("errMsg","data wrong format").toString();

        }
        String code= (String) jsonObject.get("code"),encryptedData=(String)jsonObject.get("encryptedData"),iv=(String)jsonObject.get("iv");
        if(code==null) {
            return new MsgUtil("errMsg","code should not be null").toString();
        }
        return loginServices.signUpSession(code,encryptedData,iv);

    }

    @GetMapping("/orders")
    public List getOrders(){
        List<Order> res=daoService.getTakeableOrders();
        List list=new ArrayList<JSONObject>();

        for(Order o:res){
            JSONObject jsonObject=new JSONObject();
            jsonObject.put("oid",o.getoId());
            jsonObject.put("title",o.getTitle());
            jsonObject.put("content",JSONObject.parse(o.getoContent()));
            jsonObject.put("price",o.getPrice());
            jsonObject.put("dateTime",simpleDateFormat.format(o.getDate()));
            jsonObject.put("submitterNick",o.getSubmitterNick());
            jsonObject.put("sOpenid",o.getsOpenid());
            list.add(jsonObject);
        }
        return list;

    }

    @GetMapping("/takeOrder")
    public String takeOrder(HttpServletRequest httpServletRequest, @Param("oid") String oid){
        if(oid==null) return new MsgUtil("errMsg","null oid").toString();
        Object lock=locks.get(oid);
        if(lock==null) return new MsgUtil("errMsg","订单号错误或订单已被取消/接手").toString();
        String ge_session=httpServletRequest.getHeader("ge_session");
        String openId=userInfoUtil.getUserInfo(ge_session, UserInfoUtil.INFO.OPENID);

        synchronized (lock){
            //daoService.flushTakeableOrders();
            try{
                if (daoService.ensureAdmin(openId, oid) != 0)return new MsgUtil("errMsg","不能接手自己的订单").toString();
                if(daoService.getOrderStatus(oid)!=Order.ORDER_STATUS_AVALIABLE)return new MsgUtil("errMsg","订单号错误或订单已被取消/接手").toString();
                daoService.updateOrderStatus(oid,Order.ORDER_STATUS_TAKEN);
            }catch (Exception e){
                return new MsgUtil("errMsg","请求错误").toString();
            }
        }
        locks.remove(oid);
        daoService.takeOrder(openId,oid,simpleDateFormat.format(new Date()));
        jmsTemplate.convertAndSend(destination,"flush");
        String submitter=daoService.getOrderOwner(oid);
        daoService.flushSubmittedOrders(submitter);
        return new MsgUtil("msg","接单成功").toString();




    }

    @PostMapping("submit_order")
    public String submitOrder(HttpServletRequest httpServletRequest,@RequestBody JSONObject order){
        String title=(String)order.get("title");
        JSONObject content=new JSONObject();
        String detail=order.getString("content");
        content.put("org",order.getString("org"));
        content.put("des",order.getString("des"));
        content.put("detail",detail);

        if(title==null||title.length()==0) return new MsgUtil("errMsg","标题不能为空").toString();
        if(detail==null||detail.length()==0)return new MsgUtil("errMsg","订单内容不能为空").toString();
        if(title.length()>15) return new MsgUtil("errMsg","标题不能超过15个字符").toString();
        if(detail.length()>200)return new MsgUtil("errMsg","订单内容不能超过200个字符").toString();
        Double price;

        try{
            price=Double.parseDouble(order.getString("price"));
        }
        catch (NumberFormatException e){
            return "{\"errMsg\":\"wrong price format!\"}";
        }
        if(price<=0) return new MsgUtil("errMsg","价格不能为非正数").toString();
        String openid=userInfoUtil.getUserInfo(httpServletRequest.getHeader("ge_session"), UserInfoUtil.INFO.OPENID);
        String oid=oidGenerator.generate();
        if(daoService.submittedNumber(openid)>maxSubmit) return "{\"errMsg\":\"超过最大下单数（"+maxSubmit+"）!\"}";

        daoService.submitOrder(oid,openid,simpleDateFormat.format(new Date()));
        daoService.createOrder(oid,title,content.toJSONString(),price);
        daoService.flushSubmittedOrders(openid);
        jmsTemplate.convertAndSend(destination,"");
        locks.put(oid,new ReentrantLock());
        return "{\"msg\":\"下单成功!\"}";
    }

    @GetMapping("/submittedOrders")
    public JSONArray getSubmittedOrders(HttpServletRequest httpServletRequest){
        List<Order> orders=daoService.getSubmittedOrders(userInfoUtil.getUserInfo(httpServletRequest.getHeader("ge_session"), UserInfoUtil.INFO.OPENID));

        JSONArray res=new JSONArray();
        if(orders.size()==0){
            res.add(JSONObject.parse(new MsgUtil("errMsg","openid ERR").toString()));
            return res;
        }

        for(Order o:orders){
            JSONObject oo=new JSONObject();
            for(Field f:o.getClass().getDeclaredFields()){
                if(Modifier.isStatic(f.getModifiers()) ||f.getName().equals("sOpenid")||f.getName().equals("submitterNick")||f.getName().equals("submitterTel")) continue;
                if(!f.trySetAccessible()){
                    f.setAccessible(true);
                }
                try{
                    if(!f.getType().getSimpleName().equals("Date")){
                        if(!f.getName().equals("oContent"))oo.put(f.getName(),f.get(o));
                        else oo.put(f.getName(),JSONObject.parse((String)f.get(o)));
                    }
                    else{
                        Date d=(Date)f.get(o);
                            oo.put(f.getName(),d==null?d:simpleDateFormat.format((Date)f.get(o)));

                    }
                }
                catch (IllegalAccessException e){
                    res.clear();
                    res.add(JSONObject.parse(new MsgUtil("errMsg","unknown ERR").toString()));
                    return res;
                }
            }
            res.add(oo);
        }
        return res;
    }

    @GetMapping("takenOrders")
    public JSONArray getTakenOrders(@RequestHeader("ge_session")String ge_session){
        String openid=userInfoUtil.getUserInfo(ge_session, UserInfoUtil.INFO.OPENID);
        List<Order> orders=daoService.getTakenOrdersByOpenid(openid);
        Set<String> fieldName=new HashSet<>();
        fieldName.add("oId");
        fieldName.add("sOpenid");
        fieldName.add("submitterNick");
        fieldName.add("price");
        fieldName.add("date");
        fieldName.add("tDate");
        fieldName.add("title");
        fieldName.add("oContent");
        fieldName.add("status");
        fieldName.add("fDate");
        fieldName.add("submitterTel");
        fieldName.add("cDate");
        fieldName.add("canceler");

        return userInfoUtil.mapOrderFields(orders,fieldName);

    }

    @GetMapping("/cancel")
    public String cancle(HttpServletRequest httpServletRequest,@Param("oid") @NotNull String oid){
        if(oid==null) return new MsgUtil("errMsg","null Oid!").toString();
        String openid=userInfoUtil.getUserInfo(httpServletRequest.getHeader("ge_session"), UserInfoUtil.INFO.OPENID);
        String canceler=null;
        if(daoService.ensureAdmin(openid,oid)!=0){
            canceler="submitter";
        }
        else if(daoService.checkTaker(oid).equals(openid)){
            canceler="taker";
        }

        if(canceler==null){
            return new MsgUtil("errMsg","admin refused").toString();
        }
//
//        Object lock=locks.get(oid);
//        if(lock==null)return new MsgUtil("errMsg","订单处于无法被取消的状态").toString();
//        synchronized (lock){
//            if(daoService.getOrderStatus(oid)!=Order.ORDER_STATUS_AVALIABLE){
//                return new MsgUtil("errMsg","订单处于无法被取消状态").toString();
//            }
//            try{
//                daoService.cancleOrder(oid);
//            }catch (Exception e){
//                return new MsgUtil("errMsg","请求错误").toString();
//            }
//        }
        int status=daoService.getOrderStatus(oid);
        if(!(status==Order.ORDER_STATUS_TAKEN ||status==Order.ORDER_STATUS_AVALIABLE)){
            return new MsgUtil("errMsg","unable to cancel").toString();
        }
        daoService.cancleOrder(oid);
        daoService.pushCancleRecord(oid,canceler,simpleDateFormat.format(new Date()));
        jmsTemplate.convertAndSend(destination,"");
        locks.remove(oid);
        daoService.flushSubmittedOrders(openid);
        return new MsgUtil("msg","订单取消成功").toString();


    }
    @GetMapping("/openid")
    public JSONObject getOpenidOf(HttpServletRequest httpServletRequest){
        String session=httpServletRequest.getHeader("ge_session");
        if(!userInfoUtil.isSessionValid(session)) return null;
        else{
            JSONObject j=new JSONObject();
            j.put("openid",userInfoUtil.getUserInfo(session, UserInfoUtil.INFO.OPENID));
            return j;
        }
    }

    @PostMapping("/updateUserInfo")
    public JSONObject updateUserCustomizedInfo(@RequestBody String body, @RequestHeader String ge_session){
        JSONArray request=null;
        try {
           request = JSONArray.parseArray(body);
        }catch (JSONException e){
            return JSONObject.parseObject(new MsgUtil("errMsg","wrong format data").toString());
        }
        JSONObject newInfo=new JSONObject(),proMap= JSON.parseObject("{昵称:'nickName',真实姓名:'name',性别:'sex',绑定学号:'sid',所在校区:'sArea',联系方式:'phone',身份证号:'id',个性签名:'sign',年龄:'age'}");
        for (Object dic : request){
            JSONObject infoDic=(JSONObject)dic;
            String prop=proMap.getString(infoDic.getString("attr"));
            newInfo.put(prop,infoDic.get("info"));
        }
        try{
            String openid=userInfoUtil.getUserInfo(ge_session, UserInfoUtil.INFO.OPENID);
            userInfoUtil.updateCustomizedUserInfo(openid,newInfo);
        }catch (UserInfoUtil.UserInfoException e){
            return JSON.parseObject(e.getMessage());
        }
        return JSONObject.parseObject("{\"msg\":\"success\"}");

    }

    @GetMapping("/hicon/{openid}")
    public void getHicon(@PathVariable("openid") String openid, ServletOutputStream servletOutputStream,HttpServletResponse httpServletResponse) throws IOException {

        File icon=new File(userHiconPath,openid+".png");
        if(!icon.exists()){
            httpServletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        httpServletResponse.setHeader("Content-Type","image/png");
        httpServletResponse.setHeader("Content-Length",Long.toString(icon.length()));



        //一定要放后面
        servletOutputStream.write(new FileInputStream(icon).readAllBytes());
        //httpServletResponse.setHeader();
        //httpServletResponse.setHeader();


    }
    @GetMapping("/getUserInfo")
    public JSONObject getUserCustomizedInfo(@RequestHeader("ge_session")String ge_session){
        String openid=userInfoUtil.getUserInfo(ge_session, UserInfoUtil.INFO.OPENID);
        UserCustomizedInfo userCustomizedInfo=userInfoUtil.getUserCustomizedInfo(openid);
        JSONObject res=new JSONObject();
        for(Field f: userCustomizedInfo.getClass().getDeclaredFields()){
            if(!f.trySetAccessible()){
                f.setAccessible(true);
            }
            String key=f.getName();
            String value=null;
            try{
               value= (String)f.get(userCustomizedInfo);
            }
            catch (IllegalAccessException e){
                res.put("errMsg","告诉管理员出问题啦");
                return res;
            }
            res.put(key,value);
        }
        return res;
    }
    @PostMapping("/uploadHicon")
    public void uploadHicon(@RequestHeader String ge_session, @RequestParam("file") MultipartFile multipartFile,HttpServletResponse resp){
        String openid=userInfoUtil.getUserInfo(ge_session, UserInfoUtil.INFO.OPENID);
        String fileName=multipartFile.getOriginalFilename();
        String type=fileName.indexOf('.')==-1?"jpg":fileName.substring(fileName.lastIndexOf('.')+1);
        fileName=openid+"."+type;
        File Hicon=null;
        try{
            Hicon=new File(userHiconPath,fileName);
            FileOutputStream fos=new FileOutputStream(Hicon);
            fos.write(multipartFile.getBytes());
            fos.close();
        }
        catch (IOException e){
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        try{
            adjustIMGtoPNG(Hicon);
        }catch (IOException e){
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    public void adjustIMGtoPNG(File img) throws IOException {
        if(!img.exists()||img.isDirectory()){
            throw new IOException();
        }
        if(img.getName().endsWith(".png")) return;
        BufferedImage bfimg= ImageIO.read(img);
        String subfix=".png",path=img.getParent(),filename=img.getName();
        if(filename.indexOf('.')==-1) filename+=subfix;
        else{
            filename=filename.substring(0,filename.indexOf("."))+subfix;
        }
        ImageIO.write(bfimg,"png",new File(path,filename));
        bfimg.flush();
        img.delete();


    }

    @GetMapping("finish")
    public String finishOrder(@RequestHeader("ge_session") String ge_session,@Param("oid") @NotNull String oid){
        String openid=userInfoUtil.getUserInfo(ge_session, UserInfoUtil.INFO.OPENID);
        Order o=daoService.checkSubmitAdmin(openid,oid);
        if(o==null) return new MsgUtil("errMsg","不是你的订单啊憨批").toString();
        int status=o.getstatus();
        if(status!=2) return new MsgUtil("errMsg","invalid finish").toString();
        else{
            daoService.setOrderStatusFinished(oid);
            String f_time=simpleDateFormat.format(new Date());
            daoService.setOrderFinishTime(oid,f_time);
            return new MsgUtil("msg","finish successfully").toString();
        }


    }

    @GetMapping("whetherComplete")
    public boolean whetherComplete(@RequestHeader("ge_session") String ge_session){
        String openid=userInfoUtil.getUserInfo(ge_session, UserInfoUtil.INFO.OPENID);
        return userInfoUtil.hasSignUpReality(openid);

    }

    @Autowired
    DelayJobService delayJobService;
    @Value("${spring.quartz.autoFinishScale}")
    long expireScale;
    @GetMapping("RFF")
    public JSONObject requestForFinish(@NotNull @Param("oid")String oid,@RequestHeader("ge_session")String ge_session,HttpServletResponse httpServletResponse){
        JSONObject resp=new JSONObject();
        String openid=userInfoUtil.getUserInfo(ge_session, UserInfoUtil.INFO.OPENID);
        try{
            String taker=daoService.checkTaker(oid);
            if(taker==null){
                resp.put("errMsg","wrong oid");
            }
            else if(!taker.equals(openid)){
                resp.put("errMsg","not yours bitch");
            }
            else  if(daoService.getOrderStatus(oid)!=2){
                resp.put("errMsg","invalid option for current order status");
            }
            else{
                delayJobService.add(new DelayJobService.AutoFinishOrderDelayed(expireScale, TimeUnit.SECONDS,oid));
                resp.put("msg","success");
            }
            return resp;
        }
        catch (Exception e){
            e.printStackTrace();
            resp.put("errMsg","unknown error");
            httpServletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return resp;
        }
    }

    @PostMapping("edit")
    public String edit(@Param("oid") String oid,@RequestHeader("ge_session") String ge_session,@NotNull @RequestBody JSONObject newDetail){
        String openid=userInfoUtil.getUserInfo(ge_session, UserInfoUtil.INFO.OPENID);
        if(daoService.ensureAdmin(openid,oid)==0){
            return new MsgUtil("errMsg","admin denied").toString();
        }
        int status=daoService.getOrderStatus(oid);
        if(status!=Order.ORDER_STATUS_AVALIABLE){
            return new MsgUtil("errMsg","order not editable").toString();
        }
        LinkedHashMap _content= (LinkedHashMap) newDetail.get("content");
        JSONObject content=new JSONObject();
        content.put("des",_content.get("des"));
        content.put("org",_content.get("org"));
        content.put("detail",_content.get("detail"));
        String title;
        double price;
        try{
            //System.out.println(newDetail.toJSONString());
            title=newDetail.getString("title");
            price=newDetail.getDouble("price");
            if(title==null||content.get("des")==null||content.get("org")==null||content.get("detail")==null||newDetail.get("price")==null){
                throw new NullPointerException();
            }

        }
        catch (Exception e){
            return new MsgUtil("errMsg","detail wrong format").toString();
        }
        daoService.updateOrder(oid,title,price,content.toJSONString());
        if(status==1){
            jmsTemplate.convertAndSend("flushOrders", ' ');
        }
        return new MsgUtil("msg","success").toString();


    }

}
