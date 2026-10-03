package de.emphasize.time2.storage.persistence;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EventSystem {

  @Value("${eventBaseUrl}")
  String eventBaseUrl;

  public void postSSE(String topic, String source, String target, String content) {
    try {
      HttpURLConnection connection =
          (HttpURLConnection)
              new URI(eventBaseUrl + "event/?topic=" + topic + "&u=" + source + "&target=" + target)
                  .toURL()
                  .openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "text/plain;charset=UTF-8");
      connection.setDoOutput(true);
      try (OutputStream out = connection.getOutputStream()) {
        byte[] payload = content.getBytes("utf-8");
        out.write(payload, 0, payload.length);
      }
      try (BufferedReader br =
          new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
        br.readAllAsString();
      }
      connection.disconnect();
    } catch (Exception e) {
      throw new RuntimeException("posting SSE failed", e);
    }
  }
}
