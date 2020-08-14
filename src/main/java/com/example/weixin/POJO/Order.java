package com.example.weixin.POJO;

import com.alibaba.fastjson.JSONObject;

import java.util.Date;

public class Order {
    public static int ORDER_STATUS_FINISHED =0;
    public static int ORDER_STATUS_AVALIABLE =1;
    public static int ORDER_STATUS_TOOK =2;
    public static int ORDER_STATUS_CANCLED =3;
    public static int ORDER_STATUS_TOFINISH =4;




    private String oId;
    private String oContent;
    private String title;
    private String taker;//接单用户的openid
    private Date tDate;//接单时间
    private String takerNick;
    private String sOpenid;//下单用户的openid
    private String submitterNick;
    private int status;
    private Date date;//下单时间
    private double price;


    public String getSubmitterNick() {
        return submitterNick;
    }

    public void setSubmitterNick(String submitterNick) {
        this.submitterNick = submitterNick;
    }


    public void setStatus(int status) {
        this.status = status;
    }

    public String getsOpenid() {
        return sOpenid;
    }

    public void setsOpenid(String sOpenid) {
        this.sOpenid = sOpenid;
    }

    public String getTakerNick() {
        return takerNick;
    }

    public void setTakerNick(String takerNick) {
        this.takerNick = takerNick;
    }

    public Date gettDate() {
        return tDate;
    }

    public void settDate(Date tDate) {
        this.tDate = tDate;
    }




    public String getTaker() {
        return taker;
    }

    public void setTaker(String taker) {
        this.taker = taker;
    }



    public int getstatus() {
        return status;
    }

    public void setoStatus(int status) {
        this.status = status;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getoId() {
        return oId;
    }

    public void setoId(String oId) {
        this.oId = oId;
    }

    public String getoContent() {
        return oContent;
    }

    public void setoContent(String oContent) {
        this.oContent = oContent;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public String toString() {
        JSONObject jsonObject=new JSONObject();
        jsonObject.put("oId",oId);
        jsonObject.put("title",title);
        jsonObject.put("oContent",oContent);
        jsonObject.put("date",date);
        jsonObject.put("price",price);
        jsonObject.put("status",status);
        return jsonObject.toJSONString();
    }
}
