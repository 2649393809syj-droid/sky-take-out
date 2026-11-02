package com.sky.interceptor;

import com.sky.annotation.Idempotent;
import com.sky.exception.OrderBusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

/**
 * 接口幂等性拦截器
 * 【学习笔记】
 * HandlerInterceptor 是 Spring MVC 提供的拦截器接口。
 * 它可以在请求到达 Controller 之前（preHandle）、之后（postHandle）进行拦截处理。
 * 这里我们用它来在订单提交前，检查 Redis 里有没有对应的 Token。
 */
@Component
public class IdempotentInterceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String IDEMPOTENT_TOKEN_PREFIX = "idempotent_token:";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 如果拦截到的不是 Controller 中的方法，直接放行 (比如拦截到了静态资源)
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        // 2. 反射获取当前调用的方法上，有没有打上我们自定义的 @Idempotent 注解
        Idempotent idempotent = handlerMethod.getMethodAnnotation(Idempotent.class);
        
        // 3. 如果有这个注解，说明这个接口需要进行幂等性校验（防重提交校验）
        if (idempotent != null) {
            // 从 HTTP 请求头 (Header) 中获取前端传过来的 token
            String token = request.getHeader("idempotent-token");
            if (token == null || token.isEmpty()) {
                throw new OrderBusinessException("幂等性Token为空");
            }

            String key = IDEMPOTENT_TOKEN_PREFIX + token;
            
            // 4. 【核心亮点】使用 Lua 脚本保证原子性操作
            // 面试常问：为什么要用 Lua 脚本？
            // 答：因为我们要先 GET 判断 token 存不存在，如果存在再 DEL 删除它。
            // 这是两步错开的操作。在极其极端的高并发下（比如前端连发两个一模一样的请求），
            // 两个线程可能同时 GET 到存在，然后都去执行下面的业务。
            // Redis 执行 Lua 脚本是单线程执行的，整个脚本天然具有原子性（类似包裹了一个事务），彻底杜绝并发问题。
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " + // 找到了就删除，并返回删除成功的标志(1)
                    "else " +
                    "return 0 " + // 没找到或者值不对，返回 0
                    "end";
            
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(script);
            redisScript.setResultType(Long.class);

            // 让 Redis 执行这个 Lua 脚本
            Long result = stringRedisTemplate.execute(redisScript, Collections.singletonList(key), token);
            
            // 5. 如果返回 0，说明 Redis 里已经没有这个 token 了
            // 要么是 token 压根没生成过（非法请求），要么是已经被第一个请求删掉了（重复提交）
            if (result == null || result == 0L) {
                // 抛出异常，前端会提示用户
                throw new OrderBusinessException("请勿重复提交请求");
            }
        }
        
        // 校验通过，放行，继续去执行 Controller 里的方法
        return true;
    }
}
