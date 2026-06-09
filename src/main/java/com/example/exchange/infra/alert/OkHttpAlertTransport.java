/*
 * 檔案用途：Alert backend HTTP transport，負責將 OperationalAlert 以 JSON webhook 送出。
 */
package com.example.exchange.infra.alert;

import com.example.exchange.application.service.AlertDispatchService;
import com.example.exchange.domain.model.dto.AlertDispatchResult;
import com.example.exchange.domain.model.dto.OperationalAlert;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OkHttpAlertTransport implements AlertDispatchService.AlertTransport {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    @Override
    public AlertDispatchResult post(String webhookUrl, OperationalAlert alert, int timeoutMs) {
        byte[] payload;
        try {
            payload = objectMapper.writeValueAsBytes(alert);
        } catch (JsonProcessingException e) {
            return AlertDispatchResult.failed(0, "ALERT_PAYLOAD_SERIALIZATION_FAILED");
        }

        OkHttpClient client = okHttpClient.newBuilder()
                .callTimeout(Duration.ofMillis(timeoutMs))
                .build();
        Request request = new Request.Builder()
                .url(webhookUrl)
                .post(RequestBody.create(payload, JSON))
                .header("Content-Type", JSON.toString())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return AlertDispatchResult.delivered(response.code());
            }
            return AlertDispatchResult.failed(response.code(), "ALERT_BACKEND_HTTP_" + response.code());
        } catch (IOException e) {
            return AlertDispatchResult.failed(0, "ALERT_BACKEND_IO_FAILURE");
        }
    }
}
