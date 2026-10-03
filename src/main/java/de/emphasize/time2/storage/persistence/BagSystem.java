package de.emphasize.time2.storage.persistence;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class BagSystem {

  @Value("${eventBaseUrl}")
  String eventBaseUrl;

  public String postBag(String content) {
    try {
      HttpURLConnection connection =
          (HttpURLConnection) new URI(eventBaseUrl + "bag/").toURL().openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setDoOutput(true);
      try (OutputStream out = connection.getOutputStream()) {
        byte[] payload = content.getBytes("utf-8");
        out.write(payload, 0, payload.length);
      }
      String response = null;
      try (BufferedReader br =
          new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
        response = br.readAllAsString();
      }
      connection.disconnect();
      var map = new ObjectMapper().readValue(response, Map.class);
      return (String) map.get("bid");
    } catch (Exception e) {
      throw new RuntimeException("posting bag failed", e);
    }
  }

  public String getBag(String bid) {
    try {
      HttpURLConnection connection =
          (HttpURLConnection) new URI(eventBaseUrl + "bag/?bid=" + bid).toURL().openConnection();
      String response = null;
      try (BufferedReader br =
          new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
        response = br.readAllAsString();
      }
      connection.disconnect();
      return response;
    } catch (Exception e) {
      throw new RuntimeException("getting bag failed", e);
    }
  }
}
