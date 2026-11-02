package com.sky.service.impl;

import com.sky.constant.StatusConstant;
import com.sky.entity.Orders;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 根据时间段统计营业数据
     * @param begin
     * @param end
     * @return
     */
    public BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end) {
        /**
         * 营业额：当日已完成订单的总金额
         * 有效订单：当日已完成订单的数量
         * 订单完成率：有效订单数 / 总订单数
         * 平均客单价：营业额 / 有效订单数
         * 新增用户：当日新增用户的数量
         */
        log.info("查询工作台数据，开始时间：{}，结束时间：{}", begin, end);

        Map map = new HashMap();
        map.put("begin",begin);
        map.put("end",end);

        //查询总订单数（所有状态的订单）
        Integer totalOrderCount = orderMapper.countByMap(map);

        map.put("status", Orders.COMPLETED);
        //营业额
        Double turnover = orderMapper.sumByMap(map);
        turnover = turnover == null? 0.0 : turnover;

        //有效订单数（已完成的订单）
        Integer validOrderCount = orderMapper.countByMap(map);

        Double unitPrice = 0.0;

        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0){
            //订单完成率
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }
        
        if(validOrderCount != 0){
            //平均客单价
            unitPrice = turnover / validOrderCount;
        }

        //新增用户数
        Integer newUsers = userMapper.countByMap(map);

        log.info("工作台数据统计结果 - 营业额：{}，有效订单数：{}，总订单数：{}，订单完成率：{}，平均客单价：{}，新增用户：{}", 
                turnover, validOrderCount, totalOrderCount, orderCompletionRate, unitPrice, newUsers);

        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }


    /**
     * 查询订单管理数据
     *
     * @return
     */
    public OrderOverViewVO getOrderOverView() {
        Map map = new HashMap();
        map.put("begin", LocalDateTime.now().with(LocalTime.MIN));
        map.put("status", Orders.TO_BE_CONFIRMED);

        //待接单
        Map<String, Object> waitingStats = orderMapper.getOrdersStatistics(map);
        Integer waitingOrders = waitingStats.get("totalOrders") == null ? 0 : ((Number) waitingStats.get("totalOrders")).intValue();

        //待派送
        map.put("status", Orders.CONFIRMED);
        Map<String, Object> deliveredStats = orderMapper.getOrdersStatistics(map);
        Integer deliveredOrders = deliveredStats.get("totalOrders") == null ? 0 : ((Number) deliveredStats.get("totalOrders")).intValue();

        //已完成
        map.put("status", Orders.COMPLETED);
        Map<String, Object> completedStats = orderMapper.getOrdersStatistics(map);
        Integer completedOrders = completedStats.get("totalOrders") == null ? 0 : ((Number) completedStats.get("totalOrders")).intValue();

        //已取消
        map.put("status", Orders.CANCELLED);
        Map<String, Object> cancelledStats = orderMapper.getOrdersStatistics(map);
        Integer cancelledOrders = cancelledStats.get("totalOrders") == null ? 0 : ((Number) cancelledStats.get("totalOrders")).intValue();

        //全部订单
        map.put("status", null);
        Map<String, Object> allStats = orderMapper.getOrdersStatistics(map);
        Integer allOrders = allStats.get("totalOrders") == null ? 0 : ((Number) allStats.get("totalOrders")).intValue();

        return OrderOverViewVO.builder()
                .waitingOrders(waitingOrders)
                .deliveredOrders(deliveredOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .allOrders(allOrders)
                .build();
    }

    /**
     * 查询菜品总览
     *
     * @return
     */
    public DishOverViewVO getDishOverView() {
        Map map = new HashMap();
        map.put("status", StatusConstant.ENABLE);
        Integer sold = dishMapper.countByMap(map);

        map.put("status", StatusConstant.DISABLE);
        Integer discontinued = dishMapper.countByMap(map);

        return DishOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    /**
     * 查询套餐总览
     *
     * @return
     */
    public SetmealOverViewVO getSetmealOverView() {
        Map map = new HashMap();
        map.put("status", StatusConstant.ENABLE);
        Integer sold = setmealMapper.countByMap(map);

        map.put("status", StatusConstant.DISABLE);
        Integer discontinued = setmealMapper.countByMap(map);

        return SetmealOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }
}
