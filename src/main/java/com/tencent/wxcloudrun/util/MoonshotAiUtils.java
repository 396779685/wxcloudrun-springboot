package com.tencent.wxcloudrun.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.NonNull;
import lombok.SneakyThrows;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class MoonshotAiUtils {

    private static final String API_KEY = "sk-ZHcZjqEMWZTCY3ER47SJ4rGo9042F9hpytjsUXHXke3lKk9C";
    private static final String MODELS_URL = "https://api.moonshot.cn/v1/models";
    private static final String FILES_URL = "https://api.moonshot.cn/v1/files";
    private static final String ESTIMATE_TOKEN_COUNT_URL = "https://api.moonshot.cn/v1/tokenizers/estimate-token-count";
    private static final String CHAT_COMPLETION_URL = "https://api.moonshot.cn/v1/chat/completions";

    public static String getModelList() {
        return getCommonRequest(MODELS_URL)
                .execute()
                .body();
    }

    public static String uploadFile(@NonNull File file) {
        return getCommonRequest(FILES_URL)
                .method(Method.POST)
                .header("purpose", "file-extract")
                .form("file", file)
                .execute()
                .body();
    }

    public static String getFileList() {
        return getCommonRequest(FILES_URL)
                .execute()
                .body();
    }

    public static String deleteFile(@NonNull String fileId) {
        return getCommonRequest(FILES_URL + "/" + fileId)
                .method(Method.DELETE)
                .execute()
                .body();
    }

    public static String getFileDetail(@NonNull String fileId) {
        return getCommonRequest(FILES_URL + "/" + fileId)
                .execute()
                .body();
    }

    public static String getFileContent(@NonNull String fileId) {
        return getCommonRequest(FILES_URL + "/" + fileId + "/content")
                .execute()
                .body();
    }

    public static String estimateTokenCount(@NonNull String model, @NonNull List<Message> messages) {
        String requestBody = new JSONObject()
                .putOpt("model", model)
                .putOpt("messages", messages)
                .toString();
        return getCommonRequest(ESTIMATE_TOKEN_COUNT_URL)
                .method(Method.POST)
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(requestBody)
                .execute()
                .body();
    }

    @SneakyThrows
    public static void chat(@NonNull String model, @NonNull List<Message> messages) {
        String requestBody = new JSONObject()
                .putOpt("model", model)
                .putOpt("messages", messages)
                .putOpt("stream", true)
                .toString();
        Request okhttpRequest = new Request.Builder()
                .url(CHAT_COMPLETION_URL)
                .post(RequestBody.create(requestBody, MediaType.get(ContentType.JSON.getValue())))
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();
        Call call = new OkHttpClient().newCall(okhttpRequest);
        Response okhttpResponse = call.execute();
        BufferedReader reader = new BufferedReader(okhttpResponse.body().charStream());
        String line;
        while ((line = reader.readLine()) != null) {
            if (StrUtil.isBlank(line)) {
                continue;
            }
            if (JSONUtil.isTypeJSON(line)) {
                Optional.of(JSONUtil.parseObj(line))
                        .map(x -> x.getJSONObject("error"))
                        .map(x -> x.getStr("message"))
                        .ifPresent(x -> System.out.println("error: " + x));
                return;
            }
            line = StrUtil.replace(line, "data: ", StrUtil.EMPTY);
            if (StrUtil.equals("[DONE]", line) || !JSONUtil.isTypeJSON(line)) {
                return;
            }
            Optional.of(JSONUtil.parseObj(line))
                    .map(x -> x.getJSONArray("choices"))
                    .filter(CollUtil::isNotEmpty)
                    .map(x -> (JSONObject) x.get(0))
                    .map(x -> x.getJSONObject("delta"))
                    .map(x -> x.getStr("content"))
                    .ifPresent(x -> System.out.println("rowData: " + x));
        }
    }

    private static HttpRequest getCommonRequest(@NonNull String url) {
        return HttpRequest.of(url).header(Header.AUTHORIZATION, "Bearer " + API_KEY);
    }



    @SneakyThrows
    public static JSONObject chatNew(@NonNull String model, @NonNull List<Message> messages) {
        String requestBody = new JSONObject()
                .putOpt("model", model)
                .putOpt("messages", messages)
                .putOpt("stream", true)
                .toString();

            String url= CHAT_COMPLETION_URL;
            HttpResponse response = HttpRequest.post(url)
                    .contentType(ContentType.JSON.getValue())
                    .body(requestBody)
                    .header("Authorization", "Bearer " + API_KEY)
                    .execute();

            System.out.printf("status:" + response.getStatus());
            String body = response.body();
            //System.out.printf("addJobInfo:" + body);
            JSONObject jo = new JSONObject();
            jo.put("body",body);
            jo.put("status",response.getStatus());
            return jo;
    }
}
