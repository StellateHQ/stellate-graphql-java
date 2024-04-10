package co.stellate.stellate;

import java.util.HashMap;
import java.util.Map;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.simple.JSONObject;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.instrumentation.*;
import graphql.execution.instrumentation.parameters.*;
import graphql.introspection.IntrospectionQuery;
import graphql.schema.GraphQLSchema;

/**
 * This creates a {@link graphql.execution.instrumentation.Instrumentation}
 * that logs information about all requests and their response to Stellate. It
 * also automatically synchronizes the GraphQL schema to Stellate (only once on
 * the first request that is observed).
 */
public class StellateInstrumentation implements Instrumentation {
    /**
     * The name of your Stellate service.
     */
    private String serviceName;

    /**
     * The logging token for your Stellate service, see
     * https://stellate.co/docs/graphql-metrics/metrics-get-started#create-your-own-logging-token
     */
    private String token;

    private boolean hasSyncedSchema;

    public StellateInstrumentation(String serviceName, String token) {
        this.serviceName = serviceName;
        this.token = token;
        this.hasSyncedSchema = false;
    };

    public InstrumentationContext<ExecutionResult> beginExecution(InstrumentationExecutionParameters parameters, InstrumentationState state) {
        long startNanos = System.nanoTime();
        return new SimpleInstrumentationContext<ExecutionResult>() {
            @Override
            public void onCompleted(ExecutionResult result, Throwable t) {
                long endNanos = System.nanoTime();
                String resultJSON = new JSONObject(result.toSpecification()).toJSONString();

                Map<String, Object> payload = new HashMap();
                payload.put("operation", parameters.getQuery());
                payload.put("method", "POST");
                payload.put("responseSize", resultJSON.length());
                payload.put("responseHash", createBlake3Hash(resultJSON));
                payload.put("elapsed", (endNanos - startNanos) / 1_000_000);
                payload.put("operationName", parameters.getOperation());
                payload.put("variablesHash", createBlake3Hash(new JSONObject(parameters.getVariables()).toJSONString()));
                payload.put("errors", result.getErrors().size() > 0 ? result.getErrors() : null);
                payload.put("statusCode", 200);

                sendStellateRequest("log", new JSONObject(payload).toJSONString());
            }
        };
    }

    public GraphQLSchema instrumentSchema(GraphQLSchema schema, InstrumentationExecutionParameters parameters, InstrumentationState state) {
        if (hasSyncedSchema) {
            return schema;
        }

        hasSyncedSchema = true;

        GraphQL graphQL = GraphQL.newGraphQL(schema).build();
        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(IntrospectionQuery.INTROSPECTION_QUERY)
                .build();
        ExecutionResult executionResult = graphQL.execute(executionInput);
        String introspectionJSON = new JSONObject(executionResult.getData()).toJSONString();

        sendStellateRequest("schema", "{\"schema\":" + introspectionJSON + "}");

        return schema;
    }

    /**
     * Utility function that asychronously sends a HTTP POST request to
     * Stellate, for the purpose of either Metrics Logging or Schema Syncing.
     *
     * @param endpoint The endpoint that the request should be sent to ("/log"
     *                 or "/schema")
     * @param payload The body of the POST request.
     */
    private void sendStellateRequest(String endpoint, String payload) {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://" + serviceName + ".stellate.sh/" + endpoint))
                .header("Content-Type", "application/json")
                .header("Stellate-Logging-Token", token)
                .header("Stellate-Schema-Token", token)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenAccept(res -> {
                    if (res.statusCode() >= 300) {
                        System.out.println("Failed to send Stellate request");
                    }
                });
    }

    /**
     * Utility function to create a blake3 hash for any input string. It
     * returns a UInt32, which needs to be represented as a long in Java.
     *
     * @param str The string that should be hashed.
     * @return The blake3 hash (UInt32) of the given string.
     */
    private long createBlake3Hash(String str) {
        int val = 0;
        int strlen = str.length();

        if (strlen == 0) {
          return (long) val;
        }

        for (int i = 0; i < strlen; ++i) {
          int code = Character.codePointAt(str, i);
          val = (val << 5) - val + code;
        }

        // Transform into uInt32, which means we need to use a long type which
        // has 64 bit because Java does not have unsigned integers
        return (long) val & 0xffffffffL;
    }

}
