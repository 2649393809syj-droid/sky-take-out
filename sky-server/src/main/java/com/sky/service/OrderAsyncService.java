package com.sky.service;

public interface OrderAsyncService {
    
    /**
     * 支付成功后异步处理接单提醒、清空购物车等非核心逻辑
     * @param orderId 订单ID
     * @param outTradeNo 订单号
     */
    void handlePaySuccessAsync(Long orderId, String outTradeNo);
}
