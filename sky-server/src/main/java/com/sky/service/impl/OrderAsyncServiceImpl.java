package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.sky.service.OrderAsyncService;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class OrderAsyncServiceImpl implements OrderAsyncService {

    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 【学习笔记】@Async 注解的使用
     * 1. 只要方法上加了 @Async，Spring 就会去拦截这个方法，把它放进单独的线程池去跑。
     * 2. 这里的 "customExecutor" 就是我们在 AsyncConfiguration 里配置好的自定义线程池名字。
     * 3. 结果验证：你可以观察这个 log 打印出来的当前线程名字，它绝对不是 http-nio-xxx，
     *    而是 sky-async-pool-xxx。证明主线程已经下班了，是新工人在干活。
     */
    @Override
    @Async("customExecutor")
    public void handlePaySuccessAsync(Long orderId, String outTradeNo) {
        log.info("异步执行支付成功后回调，当前线程：{}", Thread.currentThread().getName());
        
        // 推送给商家端
        Map map = new HashMap();
        map.put("type", 1); // 1表示来单提醒 2表示客户催单
        map.put("orderId", orderId);
        map.put("content", "订单号：" + outTradeNo);

        String json = JSON.toJSONString(map);
        log.info("通过WebSocket向商家端推送消息：{}", json);
        webSocketServer.sendToAllClient(json);
    }
}
