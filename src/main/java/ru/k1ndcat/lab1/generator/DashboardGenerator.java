package ru.k1ndcat.lab1.generator;

import ru.k1ndcat.lab1.dto.HandlerDto;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

public class DashboardGenerator {

    public static void main(String[] args) throws Exception {
        String appUrl = System.getenv("APP_URL");
        String grafanaUrl = System.getenv("GRAFANA_URL");
        String grafanaUser = System.getenv("GRAFANA_ADMIN_USER");
        String grafanaPassword = System.getenv("GRAFANA_ADMIN_PASSWORD");

        if (grafanaUrl == null || grafanaUrl.isBlank()
                || grafanaUser == null || grafanaUser.isBlank()
                || grafanaPassword == null || grafanaPassword.isBlank()) {
            throw new IllegalArgumentException(
                    "Не заданы параметры подключения к Grafana"
            );
        }

        if (appUrl == null || appUrl.isBlank()) {
            throw new IllegalArgumentException("Не задана APP_URL");
        }

        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build()) {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(appUrl + "/handlers"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IllegalStateException("GET /handlers вернул HTTP " + response.statusCode());
            }

            JsonMapper mapper = JsonMapper.builder().build();

            HandlerDto[] handlers = mapper.readValue(response.body(), HandlerDto[].class);

            var dashboard = mapper.createObjectNode();
            dashboard.put("uid", "items-monitoring");
            dashboard.put("title", "Items monitoring");
            dashboard.put("editable", false);
            dashboard.put("refresh", "5s");

            var time = dashboard.putObject("time");
            time.put("from", "now-15m");
            time.put("to", "now");

            var variables = dashboard.putObject("templating").putArray("list");

            for (String name : new String[]{"datacenter", "pod"}) {
                var variable = variables.addObject();

                variable.put("name", name);
                variable.put("label", name);
                variable.put("type", "query");
                variable.put("multi", true);
                variable.put("includeAll", true);
                variable.put("allValue", ".*");
                variable.put("refresh", 1);
                variable.put("sort", 1);

                var variableDatasource = variable.putObject("datasource");
                variableDatasource.put("type", "prometheus");
                variableDatasource.put("uid", "prometheus-main");

                String variableQuery;

                if ("datacenter".equals(name)) {
                    variableQuery = """
                            label_values(jvm_info{job="items-service"}, datacenter)
                            """.strip();
                } else {
                    variableQuery = "label_values(jvm_info{job=\"items-service\",datacenter=~\"${datacenter:regex}\"}, pod)";
                }

                variable.put("query", variableQuery);

                var current = variable.putObject("current");
                current.putArray("text").add("All");
                current.putArray("value").add("$__all");
            }

            var panels = dashboard.putArray("panels");

            int panelId = 1;
            int row = 0;
            for (HandlerDto handler : handlers) {
                var panel = panels.addObject();
                panel.put("id", panelId++);
                panel.put("title", handler.method() + " " + handler.path() + " RPS");
                panel.put("type", "timeseries");

                var datasource = panel.putObject("datasource");
                datasource.put("type", "prometheus");
                datasource.put("uid", "prometheus-main");

                var position = panel.putObject("gridPos");
                position.put("x", 0);
                position.put("y", row * 8);
                position.put("w", 24);
                position.put("h", 8);

                var targets = panel.putArray("targets");
                var target = targets.addObject();

                target.put("refId", "A");
                String rpsQuery = """
                        sum(
                          rate(
                            http_server_requests_seconds_count{
                              job="items-service",
                              method="%s",
                              uri="%s",
                              datacenter=~"${datacenter:regex}",
                              pod=~"${pod:regex}"
                            }[$__rate_interval]
                          )
                        )
                        """.formatted(handler.method(), handler.path()).strip();
                target.put("expr", rpsQuery);
                target.put("legendFormat", "RPS");
                target.put("range", true);
                target.put("instant", false);

                row++;

                var latencyPanel = panels.addObject();
                latencyPanel.put("id", panelId++);
                latencyPanel.put("title", handler.method() + " " + handler.path() + " Latency");
                latencyPanel.put("type", "timeseries");
                latencyPanel.set("datasource", datasource.deepCopy());

                var latencyPosition = latencyPanel.putObject("gridPos");
                latencyPosition.put("x", 0);
                latencyPosition.put("y", row * 8);
                latencyPosition.put("w", 24);
                latencyPosition.put("h", 8);

                latencyPanel.putObject("fieldConfig")
                        .putObject("defaults")
                        .put("unit", "s");

                var latencyTargets = latencyPanel.putArray("targets");

                double[] quantiles = {0.50, 0.95, 0.99};
                String[] legends = {"p50", "p95", "p99"};
                String[] references = {"A", "B", "C"};

                for (int i = 0; i < quantiles.length; i++) {
                    String latencyQuery = """
                            histogram_quantile(
                              %s,
                              sum by (le) (
                                rate(
                                  http_server_requests_seconds_bucket{
                                    job="items-service",
                                    method="%s",
                                    uri="%s",
                                    datacenter=~"${datacenter:regex}",
                                    pod=~"${pod:regex}"
                                  }[$__rate_interval]
                                )
                              )
                            )
                            """.formatted(
                            Double.toString(quantiles[i]),
                            handler.method(),
                            handler.path()
                    ).strip();

                    var latencyTarget = latencyTargets.addObject();
                    latencyTarget.put("refId", references[i]);
                    latencyTarget.put("expr", latencyQuery);
                    latencyTarget.put("legendFormat", legends[i]);
                    latencyTarget.put("range", true);
                    latencyTarget.put("instant", false);
                }

                row++;

                var errorsPanel = panels.addObject();
                errorsPanel.put("id", panelId++);
                errorsPanel.put(
                        "title",
                        handler.method() + " " + handler.path() + " Errors"
                );
                errorsPanel.put("type", "timeseries");
                errorsPanel.set("datasource", datasource.deepCopy());

                var errorsPosition = errorsPanel.putObject("gridPos");
                errorsPosition.put("x", 0);
                errorsPosition.put("y", row * 8);
                errorsPosition.put("w", 24);
                errorsPosition.put("h", 8);

                var errorsTarget = errorsPanel.putArray("targets").addObject();

                String errorsQuery = """
                        sum by (status) (
                          rate(
                            http_server_requests_seconds_count{
                              job="items-service",
                              method="%s",
                              uri="%s",
                              status=~"[45]..",
                              datacenter=~"${datacenter:regex}",
                              pod=~"${pod:regex}"
                            }[$__rate_interval]
                          )
                        )
                        """.formatted(handler.method(), handler.path()).strip();

                errorsTarget.put("refId", "A");
                errorsTarget.put("expr", errorsQuery);
                errorsTarget.put("legendFormat", "HTTP {{status}}");
                errorsTarget.put("range", true);
                errorsTarget.put("instant", false);

                row++;
            }

            var payload = mapper.createObjectNode();
            payload.set("dashboard", dashboard);
            payload.put("overwrite", true);

            String credentials = grafanaUser + ":" + grafanaPassword;
            String authorization = "Basic " + Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            HttpRequest grafanaRequest = HttpRequest.newBuilder()
                    .uri(URI.create(grafanaUrl + "/api/dashboards/db"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", authorization)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            mapper.writeValueAsString(payload)
                    ))
                    .build();

            HttpResponse<String> grafanaResponse = client.send(
                    grafanaRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (grafanaResponse.statusCode() != 200) {
                throw new IllegalStateException(
                        "Создание дашборда: HTTP "
                                + grafanaResponse.statusCode()
                                + ", ответ: " + grafanaResponse.body()
                );
            }

            IO.println(grafanaResponse.body());

            IO.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dashboard));

        }
    }
}
