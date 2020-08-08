package com.example.weixin.Services;

import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public interface ChatMsgRepository {
    void pushNewMsg(String from, String to, String date, String content,boolean accepted);
}
