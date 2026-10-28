package com.sam.membermgmt.modules.order.service.impl;

import com.sam.membermgmt.modules.member.entity.Member;
import com.sam.membermgmt.modules.member.entity.MemberLevel;
import com.sam.membermgmt.modules.member.mapper.MemberLevelMapper;
import com.sam.membermgmt.modules.member.mapper.MemberMapper;
import com.sam.membermgmt.modules.member.service.IPointsService;
import com.sam.membermgmt.modules.order.dto.CartItemDTO;
import com.sam.membermgmt.modules.order.dto.CheckoutRequestDTO;
import com.sam.membermgmt.modules.order.dto.OrderVO;
import com.sam.membermgmt.modules.order.mapper.OrderItemMapper;
import com.sam.membermgmt.modules.order.mapper.OrderMapper;
import com.sam.membermgmt.modules.product.entity.Product;
import com.sam.membermgmt.modules.product.mapper.ProductMapper;
import com.sam.membermgmt.modules.product.mapper.StockRecordMapper;
import com.sam.membermgmt.utils.OrderNoGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceStockTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private MemberMapper memberMapper;
    @Mock
    private MemberLevelMapper levelMapper;
    @Mock
    private StockRecordMapper stockRecordMapper;
    @Mock
    private IPointsService pointsService;
    @Mock
    private OrderNoGenerator orderNoGenerator;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void checkout_same_product_multiple_lines_should_aggregate_and_deduct_once() {
        Member member = new Member();
        member.setId(1L);
        member.setStatus(1);
        member.setCurrentLevelId(2L);
        member.setName("测试会员");
        member.setTotalConsumption(BigDecimal.ZERO);

        MemberLevel level = new MemberLevel();
        level.setId(2L);
        level.setLevelName("白金会员");
        level.setDiscountRate(BigDecimal.ONE);
        level.setPointsRate(BigDecimal.ONE);

        Product product = new Product();
        product.setId(100L);
        product.setProductName("商品A");
        product.setProductCode("A001");
        product.setPrice(new BigDecimal("10"));
        product.setStockQuantity(10);
        product.setStatus(1);

        when(memberMapper.selectById(1L)).thenReturn(member);
        when(levelMapper.selectById(2L)).thenReturn(level);
        when(productMapper.selectById(100L)).thenReturn(product);
        when(productMapper.decrementStock(anyLong(), anyInt())).thenReturn(1);
        when(orderNoGenerator.generate()).thenReturn("SAM202609030001");

        CartItemDTO a = new CartItemDTO();
        a.setProductId(100L);
        a.setQuantity(2);
        CartItemDTO b = new CartItemDTO();
        b.setProductId(100L);
        b.setQuantity(1);
        CheckoutRequestDTO dto = new CheckoutRequestDTO();
        dto.setMemberId(1L);
        dto.setItems(Arrays.asList(a, b));
        dto.setPaymentMethod("到店支付");

        OrderVO vo = orderService.checkout(dto, 99L);

        assertNotNull(vo);
        assertEquals(0, vo.getTotalAmount().compareTo(new BigDecimal("30")));

        // 核心断言：同一商品两次出现被聚合为一次扣减，数量合计为 3，而非逐行扣减两次
        verify(productMapper, times(1)).decrementStock(100L, 3);
    }
}
