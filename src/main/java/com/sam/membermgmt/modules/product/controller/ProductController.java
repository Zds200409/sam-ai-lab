package com.sam.membermgmt.modules.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sam.membermgmt.common.PageResult;
import com.sam.membermgmt.common.Result;
import com.sam.membermgmt.common.annotation.OperationLog;
import com.sam.membermgmt.common.annotation.RequireRole;
import com.sam.membermgmt.common.exception.BusinessException;
import com.sam.membermgmt.modules.product.entity.Product;
import com.sam.membermgmt.modules.product.entity.ProductCategory;
import com.sam.membermgmt.modules.product.entity.StockRecord;
import com.sam.membermgmt.modules.product.mapper.ProductCategoryMapper;
import com.sam.membermgmt.modules.product.mapper.ProductMapper;
import com.sam.membermgmt.modules.product.mapper.StockRecordMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "商品管理")
@RestController
@RequestMapping("/api/products")
@RequireRole({"ROLE_ADMIN", "ROLE_MANAGER"})
public class ProductController {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductCategoryMapper categoryMapper;

    @Autowired
    private StockRecordMapper stockRecordMapper;

    @Operation(summary = "分页查询商品")
    @GetMapping("/page")
    @RequireRole({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_CASHIER"})
    public Result<PageResult<Product>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .like(StringUtils.hasText(productName), Product::getProductName, productName)
                .eq(categoryId != null, Product::getCategoryId, categoryId)
                .eq(status != null, Product::getStatus, status)
                .orderByDesc(Product::getCreateTime);
        IPage<Product> page = productMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(PageResult.of(page));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{id}")
    @RequireRole({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_CASHIER"})
    public Result<Product> detail(@PathVariable Long id) {
        return Result.success(productMapper.selectById(id));
    }

    @OperationLog("新增商品")
    @Operation(summary = "新增商品")
    @PostMapping
    public Result<Void> save(@RequestBody Product product) {
        productMapper.insert(product);
        return Result.success();
    }

    @OperationLog("编辑商品")
    @Operation(summary = "编辑商品")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Product product) {
        product.setId(id);
        productMapper.updateById(product);
        return Result.success();
    }

    @OperationLog("商品上下架切换")
    @Operation(summary = "上下架切换")
    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        product.setStatus(product.getStatus() == 1 ? 0 : 1);
        productMapper.updateById(product);
        return Result.success();
    }

    @OperationLog("商品入库")
    @Operation(summary = "商品入库")
    @PostMapping("/{id}/stock/in")
    @Transactional
    public Result<Void> stockIn(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        int quantity = Integer.parseInt(params.get("quantity").toString());
        String remark = (String) params.getOrDefault("remark", "入库操作");
        int stockBefore = product.getStockQuantity();
        product.setStockQuantity(stockBefore + quantity);
        productMapper.updateById(product);

        StockRecord record = new StockRecord();
        record.setProductId(id);
        record.setQuantityChange(quantity);
        record.setType(1);
        record.setStockBefore(stockBefore);
        record.setStockAfter(product.getStockQuantity());
        record.setRemark(remark);
        stockRecordMapper.insert(record);
        return Result.success();
    }

    @OperationLog("删除商品")
    @Operation(summary = "删除商品（逻辑删除）")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在或已删除");
        }
        // @TableLogic 逻辑删除：仅置 deleted=1，历史订单明细中的商品快照不受影响
        productMapper.deleteById(id);
        return Result.success();
    }

    @Operation(summary = "库存流水")
    @GetMapping("/{id}/stock-records")
    public Result<List<StockRecord>> stockRecords(@PathVariable Long id) {
        return Result.success(stockRecordMapper.selectList(
                new LambdaQueryWrapper<StockRecord>()
                        .eq(StockRecord::getProductId, id)
                        .orderByDesc(StockRecord::getCreateTime)));
    }

    @Operation(summary = "POS商品搜索")
    @GetMapping("/search")
    @RequireRole({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_CASHIER"})
    public Result<List<Product>> search(@RequestParam String keyword) {
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .and(w -> w.like(Product::getProductName, keyword)
                                .or().like(Product::getProductCode, keyword)
                                .or().like(Product::getBarcode, keyword))
                        .eq(Product::getStatus, 1)
                        .last("LIMIT 20"));
        return Result.success(products);
    }
}
