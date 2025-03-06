//import cn.hutool.core.collection.CollUtil;
//import cn.hutool.core.io.FileUtil;
//import cn.hutool.core.util.StrUtil;
//import cn.hutool.json.JSONUtil;
//import com.tencent.wxcloudrun.util.Message;
//import com.tencent.wxcloudrun.util.MoonshotAiUtils;
//import com.tencent.wxcloudrun.util.RoleEnum;
//import org.junit.jupiter.api.Test;
//
//import java.util.List;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//import java.io.IOException;
//
//public class ApiTest {
//
//    @Test
//    void getModelList() {
//        System.out.println(MoonshotAiUtils.getModelList());
//    }
//
//    @Test
//    void uploadFile() {
//        System.out.println(MoonshotAiUtils.uploadFile(FileUtil.file("/Users/steven/Desktop/test.pdf")));
//    }
//
//    @Test
//    void getFileList() {
//        System.out.println(MoonshotAiUtils.getFileList());
//    }
//
//    @Test
//    void deleteFile() {
//        System.out.println(MoonshotAiUtils.deleteFile("co17orilnl9coc91noh0"));
//        System.out.println(MoonshotAiUtils.getFileList());
//    }
//
//    @Test
//    void getFileContent() {
//        System.out.println(MoonshotAiUtils.getFileContent("co18sokudu6bc6fqdhhg"));
//    }
//
//    @Test
//    void getFileDetail() {
//        System.out.println(MoonshotAiUtils.getFileDetail("co18sokudu6bc6fqdhhg"));
//    }
//
//    @Test
//    void estimateTokenCount() {
//        List<Message> messages = CollUtil.newArrayList(
//                new Message(RoleEnum.system.name(), "你是kimi AI"),
//                new Message(RoleEnum.user.name(), "hello")
//        );
//        System.out.println(MoonshotAiUtils.estimateTokenCount("moonshot-v1-8k", messages));
//    }
//
//    @Test
//    void chat(){
//        List<Message> messages = CollUtil.newArrayList(
//                new Message(RoleEnum.system.name(), "你是一个暴躁老哥，你对用户提出的问题要以一个吵架的语气回复，但是又有一定的深度，并不是无脑硬怼"),
//                new Message(RoleEnum.user.name(), "什么是人工智能")
//        );
//        long start = System.currentTimeMillis();
//
//
//        // 返回数据如下
//        //data: {"id":"chatcmpl-67c96964971a7a505b6ff17a","object":"chat.completion.chunk","created":1741252964,"model":"moonshot-v1-8k","choices":[{"index":0,"delta":{"content":"。"},"finish_reason":null}],"system_fingerprint":"fpv0_ca1d2527"}
//        //
//        //data: {"id":"chatcmpl-67c96964971a7a505b6ff17a","object":"chat.completion.chunk","created":1741252964,"model":"moonshot-v1-8k","choices":[{"index":0,"delta":{},"finish_reason":"stop","usage":{"prompt_tokens":39,"completion_tokens":211,"total_tokens":250}}],"system_fingerprint":"fpv0_ca1d2527"}
//        //
//        //data: [DONE]
//        // 根据换行符分隔成为字符串数组
//        String[] jsonDataStrings = MoonshotAiUtils.chatNew("moonshot-v1-8k", messages).split("\n");
//        String res = extractAndConcatenateContent(jsonDataStrings);
//        System.out.println(res);
//
//        // 统计以下代码耗时
//        System.out.println("耗时："+(System.currentTimeMillis()-start));
//    }
//    public static String extractAndConcatenateContent(String[] jsonDataStrings) {
//        ObjectMapper objectMapper = new ObjectMapper();
//        StringBuilder result = new StringBuilder();
//
//        for (String jsonData : jsonDataStrings) {
//            if ("".equals(jsonData) || StrUtil.equals("[DONE]", jsonData.replace("data: ", ""))) {
//                continue;
//            }
//            try {
//                JsonNode rootNode = objectMapper.readTree(jsonData.replace("data: ", ""));
//                JsonNode choicesNode = rootNode.path("choices");
//
//                for (JsonNode choiceNode : choicesNode) {
//                    JsonNode deltaNode = choiceNode.path("delta");
//                    if (deltaNode.has("content")) {
//                        String content = deltaNode.get("content").asText();
//                        result.append(content);
//                    }
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        return result.toString();
//    }
//}

