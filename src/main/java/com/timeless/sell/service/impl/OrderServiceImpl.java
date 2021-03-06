package com.timeless.sell.service.impl;

import com.timeless.sell.converter.OrderMaster2OrderDtoConverter;
import com.timeless.sell.dto.CartDTO;
import com.timeless.sell.dto.OrderDTO;
import com.timeless.sell.entity.OrderDetail;
import com.timeless.sell.entity.OrderMaster;
import com.timeless.sell.entity.ProductInfo;
import com.timeless.sell.enums.OrderStatusEnum;
import com.timeless.sell.enums.PayStatusEnum;
import com.timeless.sell.enums.ResultEnum;
import com.timeless.sell.exception.SellException;
import com.timeless.sell.repository.OrderDetailRepository;
import com.timeless.sell.repository.OrderMasterRepository;
import com.timeless.sell.service.*;
import com.timeless.sell.utils.KeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lijiayin
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private PayService payService;
    
    @Autowired
    private PushMessageService pushMessageService;
    
    @Autowired
    private OrderDetailRepository orderDetailRepository;
    
    @Autowired
    private OrderMasterRepository orderMasterRepository;
    
    @Autowired
    private WebSocket webSocket;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDTO create(OrderDTO orderDTO) {

        String orderId = KeyUtil.genUniqueKey();
        BigDecimal orderAmount = new BigDecimal(BigInteger.ZERO);
        
        //1.查询商品（数量，价格）
        for(OrderDetail orderDetail : orderDTO.getOrderDetailList()){
            ProductInfo productInfo = productService.findOne(orderDetail.getProductId());
            if(productInfo == null){
                throw new SellException(ResultEnum.PRODUCT_NOT_EXIST);
            }
            //2.计算订单总价
            orderAmount = productInfo.getProductPrice()
                    .multiply(new BigDecimal(orderDetail.getProductQuantity()))
                    .add(orderAmount);
            //订单详情入库
            orderDetail.setDetailId(KeyUtil.genUniqueKey());
            orderDetail.setOrderId(orderId);
            BeanUtils.copyProperties(productInfo, orderDetail);
            orderDetail.setCreateTime(new Date());
            orderDetail.setUpdateTime(new Date());
            orderDetailRepository.save(orderDetail);
            
        }
        
        //3.写入订单数据库
        OrderMaster orderMaster = OrderMaster.builder().build();
        orderDTO.setOrderId(orderId);
        BeanUtils.copyProperties(orderDTO, orderMaster);
        orderMaster.setOrderAmount(orderAmount);
        orderMaster.setOrderStatus(OrderStatusEnum.NEW.getCode());
        orderMaster.setPayStatus(PayStatusEnum.WAIT.getCode());
        orderMasterRepository.save(orderMaster);
        
        //4.扣库存
        List<CartDTO> cartDTOList = orderDTO.getOrderDetailList().stream().map(e -> 
            CartDTO.builder().productId(e.getProductId())
                    .productQuantity(e.getProductQuantity())
                    .build()
        ).collect(Collectors.toList());
        productService.decreaseStock(cartDTOList);

        webSocket.sendMessage(orderId);
        
        return orderDTO;
    }

    @Override
    public OrderDTO findOne(String orderId) {
        OrderMaster orderMaster = orderMasterRepository.findById(orderId).orElse(null);
        if(orderMaster == null){
            throw new SellException(ResultEnum.ORDER_NOT_EXIST);
        }
        List<OrderDetail> orderDetailList = orderDetailRepository.findByOrderId(orderId);
        if(CollectionUtils.isEmpty(orderDetailList)){
            throw new SellException(ResultEnum.ORDER_DETAIL_NOT_EXIST);
        }
        OrderDTO orderDTO = new OrderDTO();
        BeanUtils.copyProperties(orderMaster, orderDTO);
        orderDTO.setOrderDetailList(orderDetailList);
        
        return orderDTO;
    }

    @Override
    public Page<OrderDTO> findList(Pageable pageable) {
        Page<OrderMaster> orderMasterPage = orderMasterRepository.findAll(pageable);
        List<OrderDTO> orderDTOList = OrderMaster2OrderDtoConverter.convert(orderMasterPage.getContent());
        return new PageImpl<>(orderDTOList, pageable, orderMasterPage.getTotalElements());
    }

    @Override
    public Page<OrderDTO> findList(String buyerOpenid, Pageable pageable) {
        Page<OrderMaster> orderMasterPage = orderMasterRepository.findByBuyerOpenid(buyerOpenid, pageable);
        List<OrderDTO> orderDTOList = OrderMaster2OrderDtoConverter.convert(orderMasterPage.getContent());
        return new PageImpl<>(orderDTOList, pageable, orderMasterPage.getTotalElements());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDTO cancel(OrderDTO orderDTO) {
        
        OrderMaster orderMaster = new OrderMaster();
        
        //判断订单状态
        if(!orderDTO.getOrderStatus().equals(OrderStatusEnum.NEW.getCode())){
            log.error("【取消订单】订单状态不正确，orderId={}, orderStatus={}", 
                    orderDTO.getOrderId(), orderDTO.getOrderStatus());
            throw new SellException(ResultEnum.ORDER_STATUS_ERROR);
        }
        
        //修改订单状态
        orderDTO.setOrderStatus(OrderStatusEnum.CANCEL.getCode());
        BeanUtils.copyProperties(orderDTO, orderMaster);
        OrderMaster updateResult = orderMasterRepository.save(orderMaster);
        if(updateResult == null){
            log.error("【取消订单】更新失败， orderMaster={}", orderMaster);
            throw new SellException(ResultEnum.ORDER_UPDATE_FAIL);
        }
        
        //返还库存
        if(CollectionUtils.isEmpty(orderDTO.getOrderDetailList())){
            log.error("【取消订单】订单中无商品详情，orderDTO={}", orderDTO);
            throw new SellException(ResultEnum.ORDER_DETAIL_EMPTY);
        }
        List<CartDTO> cartDTOList = orderDTO.getOrderDetailList().stream()
                .map(e -> CartDTO.builder()
                        .productId(e.getProductId())
                        .productQuantity(e.getProductQuantity())
                        .build())
                .collect(Collectors.toList());
        productService.increaseStock(cartDTOList);
        
        //如果已支付退款
        if(orderDTO.getPayStatus().equals(PayStatusEnum.SUCCESS.getCode())){
            payService.refund(orderDTO);
        }
        return orderDTO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDTO finish(OrderDTO orderDTO) {
        
        
        //判断订单状态
        if(!orderDTO.getOrderStatus().equals(OrderStatusEnum.NEW.getCode())){
            log.error("【完结订单】订单状态不正确， orderId={}, orderStatus={}",
                    orderDTO.getOrderId(), orderDTO.getOrderStatus());
            throw new SellException(ResultEnum.ORDER_STATUS_ERROR);
        }
        
        //修改订单状态
        OrderMaster orderMaster = new OrderMaster();
        orderDTO.setOrderStatus(OrderStatusEnum.FINISH.getCode());
        BeanUtils.copyProperties(orderDTO, orderMaster);
        OrderMaster updateResult = orderMasterRepository.save(orderMaster);
        if(updateResult == null){
            log.error("【完结订单】更新失败， orderMaster={}", orderMaster);
            throw new SellException(ResultEnum.ORDER_UPDATE_FAIL);
        }
        
        //推送微信模板消息
        pushMessageService.orderStatus(orderDTO);
        
        return orderDTO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDTO paid(OrderDTO orderDTO) {
        
        //判断订单状态
        if(!orderDTO.getOrderStatus().equals(OrderStatusEnum.NEW.getCode())){
            log.error("【订单支付成功】订单状态不正确， orderId={}, orderStatus={}",
                    orderDTO.getOrderId(), orderDTO.getOrderStatus());
            throw new SellException(ResultEnum.ORDER_STATUS_ERROR);
        }
        
        //判断支付状态
        if(!orderDTO.getPayStatus().equals(PayStatusEnum.WAIT.getCode())){
            log.error("【订单支付成功】订单支付状态不正确，orderDTO={}", orderDTO);
            throw new SellException(ResultEnum.ORDER_PAY_STATUS_ERROR);
        }
        
        //修改支付状态
        OrderMaster orderMaster = new OrderMaster();
        orderDTO.setPayStatus(PayStatusEnum.SUCCESS.getCode());
        BeanUtils.copyProperties(orderDTO, orderMaster);
        OrderMaster updateResult = orderMasterRepository.save(orderMaster);
        if(updateResult == null){
            log.error("【订单支付成功】更新失败， orderMaster={}", orderMaster);
            throw new SellException(ResultEnum.ORDER_UPDATE_FAIL);
        }

        return orderDTO;
    }
}
