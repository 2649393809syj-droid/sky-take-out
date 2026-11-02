package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

//自定义切面
@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    /**
     * 逻辑：
     * 1. 获取方法参数：即实体对象
     * 2. 获取当前时间
     * 3. 将当前时间设置到实体对象中的更新时间和创建时间字段中
     * 4. 将当前登录用户设置到实体对象中的更新人和创建人字段中
     *
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut() {
    }

    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        log.info("开始进行数据填充");

        // 获取当前被拦截的方法参数，即实体对象
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 修正：应该从方法上获取注解，而不是从signature对象上获取
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
        
        // 添加空值检查
        if (autoFill == null) {
            return;
        }
        
        OperationType operationType = autoFill.value();

        //获取到当前被拦截的方法的参数--实体对象
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return;
        }
        Object arg = args[0];
        //准备赋值的数据
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        //根据当前不同的操作类型，为对应的属性通过反射来赋值
        if(operationType == OperationType.INSERT){
            // INSERT操作：设置所有四个字段
            try {
                Method setCreateTime = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setCreateUser = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateTime = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                setCreateTime.invoke(arg, now);
                setUpdateTime.invoke(arg, now);
                setCreateUser.invoke(arg, currentId);
                setUpdateUser.invoke(arg, currentId);
            } catch (NoSuchMethodException e) {
                log.warn("实体类 {} 缺少必要的setter方法: {}", arg.getClass().getSimpleName(), e.getMessage());
            }
        } else if (operationType == OperationType.UPDATE) {
            // UPDATE操作：只设置更新时间和更新人
            try {
                Method setUpdateTime = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                setUpdateTime.invoke(arg, now);
                setUpdateUser.invoke(arg, currentId);
            } catch (NoSuchMethodException e) {
                log.warn("实体类 {} 缺少更新字段的setter方法: {}", arg.getClass().getSimpleName(), e.getMessage());
            }
        }
    }
}
