package lambdawebsitemonitor;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

// Handler value: lambdawebsitemonitor.Handler
public class Handler implements RequestHandler<ScheduledEvent, Void> {

    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public Void handleRequest(ScheduledEvent event, Context context) {
        LambdaLogger logger = context.getLogger();
        String url = System.getenv("URL");

        var eventDetail = event.getDetail();
        if (eventDetail != null && eventDetail.containsKey(("is-test"))) {
            url = System.getenv("TEST_URL");
        }

        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder(
                URI.create(url))
                .header("User-Agent", "AWS Lambda")
                .build();

        logger.log("[INFO]: Sending request" + gson.toJson(request) + "\n");
        HttpResponse<String> response = null;
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            logger.log("[INFO]: Attempt #" + attempt + "\n");
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                logger.log("[INFO]: Request Sent\n");
                break;
            } catch (Exception e) {
                if (attempt == maxAttempts) {
                    logger.log("[ERROR]: " + e.getMessage() + "\n");
                    throw new RuntimeException("ERROR: There was an error sending the request!");
                }
            }
        }

        if (response.statusCode() != 200) {
            logger.log("[ERROR]: Site could not be reached. StatusCode: " + response.statusCode() + "\n");
            logger.log("[INFO]: Writing metric to CloudWatch (Site Availability: 0)\n");
            writeMetric("Site Availability", 0);
        } else {
            logger.log("[INFO]: Site is up with StatusCode: " + response.statusCode() + "\n");
            logger.log("[INFO]: Writing metric to CloudWatch (Site Availability: 200)\n");
            writeMetric("Site Availability", 200);
        }
        return null;
    }

    void writeMetric(String metricName, double metricValue) {
        var metricDatum = MetricDatum.builder()
                .metricName(metricName)
                .value(metricValue)
                .dimensions(Dimension.builder()
                        .name("Status")
                        .value("WebsiteStatusCode")
                        .build()
                ).build();

        var cloudWatchClient = CloudWatchClient.builder().build();
        cloudWatchClient.putMetricData(
                PutMetricDataRequest.builder()
                        .namespace("WebsiteMonitor")
                        .metricData(metricDatum)
                        .build()
        );

    }
}