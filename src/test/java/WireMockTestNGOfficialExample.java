import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import io.restassured.RestAssured;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

public class WireMockTestNGOfficialExample {

    private WireMockServer wireMockServer;

    @BeforeClass
    public void setUp() {
        // Инициализация WireMock-сервера на порту 8080
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(8080));
        wireMockServer.start();
        // Настроим WireMock-клиент
        configureFor("localhost", 8080);
    }

    @AfterClass
    public void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    public void exactUrlOnly() {
        // Настраиваем заглушку: для GET-запроса по адресу "/some/thing" вернуть "Hello world!"
        stubFor(get(urlEqualTo("/some/thing"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Hello world!")));

        stubFor(post(urlPathEqualTo("/complex/endpoint"))
                // --------------------------
                // Условия сопоставления запроса (request matching):
                // --------------------------
                // Сопоставление URL: метод выбирает только путь (без учета query string)
                // Использование urlPathEqualTo позволяет сравнивать часть пути без параметров.

                // Сопоставление параметров запроса: проверяет, что query parameter "queryParam1" равен "value1"
                .withQueryParam("queryParam1", equalTo("value1"))

                // Сопоставление заголовков: например, заголовок "X-Request-Header" должен содержать строку, удовлетворяющую регулярному выражению
                .withHeader("X-Request-Header", matching("HeaderValue-.*"))

                // Сопоставление cookies: cookie с именем "sessionId" должна иметь значение "abc123"
                .withCookie("sessionId", equalTo("abc123"))

                // Аутентификация: базовая авторизация с заданными логином и паролем
                .withBasicAuth("admin", "secret")

                // Сопоставление тела запроса: например, JSON, в котором по пути $.data.id ожидается значение "12345"
                .withRequestBody(matchingJsonPath("$.data.id", equalTo("12345")))

                // --------------------------
                // Определение ответа (response definition):
                // --------------------------
                .willReturn(aResponse()
                        // HTTP статус ответа – например, 201 (Created)
                        .withStatus(201)

                        // Текстовое сообщение статуса (не всегда используется, но может быть полезно)
                        .withStatusMessage("Created")

                        // Заголовки ответа
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-Response-Header", "SomeValue")

                        // Тело ответа можно задать напрямую – здесь возвращается JSON-строка
                        .withBody("{ \"result\": \"success\", \"message\": \"Resource created successfully\" }")
                        // Альтернативно, можно задать тело из файла:
                        // .withBodyFile("response.json")

                        // Задержка перед отправкой ответа (в миллисекундах) для имитации сетевой задержки
                        .withFixedDelay(500)

                        // Если требуется динамическая трансформация ответа, можно указать трансформеры
                        .withTransformers("response-template")

                        // Передача параметров в трансформер (например, для вставки динамических значений)
                        .withTransformerParameter("timestamp", "2025-06-05T14:15:00Z")
                )
        );

        // Отправляем запросы с помощью RestAssured
        int statusCodeValid = RestAssured.get("http://localhost:8080/some/thing").getStatusCode();
        int statusCodeInvalid = RestAssured.get("http://localhost:8080/some/thing/else").getStatusCode();

        // Проверяем: валидному URL должен соответствовать HTTP-статус 200, а невалидному — 404
        assertThat(statusCodeValid, is(200));
        assertThat(statusCodeInvalid, is(404));
    }
}
