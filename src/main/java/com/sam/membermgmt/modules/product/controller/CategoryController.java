package com.sam.membermgmt.modules.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sam.membermgmt.common.PageResult;
import com.sam.membermgmt.common.Result;
import com.sam.membermgmt.common.annotation.OperationLog;
import com.sam.membermgmt.common.annotation.RequireRole;
import com.sam.membermgmt.common.exception.BusinessException;
import com.sam.membermgmt.modules.product.entity.Product;
import com.sam.membermgmt.modules.product.entity.ProductCategory;
import com.sam.membermgmt.modules.product.mapper.ProductCategoryMapper;
import com.sam.membermgmt.modules.product.mapper.ProductMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "商品分类管理")
@RestController
@RequestMapping("/api/categories")
@RequireRole({"ROLE_ADMIN", "ROLE_MANAGER"})
public class CategoryController {

    @Autowired
    private ProductCategoryMapper categoryMapper;

    @Autowired
    private ProductMapper productMapper;

    @Operation(summary = "分类树")
    @GetMapping("/tree")
    public Result<List<ProductCategory>> tree() {
        List<ProductCategory> all = categoryMapper.selectList(
                new LambdaQueryWrapper<ProductCategory>().orderByAsc(ProductCategory::getSortOrder));
        List<ProductCategory> tree = all.stream()
                .filter(c -> c.getParentId() == 0)
                .peek(c -> c.setChildren(all.stream()
                        .filter(child -> child.getParentId().equals(c.getId()))
                        .collect(Collectors.toList())))
                .collect(Collectors.toList());
        return Result.success(tree);
    }

    @OperationLog("新增分类")
    @Operation(summary = "新增分类")
    @PostMapping
    public Result<Void> save(@RequestBody ProductCategory category) {
        categoryMapper.insert(category);
        return Result.success();
    }

    @OperationLog("编辑分类")
    @Operation(summary = "编辑分类")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ProductCategory category) {
        category.setId(id);
        categoryMapper.updateById(category);
        return Result.success();
    }

    @OperationLog("删除分类")
    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean hasChildren = categoryMapper.exists(
                new LambdaQueryWrapper<ProductCategory>().eq(ProductCategory::getParentId, id));
        if (hasChildren) {
            throw new BusinessException("该分类下有子分类，无法删除");
        }
        boolean hasProducts = productMapper.exists(
                new LambdaQueryWrapper<Product>().eq(Product::getCategoryId, id));
        if (hasProducts) {
            throw new BusinessException("该分类下有商品，无法删除");
        }
        categoryMapper.deleteById(id);
        return Result.success();
    }
}
