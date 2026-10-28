package com.sam.membermgmt.modules.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sam.membermgmt.modules.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 原子扣减库存：仅在当前库存充足且商品未被删除时扣减，避免并发或同商品重复下单导致超卖。
     * 返回受影响行数，0 表示库存不足或商品已删除。
     */
    @Update("UPDATE product SET stock_quantity = stock_quantity - #{qty} " +
            "WHERE id = #{id} AND stock_quantity >= #{qty} AND deleted = 0")
    int decrementStock(@Param("id") Long id, @Param("qty") int qty);
}
