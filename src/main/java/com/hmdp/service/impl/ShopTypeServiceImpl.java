package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_LIST;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ShopTypeMapper shopTypeMapper;
    /**
     * 查询所有商铺类型
     * @return
     */
    @Override
    public Result queryTypeList() {
        // 去redis里查询是否存在商户类型  (返回 list 类型）
        List<String> jsonList = stringRedisTemplate.opsForList().range(CACHE_SHOP_TYPE_LIST, 0, -1);
        //如果有就返回
        if(jsonList != null && !jsonList.isEmpty()) {
            // 转成 List<ShopType>
            List<ShopType> shopTypeList = jsonList.stream()
                    .map(json -> JSONUtil.toBean(json, ShopType.class))
                    .collect(Collectors.toList());
            return Result.ok(shopTypeList);
        }
        //没有则去数据库查询
        List<ShopType> shopTypeListFromDB = shopTypeMapper.selectList(null);
        //数据库没有就报错
        if (shopTypeListFromDB == null || shopTypeListFromDB.isEmpty()) {
            return Result.fail("商户类型不存在");
        }
        //有就写入redis 再返回
        List<String> redisValueList = shopTypeListFromDB.stream()
                .map(JSONUtil::toJsonStr)
                .collect(Collectors.toList());

        stringRedisTemplate.opsForList().rightPushAll(CACHE_SHOP_TYPE_LIST, redisValueList);
        return Result.ok(shopTypeListFromDB);
    }
}
