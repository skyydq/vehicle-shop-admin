package com.biao.shop.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biao.shop.common.dto.ItemListEntityDto;
import com.biao.shop.common.dto.OrderDto;
import com.biao.shop.common.dto.ShopOrderAppDTO;
import com.biao.shop.common.entity.ShopOrderEntity;
import com.biao.shop.common.enums.RespStatusEnum;
import com.biao.shop.common.response.ObjectResponse;
import com.biao.shop.order.manager.OrderManager;
import com.biao.shop.order.service.ItemListService;
import com.biao.shop.order.service.OrderService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.log.XxlJobLogger;
import org.dromara.soul.client.common.annotation.SoulClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
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
    private OrderManager orderManager;

    @Autowired
    public OrderController(OrderService orderService, ItemListService itemListService, OrderManager orderManager){
        this.orderService = orderService;
        this.itemListService =  itemListService;
        this.orderManager = orderManager;
    }

    @SoulClient(path = "/vehicle/order/list", desc = "获取订单列表")
    @GetMapping("/list")
    public Page<OrderDto> listOrder(@RequestParam("pageNum")Integer current, @RequestParam("pageSize")Integer size,
                                    @RequestParam(value = "orderUuid",required = false) String orderUuid,
                                    @RequestParam(value = "clientName",required = false) String clientName,
                                    @RequestParam(value = "phone",required = false) String phone,
                                    @RequestParam(value = "vehicleSeries",required = false)String vehicleSeries,
                                    @RequestParam(value = "vehiclePlate",required = false)String vehiclePlate,
                                    @RequestParam(value = "generateDateStart",required = false)String generateDateStart,
                                    @RequestParam(value = "generateDateEnd",required = false)String generateDateEnd,
                                    @RequestParam(value = "paidStatus",required = false)int paidStatus){
        // 这里的paidStatus最好设计为int，可以接收 0 1 2 ，boolean型，只能是0 1，前端传来都会自带默认0，导致无法查询无此条件限制的
        return orderService.listOrderDTO(current,size,orderUuid,clientName,phone,vehicleSeries,
                vehiclePlate,generateDateStart,generateDateEnd,paidStatus);
    }

    @SoulClient(path = "/vehicle/order/list/uniapp", desc = "移动端获取订单列表")
    @GetMapping("/list/uniapp")
    public ObjectResponse<Page<ShopOrderAppDTO>> listOrderApp(@RequestParam("pageNum")int current, @RequestParam("pageSize")int size,
                                                              @RequestParam(value = "clientName",required = false) String clientName,
                                                              @RequestParam(value = "vehiclePlate",required = false)String vehiclePlate,
                                                              @RequestParam(value = "generateDateStart",required = false)String generateDateStart,
                                                              @RequestParam(value = "generateDateEnd",required = false)String generateDateEnd,
                                                              @RequestParam(value = "paidStatus",required = false)int paidStatus){
        // 这里的paidStatus最好设计为int，可以接收 0 1 2 ，boolean型，只能是0 1，前端传来都会自带默认0，导致无法查询无此条件限制的
        Page<ShopOrderAppDTO> itemAppDTOPage = orderManager.listOrderAppDto(current, size, paidStatus,clientName,vehiclePlate);
        ObjectResponse<Page<ShopOrderAppDTO>> response = new ObjectResponse<>();
        response.setCode(RespStatusEnum.SUCCESS.getCode());
        response.setMessage(RespStatusEnum.SUCCESS.getMessage());
        response.setData(itemAppDTOPage);
        return response;
    }

    @SoulClient(path = "/vehicle/order/itemList", desc = "获取订单商品列表")
    @GetMapping("/itemList")
    public List<ItemListEntityDto> itemList(@RequestParam("orderUuid")String orderUuid){
        return itemListService.listDetailName(orderUuid);
    }

    @SoulClient(path = "/vehicle/order/**", desc = "获取一个订单商品")
    @GetMapping("/{id}")
    public ShopOrderEntity getOrder(@PathVariable("id") int id){
        return orderService.queryOrder(id);
    }

    @SoulClient(path = "/vehicle/order/del", desc = "删除订单")
    @GetMapping("/del")
    public int deleteOrder(@RequestParam("ids") String ids){
        if (ids.contains(",")){
            List<Integer> list = new ArrayList<>(8);
            String[] strings = ids.split(",");
            for (int i = 0; i < strings.length; i++) {
                list.add(Integer.valueOf(strings[i]));
            }
            return orderService.deleteBatchByIds(list);
        }
        return orderService.deleteById(Integer.parseInt(ids));
    }

    @SoulClient(path = "/vehicle/order/del/uid", desc = "删除订单")
    @GetMapping("/del/uid")
    public ObjectResponse<Integer> deleteOrderByUid(@RequestParam("uid") String uid){
        int result =  orderService.deleteOrderByUuid(uid);
        ObjectResponse<Integer> response = new ObjectResponse<>();
        response.setCode(RespStatusEnum.SUCCESS.getCode());
        response.setMessage(RespStatusEnum.SUCCESS.getMessage());
        response.setData(result);
        return response;
    }


    @SoulClient(path = "/vehicle/order/paid", desc = "支付一个订单")
    @GetMapping("/paid")
    public ObjectResponse<Integer> paidOrder(@RequestParam("Uid") String Uid,@RequestParam(value = "note",required = false) String note) throws Exception {
        int result =  orderService.paidOrder(Uid,note);
        ObjectResponse<Integer> response = new ObjectResponse<>();
        response.setCode(RespStatusEnum.SUCCESS.getCode());
        response.setMessage(RespStatusEnum.SUCCESS.getMessage());
        response.setData(result);
        return response;
    }

    @SoulClient(path = "/vehicle/order/cancel", desc = "取消一个未支付订单")
    @GetMapping("/cancel")
    public ObjectResponse<Integer> cancelOrder(@RequestParam("Uuid") String Uuid,@RequestParam(value = "note",required = false) String note) throws Exception {
        int result =  orderService.cancelOrder(Uuid,note);
        ObjectResponse<Integer> response = new ObjectResponse<>();
        response.setCode(RespStatusEnum.SUCCESS.getCode());
        response.setMessage(RespStatusEnum.SUCCESS.getMessage());
        response.setData(result);
        return response;
    }

    @SoulClient(path = "/vehicle/order/autoCancel", desc = "自动取消未支付订单")
    @GetMapping("/autoCancel")
    public ObjectResponse<Integer> autoCancelOrder(){
        return orderService.autoCancelOrder();
    }
}
