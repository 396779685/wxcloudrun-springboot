package com.tencent.wxcloudrun.controller;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tencent.wxcloudrun.config.ApiResponse;
import com.tencent.wxcloudrun.dto.CounterRequest;
import com.tencent.wxcloudrun.model.Counter;
import com.tencent.wxcloudrun.service.CounterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

/**
 * counter控制器
 */
@RestController

public class CounterController {

  final CounterService counterService;
  final Logger logger;

  public CounterController(@Autowired CounterService counterService) {
    this.counterService = counterService;
    this.logger = LoggerFactory.getLogger(CounterController.class);
  }


  /**
   * 获取当前计数
   * @return API response json
   */
  @GetMapping(value = "/api/count")
  ApiResponse get() {
    logger.info("/api/count get request");
    Optional<Counter> counter = counterService.getCounter(1);
    Integer count = 0;
    if (counter.isPresent()) {
      count = counter.get().getCount();
    }

    return ApiResponse.ok(count);
  }


  /**
   * 更新计数，自增或者清零
   * @param request {@link CounterRequest}
   * @return API response json
   */
  @PostMapping(value = "/api/count")
  ApiResponse create(@RequestBody CounterRequest request) {
    logger.info("/api/count post request, action: {}", request.getAction());

    Optional<Counter> curCounter = counterService.getCounter(1);
    if (request.getAction().equals("inc")) {
      Integer count = 1;
      if (curCounter.isPresent()) {
        count += curCounter.get().getCount();
      }
      Counter counter = new Counter();
      counter.setId(1);
      counter.setCount(count);
      counterService.upsertCount(counter);
      return ApiResponse.ok(count);
    } else if (request.getAction().equals("clear")) {
      if (!curCounter.isPresent()) {
        return ApiResponse.ok(0);
      }
      counterService.clearCount(1);
      return ApiResponse.ok(0);
    } else {
      return ApiResponse.error("参数action错误");
    }
  }

  @PostMapping(value = "/api/getMsg")
  String getMsg(@RequestBody JSONObject request) {
    logger.info("/api/getMsg post request, action: {}", request.toJSONString());
    String msg = "{" +
            "\"Content\":\"哇哈\"," +
            "\"CreateTime\":1741164957," +
            "\"ToUserName\":\"gh_ece0086d4736\"," +
            "\"FromUserName\":\"oSiRW6Cu5aS3fNRnbnnE6E0PodcY\"," +
            "\"MsgType\":\"text\"," +
            "\"MsgId\":24927491174065560" +
            "}";
    String FromUserName = request.getString("FromUserName");
    long timestamp = System.currentTimeMillis() / 1000;
    String result = "<xml>\n" +
            "  <ToUserName><![CDATA["+FromUserName+"]]></ToUserName>\n" +
            "  <FromUserName><![CDATA[gh_ece0086d4736]]></FromUserName>\n" +
            "  <CreateTime>"+timestamp+"</CreateTime>\n" +
            "  <MsgType><![CDATA[text]]></MsgType>\n" +
            "  <Content><![CDATA[你好]]></Content>\n" +
            "</xml>";
    return result;
  }

  public static void main(String[] args) {
    //获取时间戳
    long timestamp = System.currentTimeMillis() / 1000;
    System.out.println(timestamp);
    // 解析时间戳成 yyyy-MM-dd HH:mm:ss 格式的日期时间字符串
    String dateTimeStr = LocalDateTime.now().toString();
    System.out.println(dateTimeStr);
  }
}