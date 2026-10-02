package ch.bedag.dap.hellodata.sidecars.airflow.client;

import ch.bedag.dap.hellodata.sidecars.airflow.client.dag.AirflowDag;
import ch.bedag.dap.hellodata.sidecars.airflow.client.dag.AirflowDagsResponse;
import ch.bedag.dap.hellodata.sidecars.airflow.client.exception.UnexpectedResponseException;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowRole;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowRolesResponse;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowUserResponse;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowUsersResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AirflowClientPagingTest {

    /**
     * Unit tests of the shared paging loop, independent of HTTP.
     */
    @Nested
    class FetchAllPages {

        private record Page(List<Integer> items, int total) {
        }

        @Test
        void fetchesAllPagesUntilTotalIsReached() throws Exception {
            List<Integer> source = IntStream.range(0, 250).boxed().toList();
            List<int[]> requests = new ArrayList<>();

            List<Integer> result = AirflowClient.fetchAllPages((offset, limit) -> {
                requests.add(new int[]{offset, limit});
                return new Page(slice(source, offset, limit), source.size());
            }, Page::items, Page::total);

            assertThat(result).containsExactlyElementsOf(source);
            assertThat(requests).extracting(r -> r[0]).containsExactly(0, 100, 200);
            assertThat(requests).extracting(r -> r[1]).containsOnly(100);
        }

        @Test
        void singleRequestWhenEverythingFitsOnePage() throws Exception {
            List<Integer> source = IntStream.range(0, 42).boxed().toList();
            List<Integer> offsets = new ArrayList<>();

            List<Integer> result = AirflowClient.fetchAllPages((offset, limit) -> {
                offsets.add(offset);
                return new Page(slice(source, offset, limit), source.size());
            }, Page::items, Page::total);

            assertThat(result).hasSize(42);
            assertThat(offsets).containsExactly(0);
        }

        @Test
        void exactMultipleOfPageLimitDoesNotRequestAnExtraPage() throws Exception {
            List<Integer> source = IntStream.range(0, 200).boxed().toList();
            List<Integer> offsets = new ArrayList<>();

            List<Integer> result = AirflowClient.fetchAllPages((offset, limit) -> {
                offsets.add(offset);
                return new Page(slice(source, offset, limit), source.size());
            }, Page::items, Page::total);

            assertThat(result).hasSize(200);
            assertThat(offsets).containsExactly(0, 100);
        }

        @Test
        void advancesByReturnedSizeWhenServerCapsPageBelowRequestedLimit() throws Exception {
            // e.g. maximum_page_limit configured lower than our PAGE_LIMIT
            List<Integer> source = IntStream.range(0, 120).boxed().toList();
            List<Integer> offsets = new ArrayList<>();

            List<Integer> result = AirflowClient.fetchAllPages((offset, limit) -> {
                offsets.add(offset);
                return new Page(slice(source, offset, Math.min(limit, 50)), source.size());
            }, Page::items, Page::total);

            assertThat(result).containsExactlyElementsOf(source);
            assertThat(offsets).containsExactly(0, 50, 100);
        }

        @Test
        void stopsOnEmptyPageWhenEntriesAreRemovedDuringPaging() throws Exception {
            // total_entries says 150, but entries disappear after the first page - the old loop never terminated here
            List<Integer> offsets = new ArrayList<>();

            List<Integer> result = AirflowClient.fetchAllPages((offset, limit) -> {
                offsets.add(offset);
                return offset == 0
                        ? new Page(IntStream.range(0, 100).boxed().toList(), 150)
                        : new Page(List.of(), 150);
            }, Page::items, Page::total);

            assertThat(result).hasSize(100);
            assertThat(offsets).containsExactly(0, 100);
        }

        @Test
        void stopsOnNullItems() throws Exception {
            List<Integer> result = AirflowClient.fetchAllPages((offset, limit) -> new Page(null, 10), Page::items, Page::total);

            assertThat(result).isEmpty();
        }

        @Test
        void emptySourceReturnsEmptyList() throws Exception {
            List<Integer> offsets = new ArrayList<>();

            List<Integer> result = AirflowClient.fetchAllPages((offset, limit) -> {
                offsets.add(offset);
                return new Page(List.of(), 0);
            }, Page::items, Page::total);

            assertThat(result).isEmpty();
            assertThat(offsets).containsExactly(0);
        }

        @Test
        void propagatesFetchErrorsInsteadOfReturningPartialResult() {
            assertThatThrownBy(() -> AirflowClient.fetchAllPages((offset, limit) -> {
                if (offset > 0) {
                    throw new IOException("boom");
                }
                return new Page(IntStream.range(0, 100).boxed().toList(), 200);
            }, Page::items, Page::total)).isInstanceOf(IOException.class).hasMessage("boom");
        }

        private List<Integer> slice(List<Integer> source, int offset, int limit) {
            return source.subList(Math.min(offset, source.size()), Math.min(offset + limit, source.size()));
        }
    }

    /**
     * End-to-end tests of the public client methods against a fake Airflow REST API.
     */
    @Nested
    class AgainstFakeAirflow {

        private HttpServer server;
        private AirflowClient client;
        private final List<Map<String, String>> requests = new CopyOnWriteArrayList<>();

        @BeforeEach
        void setUp() throws IOException {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.start();
            client = new AirflowClient("localhost", server.getAddress().getPort(), "admin", "admin");
        }

        @AfterEach
        void tearDown() throws IOException {
            client.close();
            server.stop(0);
        }

        @Test
        void dagsReturnsAllDagsBeyondFirstPage() throws Exception {
            serveList("/api/v1/dags", "dags", 230, i -> String.format("{\"dag_id\":\"dag_%03d\"}", i), limit -> limit);

            AirflowDagsResponse response = client.dags();

            assertThat(response.getDags()).hasSize(230);
            assertThat(response.getTotalEntries()).isEqualTo(230);
            assertThat(response.getDags()).extracting(AirflowDag::getId).first().isEqualTo("dag_000");
            assertThat(response.getDags()).extracting(AirflowDag::getId).last().isEqualTo("dag_229");
            assertThat(requests).extracting(r -> r.get("offset")).containsExactly("0", "100", "200");
            assertThat(requests).extracting(r -> r.get("limit")).containsOnly("100");
            assertThat(requests).extracting(r -> r.get("order_by")).containsOnly("dag_id");
        }

        @Test
        void rolesReturnsAllRolesBeyondFirstPage() throws Exception {
            serveList("/api/v1/roles", "roles", 130, i -> String.format("{\"name\":\"role_%03d\",\"actions\":[]}", i), limit -> limit);

            AirflowRolesResponse response = client.roles();

            assertThat(response.getRoles()).hasSize(130);
            assertThat(response.getTotalEntries()).isEqualTo(130);
            assertThat(response.getRoles()).extracting(AirflowRole::getName).contains("role_000", "role_129");
            assertThat(requests).extracting(r -> r.get("offset")).containsExactly("0", "100");
            assertThat(requests).extracting(r -> r.get("order_by")).containsOnly("name");
        }

        @Test
        void usersReturnsAllUsersAndKeepsOrderByEmail() throws Exception {
            serveList("/api/v1/users", "users", 205, i -> String.format("{\"username\":\"user%03d\",\"email\":\"user%03d@example.com\"}", i, i), limit -> limit);

            AirflowUsersResponse response = client.users();

            assertThat(response.getUsers()).hasSize(205);
            assertThat(response.getTotalEntries()).isEqualTo(205);
            assertThat(response.getUsers()).extracting(AirflowUserResponse::getUsername).contains("user000", "user204");
            assertThat(requests).extracting(r -> r.get("offset")).containsExactly("0", "100", "200");
            assertThat(requests).extracting(r -> r.get("order_by")).containsOnly("email");
        }

        @Test
        void usersTerminatesWhenUsersAreDeletedDuringPaging() throws Exception {
            // first page reports 150 users, then 60 of them are deleted - the next page comes back empty
            server.createContext("/api/v1/users", exchange -> {
                Map<String, String> query = recordRequest(exchange);
                int offset = Integer.parseInt(query.get("offset"));
                int total = offset == 0 ? 150 : 90;
                int remaining = offset == 0 ? 100 : Math.max(0, total - offset);
                String items = IntStream.range(offset, offset + remaining)
                        .mapToObj(i -> String.format("{\"username\":\"user%03d\"}", i))
                        .collect(Collectors.joining(","));
                respond(exchange, 200, String.format("{\"total_entries\":%d,\"users\":[%s]}", total, items));
            });

            AirflowUsersResponse response = client.users();

            assertThat(response.getUsers()).hasSize(100);
            assertThat(requests).hasSize(2);
        }

        @Test
        void dagsHonoursServerSidePageCap() throws Exception {
            // server caps every page at 40 entries regardless of the requested limit
            serveList("/api/v1/dags", "dags", 100, i -> String.format("{\"dag_id\":\"dag_%03d\"}", i), limit -> Math.min(limit, 40));

            AirflowDagsResponse response = client.dags();

            assertThat(response.getDags()).hasSize(100);
            assertThat(response.getDags()).extracting(AirflowDag::getId).doesNotHaveDuplicates();
            assertThat(requests).extracting(r -> r.get("offset")).containsExactly("0", "40", "80");
        }

        @Test
        void errorOnLaterPageIsPropagated() {
            server.createContext("/api/v1/dags", exchange -> {
                Map<String, String> query = recordRequest(exchange);
                if (!"0".equals(query.get("offset"))) {
                    respond(exchange, 500, "{\"detail\":\"boom\"}");
                    return;
                }
                String items = IntStream.range(0, 100)
                        .mapToObj(i -> String.format("{\"dag_id\":\"dag_%03d\"}", i))
                        .collect(Collectors.joining(","));
                respond(exchange, 200, String.format("{\"total_entries\":150,\"dags\":[%s]}", items));
            });

            assertThatThrownBy(() -> client.dags()).isInstanceOf(UnexpectedResponseException.class);
        }

        /**
         * Serves {@code total} generated entries under {@code field}, paged by the request's offset/limit like Airflow does.
         */
        private void serveList(String path, String field, int total, IntFunction<String> itemJson, IntUnaryOperator effectiveLimit) {
            server.createContext(path, exchange -> {
                Map<String, String> query = recordRequest(exchange);
                int offset = Integer.parseInt(query.getOrDefault("offset", "0"));
                int limit = effectiveLimit.applyAsInt(Integer.parseInt(query.getOrDefault("limit", "100")));
                String items = IntStream.range(offset, Math.min(offset + limit, total))
                        .mapToObj(itemJson)
                        .collect(Collectors.joining(","));
                respond(exchange, 200, String.format("{\"total_entries\":%d,\"%s\":[%s]}", total, field, items));
            });
        }

        private Map<String, String> recordRequest(HttpExchange exchange) {
            Map<String, String> query = URLEncodedUtils.parse(exchange.getRequestURI(), StandardCharsets.UTF_8).stream()
                    .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
            requests.add(query);
            return query;
        }

        private void respond(HttpExchange exchange, int status, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
