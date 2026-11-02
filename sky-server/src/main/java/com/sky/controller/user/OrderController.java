package com.sky.controller.user;

import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.sky.annotation.Idempotent;
import org.springframework.data.redis.core.StringRedisTemplate;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Api(tags = "用户订单接口")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    @GetMapping("/token")
    @ApiOperation("获取幂等性token")
    public Result<String> getToken() {
        // 【学习笔记】为什么前端下单前要先调用这个接口？
        // 相当于去后台排队拿一个“爱的号码牌”。拿到了号码牌，才能去下单。
        // UUID 是唯一的。存进 Redis 里，并设置有效时间 10 分钟（防止烂在内存里不管了）。
        // 下单的时候，前端把这个 token 放在 Header 里带过来校验。
        String token = java.util.UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set("idempotent_token:" + token, token, 10, java.util.concurrent.TimeUnit.MINUTES);
        return Result.success(token);
    }

    /**
     * 用户提交订单
     * 【学习笔记】
     * 只要加了 @Idempotent 注解，我们的 IdempotentInterceptor 就会拦截它。
     * 如果验证不过，直接抛出异常，连 Controller 的这个大门都进不来！
     */
    @PostMapping("/submit")
    @ApiOperation("用户下单")
    @Idempotent
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO) {
        log.info("用户下单：{}", ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.submit(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    /**
     * 订单支付
     * 
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("模拟支付成功，已生成订单");
        return Result.success(orderPaymentVO);
    }

    @GetMapping("/orderDetail/{id}")
    @ApiOperation("查询订单详情")
    public Result<OrderVO> orderDetail(@PathVariable Long id) {
        log.info("查询订单详情，订单id为：{}", id);
        OrderVO orderVO = orderService.getOrderDetail(id);
        return Result.success(orderVO);
    }

    @GetMapping("/historyOrders")
    @ApiOperation("查询历史订单")
    public Result<PageResult> pageQuery(Integer page, Integer pageSize, Integer status) {
        log.info("查询历史订单");
        PageResult pageResult = orderService.pageQuery(page, pageSize, status);
        return Result.success(pageResult);
    }

    @PutMapping("/cancel/{id}")
    @ApiOperation("取消订单")
    public Result cancel(@PathVariable Long id) throws Exception {
        log.info("取消订单：{}", id);
        orderService.cancel(id);
        return Result.success();
    }

    @PostMapping("/repetition/{id}")
    @ApiOperation("再来一单")
    public Result repitationSubmit(@PathVariable Long id) {
        log.info("再来一单：{}", id);
        orderService.repitationSubmit(id);
        return Result.success();
    }

    @GetMapping("/reminder/{id}")
    @ApiOperation("订单催单")
    public Result reminder(@PathVariable("id") Long id){
        log.info("订单催单：{}", id);
        orderService.reminder(id);
        return Result.success();
    }
}
