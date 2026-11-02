package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private org.redisson.api.RedissonClient redissonClient;

    /**
     * 添加购物车
     * 
     * @param shoppingCartDTO
     */
    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);

        // 【学习笔记】这里是防超卖、防并发刷单的关键：Redisson 分布式锁
        // 拼装一把“针对到具体商品”的锁：如果用户一直点同一个菜品，才会被锁住。
        // 加这把锁的意思是：哪怕开 100 个线程同时给这个用户加这道菜，也只能一个一个排队进。
        String lockKey = "cart_lock:" + userId + ":" + (shoppingCartDTO.getDishId() != null ? shoppingCartDTO.getDishId() : shoppingCartDTO.getSetmealId());
        
        // 找 Redisson 客户端要这把名字叫 lockKey 的锁
        org.redisson.api.RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // tryLock (最多等待拿锁多少时间, 如果拿到了锁可以占有锁多少秒, 时间单位)
            // 这里：尝试等 1 秒（等不到就算抢锁失败），拿到锁之后这把锁自爆时间为 10 秒（防止服务器宕机死锁）
            boolean isLocked = lock.tryLock(1, 10, java.util.concurrent.TimeUnit.SECONDS);
            if (!isLocked) {
                // 如果抢不到锁，说明有别的人正在操作这个购物车这道菜，直接拒绝！
                throw new com.sky.exception.OrderBusinessException("操作过于频繁，请稍后再试");
            }

            // ---------- 只有拿到锁的人，才会执行下面核心的加购物车逻辑 ----------
            List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

            if (list != null && list.size() > 0) {
                ShoppingCart cart = list.get(0);
                cart.setNumber(cart.getNumber() + 1);
                shoppingCartMapper.updateNumber(cart);
            } else {
                Long dishId = shoppingCartDTO.getDishId();
                if (dishId != null) {
                    // 本次添加的是菜品
                    Dish dish = dishMapper.getById(dishId);
                    shoppingCart.setName(dish.getName());
                    shoppingCart.setImage(dish.getImage());
                    shoppingCart.setAmount(dish.getPrice());
                } else {
                    // 本次添加的是套餐
                    Long setmealId = shoppingCartDTO.getSetmealId();
                    Setmeal setmeal = setmealMapper.getById(setmealId);
                    shoppingCart.setName(setmeal.getName());
                    shoppingCart.setImage(setmeal.getImage());
                    shoppingCart.setAmount(setmeal.getPrice());
                }
                shoppingCart.setNumber(1);
                shoppingCart.setCreateTime(LocalDateTime.now());
                shoppingCartMapper.insert(shoppingCart);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new com.sky.exception.OrderBusinessException("系统繁忙");
        } finally {
            // 【学习笔记】最终必须要释放锁！
            // isHeldByCurrentThread()：只有这把锁真正属于当前线程，才允许去解开这把锁。
            // 不然 A 线程不小心解开了 B 线程加的锁，就乱套了！这就是分布式锁的安全机制。
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 获取当前用户的购物车数据
     * 
     * @return
     */
    @Override
    public List<ShoppingCart> showShoppingCart() {
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        return list;
    }

    @Override
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);

        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        if (list != null && list.size() > 0) {
            ShoppingCart cart = list.get(0);

            if (cart.getNumber() > 1) {
                // 商品数量大于1，减少数量
                cart.setNumber(cart.getNumber() - 1);
                shoppingCartMapper.updateNumber(cart);
            } else {
                // 商品数量等于1，直接删除该记录
                shoppingCartMapper.deleteById(cart.getId());
            }
        }
    }

    public void cleanShoppingCart() {
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.deleteByUserId(userId);
    }
}
