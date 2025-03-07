package com.tencent.wxcloudrun.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.wxcloudrun.util.Message;
import com.tencent.wxcloudrun.util.MoonshotAiUtils;
import com.tencent.wxcloudrun.util.RoleEnum;
import org.jetbrains.annotations.NotNull;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
     *
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
     *
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

    private static Map<String, List<Message>> staticMap = new HashMap<>();
    private static String noticeMsg = "查询中，请回复1继续等待";
    @PostMapping(value = "/api/getMsg")
    JSONObject getMsg(@RequestBody JSONObject request) {
        logger.info("/api/getMsg post 入参: {}", request.toJSONString());

        String FromUserName = request.getString("FromUserName");
        String requestContent = request.getString("Content").trim();
        if("1".equals(requestContent)){
            // 获取上次的数据
            if(staticMap.get(FromUserName)!=null){
                List<Message> msgList = staticMap.get(FromUserName);
                // 获取最后放入的一条数据
                Message lastMsg = msgList.get(msgList.size()-1);
                String content = noticeMsg;
                if(!lastMsg.getRole().equals(RoleEnum.user.name())){
                    content = lastMsg.getContent();
                }
                JSONObject jo = getJsonObject(FromUserName, content);
                return jo;
            }
        }

        /*String msg = "{" +
            "\"Content\":\"哇哈\"," +
            "\"CreateTime\":1741164957," +
            "\"ToUserName\":\"gh_ece0086d4736\"," +
            "\"FromUserName\":\"oSiRW6Cu5aS3fNRnbnnE6E0PodcY\"," +
            "\"MsgType\":\"text\"," +
            "\"MsgId\":24927491174065560" +
            "}";*/
        // 调用chat 方法的时候开启计时，超过4s直接返回错误信息
        String response = chatWithTimeout(FromUserName, requestContent, 3, TimeUnit.SECONDS);

        //String res = chat(request.getString("Content"));
        JSONObject jo = getJsonObject(FromUserName, response);
        return jo;
    }

    @NotNull
    private static JSONObject getJsonObject(String FromUserName, String res) {
        long timestamp = System.currentTimeMillis() / 1000;
        JSONObject jo = new JSONObject();
        jo.put("ToUserName", FromUserName);
        jo.put("FromUserName", "gh_ece0086d4736");
        jo.put("CreateTime", timestamp);
        jo.put("MsgType", "text");
        jo.put("Content", res);
        return jo;
    }

    /**
     * // 返回数据如下
     *         //data: {"id":"chatcmpl-67c96964971a7a505b6ff17a","object":"chat.completion.chunk","created":1741252964,"model":"moonshot-v1-8k","choices":[{"index":0,"delta":{"content":"。"},"finish_reason":null}],"system_fingerprint":"fpv0_ca1d2527"}
     *         //
     *         //data: {"id":"chatcmpl-67c96964971a7a505b6ff17a","object":"chat.completion.chunk","created":1741252964,"model":"moonshot-v1-8k","choices":[{"index":0,"delta":{},"finish_reason":"stop","usage":{"prompt_tokens":39,"completion_tokens":211,"total_tokens":250}}],"system_fingerprint":"fpv0_ca1d2527"}
     *         //
     *         //data: [DONE]
     * @param FromUserName
     * @param textStr
     * @return
     */
    String chat(String FromUserName, String textStr){
        //启动计时，如果超过时间没有返回数据，则返回给用户请重试

        List<Message> messages = staticMap.get(FromUserName);
        if(messages==null || messages.size()==0){
            messages = CollUtil.newArrayList();
        }
        messages.add(new Message(RoleEnum.user.name(), textStr));

        long start = System.currentTimeMillis();

        cn.hutool.json.JSONObject responseJson = MoonshotAiUtils.chatNew("moonshot-v1-8k", messages);
        int status = responseJson.getInt("status");
        String response = responseJson.getStr("body");
        if(status==200){
            // 根据换行符分隔成为字符串数组
            String[] jsonDataStrings = response.split("\n");
            response = extractAndConcatenateContent(jsonDataStrings);
            System.out.println(response);
            messages.add(new Message(RoleEnum.system.name(), response));
            staticMap.put(FromUserName, messages);
        }
        // 统计以下代码耗时
        System.out.println("耗时："+(System.currentTimeMillis()-start));
        return response;
    }
    public static String extractAndConcatenateContent(String[] jsonDataStrings) {
        ObjectMapper objectMapper = new ObjectMapper();
        StringBuilder result = new StringBuilder();

        for (String jsonData : jsonDataStrings) {
            if ("".equals(jsonData) || StrUtil.equals("[DONE]", jsonData.replace("data: ", ""))) {
                continue;
            }
            try {
                JsonNode rootNode = objectMapper.readTree(jsonData.replace("data: ", ""));
                JsonNode choicesNode = rootNode.path("choices");

                for (JsonNode choiceNode : choicesNode) {
                    JsonNode deltaNode = choiceNode.path("delta");
                    if (deltaNode.has("content")) {
                        String content = deltaNode.get("content").asText();
                        result.append(content);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result.toString();
    }

    // 包装chat方法，使其可以在线程池中运行
    private Callable<String> chatCallable(String FromUserName, String content) {
        return () -> chat(FromUserName, content);
    }

    // 带超时机制的聊天方法
    public String chatWithTimeout(String FromUserName, String content, long timeout, TimeUnit unit) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(chatCallable(FromUserName, content));

        try {
            // 尝试在指定时间内获取结果
            return future.get(timeout, unit);
        } catch (TimeoutException e) {
            // 超时处理，返回错误信息
            return noticeMsg;
        } catch (InterruptedException | ExecutionException e) {
            // 其他异常处理，返回错误信息
            return "Error: " + e.getMessage();
        } finally {
            // 关闭线程池
            executor.shutdown();
        }
    }

    public static void main(String[] args) {
        /*//获取时间戳
        long timestamp = System.currentTimeMillis() / 1000;
        System.out.println(timestamp);
        // 解析时间戳成 yyyy-MM-dd HH:mm:ss 格式的日期时间字符串
        String dateTimeStr = LocalDateTime.now().toString();
        System.out.println(dateTimeStr);*/
        String param = "{\n" +
                "    \"Content\": \"西游记都\",\n" +
                "    \"CreateTime\": 1741164957,\n" +
                "    \"ToUserName\": \"gh_ece0086d4736\",\n" +
                "    \"FromUserName\": \"oSiRW6Cu5aS3fNRnbnnE6E0PodcY\",\n" +
                "    \"MsgType\": \"text\",\n" +
                "    \"MsgId\": 24927491174065560\n" +
                "}";
        CounterController controller = new CounterController(null);
        JSONObject jo = controller.getMsg(JSONObject.parseObject(param));
        System.out.println(jo.toJSONString());
    }
}