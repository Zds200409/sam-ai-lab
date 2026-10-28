package com.sam.membermgmt.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sam.membermgmt.common.PageResult;
import com.sam.membermgmt.common.Result;
import com.sam.membermgmt.common.annotation.OperationLog;
import com.sam.membermgmt.common.annotation.RequireRole;
import com.sam.membermgmt.common.exception.BusinessException;
import com.sam.membermgmt.modules.system.entity.Store;
import com.sam.membermgmt.modules.system.mapper.StoreMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stores")
@Tag(name = "门店管理")
@RequireRole({"ROLE_ADMIN", "ROLE_MANAGER"})
public class StoreController {

    @Autowired
    private StoreMapper storeMapper;

    @Operation(summary = "门店分页")
    @GetMapping("/page")
    public Result<PageResult<Store>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Store> wrapper = new LambdaQueryWrapper<Store>()
                .like(keyword != null, Store::getStoreName, keyword)
                .orderByDesc(Store::getCreateTime);
        IPage<Store> page = storeMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(PageResult.of(page));
    }

    @Operation(summary = "门店列表（营业中，用于下拉选择）")
    @GetMapping("/list")
    public Result<?> list() {
        return Result.success(storeMapper.selectList(
                new LambdaQueryWrapper<Store>().eq(Store::getStatus, 1).orderByAsc(Store::getStoreName)));
    }

    @OperationLog("新增门店")
    @Operation(summary = "新增门店")
    @PostMapping
    public Result<?> save(@RequestBody Store store) {
        storeMapper.insert(store);
        return Result.success("门店已创建");
    }

    @OperationLog("编辑门店")
    @Operation(summary = "编辑门店")
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Store store) {
        store.setId(id);
        storeMapper.updateById(store);
        return Result.success("门店已更新");
    }

    @OperationLog("删除门店")
    @Operation(summary = "删除门店")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        Store store = storeMapper.selectById(id);
        if (store == null) throw new BusinessException("门店不存在");
        storeMapper.deleteById(id);
        return Result.success("门店已删除");
    }
}
