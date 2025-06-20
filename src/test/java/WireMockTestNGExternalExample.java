import static com.github.tomakehurst.wiremock.client.WireMock.*;
import io.restassured.RestAssured;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class WireMockTestNGExternalExample {

    // Предполагается, что WireMock уже запущен на localhost:8181
    @BeforeClass
    public void setUp() {
        // Настраиваем клиент WireMock для подключения к уже запущенному серверу
        configureFor("localhost", 8181);
    }

    @AfterMethod
    public void tearDown() {
        // После каждого теста очищаем все запросы и заглушки,
        // чтобы не было влияния одного теста на другой.
      //  resetAllRequests();
      //  removeAllMappings();
    }

    @Test
    public void exactUrlOnlyTestNG() {
        // Регистрируем заглушку: для GET-запроса на URL "/some/thing"
        // вернём ответ с заголовком "Content-Type: text/plain" и телом "Hello world!".
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

        /*

curl -X POST "http://localhost:8181/complex/endpoint?queryParam1=value1&param2=123" \
  -H "Content-Type: application/json" \
  -H "X-Request-Header: HeaderValue-Test" \
  -u admin:secret \
  --cookie "sessionId=abc123" \
  -d '{"data":{"id":12345}}'



         */

        // Отправляем запросы через RestAssured на уже запущенный WireMock-сервер.
        int statusValid = RestAssured.get("http://localhost:8181/some/thing").getStatusCode();
        int statusInvalid = RestAssured.get("http://localhost:8181/some/thing/else").getStatusCode();

        // Проверяем: для корректного URL статус должен быть 200, для неправильного – 404
        Assert.assertEquals(statusValid, 200, "Успешный запрос должен вернуть статус 200");
        Assert.assertEquals(statusInvalid, 404, "Невалидный запрос должен вернуть статус 404");
    }
}
