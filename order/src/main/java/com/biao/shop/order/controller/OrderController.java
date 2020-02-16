package com.biao.shop.order.controller;

import com.biao.shop.common.entity.ItemListEntity;
import com.biao.shop.common.entity.ShopClientEntity;
import com.biao.shop.common.entity.ShopOrderEntity;
import com.biao.shop.order.service.ItemListService;
import com.biao.shop.order.service.OrderService;
import com.github.pagehelper.PageInfo;
import org.dromara.soul.client.common.annotation.SoulClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @ClassName OrderController
 * @Description: TODO
 * @Author Biao
 * @Date 2020/2/15
 * @Version V1.0
 **/
@RestController
@RequestMapping("/order")
public class OrderController {
    private Logger logger = LoggerFactory.getLogger(OrderController.class);

    private OrderService orderService;
    private ItemListService itemListService;

    @Autowired
    public OrderController(OrderService orderService,ItemListService itemListService){
        this.orderService = orderService;
        this.itemListService =  itemListService;
    }

    @SoulClient(path = "/vehicle/order/list", desc = "获取订单列表")
    @GetMapping("/list")
    public PageInfo<ShopOrderEntity> listOrder(@RequestParam("pageNum")Integer current, @RequestParam("pageSize")Integer size,
                                                @RequestParam(value = "orderUuid",required = false)String clientUuid,
                                                @RequestParam(value = "clientName",required = false)String name,
                                                @RequestParam(value = "vehiclePlate",required = false)String startDate,
                                                @RequestParam(value = "vehiclePlate",required = false)String endDate,
                                                @RequestParam(value = "status",required = false)String phone){
        return orderService.listOrder(current,size);
    }

    @SoulClient(path = "/vehicle/order/itemList", desc = "获取订单商品列表")
    @GetMapping("/itemList")
    public List<ItemListEntity> itemList(@RequestParam("orderUuid")String orderUuid){
        return itemListService.listDetail(orderUuid);
    }

    @SoulClient(path = "/vehicle/order/**", desc = "获取订单商品列表")
    @GetMapping("/{id}")
    public ShopOrderEntity getOrder(@PathVariable("id") int id){
        return orderService.queryOrder(id);
    }
}