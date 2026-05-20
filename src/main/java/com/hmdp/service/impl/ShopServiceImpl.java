package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 根据id查询商铺信息
     * @param id 商铺id
     * @return 商铺详情数据
     */
    @Override
    public Result queryById(Long id) {
        //缓存穿透
//        Shop shop = queryWithPassThrough(id);

        //利用互斥锁解决缓存击穿
        Shop shop = queryWithMutex(id);
        if(shop == null ) {
            return Result.fail("商铺不存在");
        }

        //返回
        return Result.ok(shop);
    }


    //互斥锁解决缓存击穿问题
    public Shop queryWithMutex(Long id) {
        //1 从redis查询商铺缓存
        String key = CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //2 判断缓存是否命中
        if (StrUtil.isNotBlank(shopJson)) {
            //3 存在 转成shop对象返回
            return JSONUtil.toBean(shopJson , Shop.class);
        }

        //判断命中的是否是空值
        if(shopJson != null) {
            return null;
        }

        //4 实现缓存重建
        //4.1 获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            //4.2判断是否获取成功
            if(isLock){
                //4.3 失败则休眠重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            //4.4 成功则根据id从数据库查询
            shop = getById(id);

            //模拟重建延时
            Thread.sleep(200);
            //5 商铺不存在则返回 404
            if (shop == null) {
                //将空值写入缓存  应对缓存穿透问题  2min删除
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL , TimeUnit.MINUTES);
                return null;
            }
            //6 存在则 写入redis
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop) , CACHE_SHOP_TTL , TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //7 释放互斥锁
            unlock(lockKey);
        }
        //8 返回商铺信息
        return shop;
    }



    //缓存空对象解决缓存穿透问题
    public Shop queryWithPassThrough(Long id) {
        //1 从redis查询商铺缓存
        String key = CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //2 判断缓存是否命中
        if (StrUtil.isNotBlank(shopJson)) {
            //3 存在 转成shop对象返回
            return JSONUtil.toBean(shopJson , Shop.class);
        }

        //判断命中的是否是空值
        if(shopJson != null) {
            return null;
        }

        //4 不存在则从数据库查询
        Shop shop = getById(id);
        //5 商铺不存在则返回 404
        if (shop == null) {
            //将空值写入缓存  应对缓存穿透问题  2min删除
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL , TimeUnit.MINUTES);
            return null;
        }
        //6 存在则 写入redis 返回商铺信息
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop) , CACHE_SHOP_TTL , TimeUnit.MINUTES);
        return shop;
    }


    //互斥锁
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }






    /**
     * 更新商铺信息
     * @param shop 商铺数据
     * @return 无
     */
    @Transactional
    @Override
    public Result update(Shop shop) {
        Long id  = shop.getId();
        if(id==null){
            return Result.fail("商铺id不能为空");
        }
        //更新数据库
        updateById(shop);
        //删除缓存
        stringRedisTemplate.delete((CACHE_SHOP_KEY + id));

        return null;
    }


}
