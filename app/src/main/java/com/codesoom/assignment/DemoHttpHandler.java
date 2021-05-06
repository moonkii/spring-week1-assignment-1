package com.codesoom.assignment;

import com.codesoom.assignment.models.Task;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DemoHttpHandler implements HttpHandler {
  private ObjectMapper objectMapper = new ObjectMapper();

  private List<Task> tasks = new ArrayList<>();

  private long newId = 0;

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    String method = exchange.getRequestMethod();
    URI uri = exchange.getRequestURI();
    String path = uri.getPath();

    if (path.equals("/tasks")) {
      handleCollection(exchange, method);
      return;
    }

    if (path.startsWith("/tasks/")) {
      Long id = Long.parseLong(path.substring("/tasks/".length()));
      handleItem(exchange, method, id);
      return;
    }
  }

  private void handleItem(HttpExchange exchange, String method, Long id)
      throws JsonProcessingException {
    Task task = findTask(id);

    if (task == null) {
      send(exchange, 404, "");
      return;
    }

    if (method.equals("GET")) {
      handleDetail(exchange, task);
    }

    if (method.equals("PUT") || method.equals("PATCH")) {
      handleUpdate(exchange, task);
    }

    if (method.equals("DELETE")) {
      handleDelete(exchange, task);
    }
  }

  private void handleDelete(HttpExchange exchange, Task task) {
    tasks.remove(task);

    send(exchange, 204, "");
  }

  private void handleUpdate(HttpExchange exchange, Task task) throws JsonProcessingException {
    String body = getBody(exchange);

    Task source = toTask(body);

    task.setTitle(source.getTitle());

    handleDetail(exchange, task);
  }

  private void handleDetail(HttpExchange exchange, Task task) {
    send(exchange, 200, taskToJson(task));
  }

  private Task findTask(Long id) {
    return tasks.stream()
        .filter(task -> task.getId().equals(id))
        .findFirst()
        .orElse(null);
  }

  private void handleCollection(HttpExchange exchange, String method)
      throws JsonProcessingException {
    if (method.equals("GET")) {
      handleList(exchange);
      return;
    }

    if (method.equals("POST")) {
      handleCreate(exchange);
    }
  }

  private void handleList(HttpExchange exchange) {
    send(exchange, 200, tasksToJson());
  }

  private void handleCreate(HttpExchange exchange) throws JsonProcessingException {
    String body = getBody(exchange);

    Task task = toTask(body);
    task.setId(generateId());
    tasks.add(task);

    send(exchange, 201, taskToJson(task));
  }

  private Long generateId() {
    newId += 1;
    return newId;
  }

  private void send(HttpExchange exchange, int statusCode, String content) {
    try {
      exchange.sendResponseHeaders(statusCode, content.getBytes().length);

      OutputStream outputStream = exchange.getResponseBody();
      outputStream.write(content.getBytes());
      outputStream.flush();
      outputStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private String getBody(HttpExchange exchange) {
    InputStream inputStream = exchange.getRequestBody();
    return new BufferedReader(new InputStreamReader(inputStream))
        .lines()
        .collect(Collectors.joining("\n"));
  }

  private Task toTask(String content) throws JsonProcessingException {
    return objectMapper.readValue(content, Task.class);
  }

  private String tasksToJson() {
    OutputStream outputStream = new ByteArrayOutputStream();

    try {
      objectMapper.writeValue(outputStream, tasks);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return outputStream.toString();
  }

  private String taskToJson(Task task) {
    OutputStream outputStream = new ByteArrayOutputStream();

    try {
      objectMapper.writeValue(outputStream, task);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return outputStream.toString();
  }
}
