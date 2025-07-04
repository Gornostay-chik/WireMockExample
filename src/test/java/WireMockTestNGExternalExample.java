import static com.github.tomakehurst.wiremock.client.WireMock.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonParser;
import io.restassured.RestAssured;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import java.lang.reflect.Type;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.List;

import com.google.gson.JsonObject;

public class WireMockTestNGExternalExample {

    /*
    @BeforeSuite
    @BeforeTest
    @BeforeGroups
    @BeforeClass
    @BeforeMethod
    @Test
    @AfterMethod
    @AfterClass
    @AfterGroups
    @AfterTest
    @AfterSuite

    Описание этапов:

    @BeforeSuite / @AfterSuite Выполняются один раз до/после всего набора тестов во всём фреймворке.

    @BeforeTest / @AfterTest Выполняются до/после каждого <test> блока в  testng.xml
    (группы классов).

    @BeforeGroups / @AfterGroups Запускаются перед/после всех @Test-методов, отнесённых к указанной группе.

    @BeforeClass / @AfterClass Выполняются один раз до/после всех тестовых методов в текущем классе.

    @BeforeMethod / @AfterMethod Выполняются перед/после каждого @Test-метода в классе.

    @Test Сам тестовый метод.
     */

    static String host = "localhost";
    static int port = 8181;
    static String baseUrl = "http://"+host+":"+port;
    private HttpClient client;
    private Gson gson;

    // Предполагается, что WireMock уже запущен на localhost:8181
    @BeforeClass
    public void setUp() {
        // Настраиваем клиент WireMock для подключения к уже запущенному серверу
        // sudo docker run -it --rm   -p 8181:8080   --name wiremock   wiremock/wiremock:3.13.0
        configureFor(host, port);

    }

    @BeforeMethod
    public void setUpClient() {
        client = HttpClient.newHttpClient();
        gson = new Gson();
    }

    @AfterMethod
    public void tearDown() {
        // После каждого теста очищаем все запросы и заглушки,
        // чтобы не было влияния одного теста на другой.
        resetAllRequests();
        removeAllMappings();
    }

    @Test
    public void textGetMinStub() {
        // Регистрируем заглушку: для GET-запроса на URL "/some/thing"
        // вернём ответ с заголовком "Content-Type: text/plain" и телом "Hello world!".
        // https://wiremock.org/docs/standalone/admin-api-reference/
        stubFor(get(urlEqualTo("/some/thing"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Hello world!")));

        // Отправляем запросы через RestAssured на уже запущенный WireMock-сервер.
        int statusValid = RestAssured.get(baseUrl+"/some/thing").getStatusCode();
        int statusInvalid = RestAssured.get(baseUrl+"/some/thing/else").getStatusCode();

        // Проверяем: для корректного URL статус должен быть 200, для неправильного – 404
        Assert.assertEquals(statusValid, 200, "Успешный запрос должен вернуть статус 200");
        Assert.assertEquals(statusInvalid, 404, "Невалидный запрос должен вернуть статус 404");
    }

    @Test

    public void jsonPostMaxStub () {

        String apiKey = "\"my-secret-key\"";
        String testUrl = "/complex/endpoint";

        /*

        Key Differences Between GET and POST

    GET Requests:
        Purpose: Retrieve data from a server.
        Data Location: Data is sent in the URL as path or query parameters.
        Success Status: Expect a 200 status code for successful data retrieval.

    POST Requests:
        Purpose: Send data to a server to create or update a resource, such as submitting a form, uploading a file, or adding a new item to a database.
        Data Location: Data is sent in the request body.
        Success Status: Expect a 201 status code for successful resource creation.

         These differences clarify when to use each method. POST requests, in particular, require careful handling of the request body.

         */


        // /__admin/mappings

        JsonObject resultData = new JsonObject();
        resultData.addProperty("result", "success");
        resultData.addProperty("message", "Resource created successfully");

        stubFor(post(urlPathEqualTo(testUrl))
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
                //.withBasicAuth("admin", "secret")
                .withHeader("X-API-KEY", equalTo(apiKey))
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
                        .withBody(resultData.toString())
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


        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + testUrl + "?queryParam1=value1&param2=123"))
                .header("X-Request-Header", "HeaderValue-Test")
                .header("X-API-Key", apiKey)
                .header("Content-Type", "application/json")
                .header("Cookie", "sessionId=abc123")
                .POST(HttpRequest.BodyPublishers.ofString("{\"data\":{\"id\":12345}}") )
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                System.out.println("Accessed protected endpoint successfully!");
                // Assuming response is JSON
                System.out.println(response.body());

             /*   Gson gson = new Gson();
                Type resultListType = new TypeToken<List<Result>>() {}.getType();
                List<Result> results = gson.fromJson(response.body(), resultListType);

                for (Result res : results) {
                    System.out.println(res);
                }
            */

                Result result = gson.fromJson(response.body(), Result.class);

                // Print all details of the todo item

                System.out.println("Result: " + result.result);
                System.out.println("Message: " + result.message);

            } else {
                System.out.println("An HTTP error occurred: " + response.statusCode());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


 /*
 Example for CURL

curl -X POST "http://localhost:8181/complex/endpoint?queryParam1=value1&param2=123" \
  -H "Content-Type: application/json" \
  -H "X-Request-Header: HeaderValue-Test" \
  -u admin:secret \
  --cookie "sessionId=abc123" \
  -d '{"data":{"id":12345}}'
*/

    }

    @Test
    public void jsonGetMinStub() throws IOException, InterruptedException {

        String pathURL = "/some/json";

        stubFor(get(urlEqualTo(pathURL))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody( "  {\n" +
                                "    \"description\": \"Summarize Q4 performance metrics\",\n" +
                                "    \"done\": false,\n" +
                                "    \"id\": 3,\n" +
                                "    \"title\": \"Finish project report\"\n" +
                                "  }" )));


        // Create the HTTP GET request using the path parameter

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + pathURL))
                .GET()
                .build();


        // Send the request and get the response

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) { // Вариант для одного объекта
            Todo todo = gson.fromJson(response.body(), Todo.class);

            // Print all details of the todo item

            System.out.println("ID: " + todo.id);
            System.out.println("Title: " + todo.title);
            System.out.println("Description: " + todo.description);
            System.out.println("Done: " + todo.done);}

        else {

            System.out.printf("\nUnexpected Status Code: %d%n", response.statusCode());
            var error = JsonParser.parseString(response.body()).getAsJsonObject();
            System.out.println("Error Details: " + error);

        }

    }

    @Test
    public void jsonGetArrayMinStub() throws IOException, InterruptedException {

        // Base URL for the API
        String pathURL = "/some/jsonarray";

        stubFor(get(urlEqualTo(pathURL))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[\n" +
                                "  {\n" +
                                "    \"description\": \"Summarize Q4 performance metrics\",\n" +
                                "    \"done\": false,\n" +
                                "    \"id\": 3,\n" +
                                "    \"title\": \"Finish project report\"\n" +
                                "  }\n" +
                                "]")));


        // Create the HTTP GET request using the path parameter

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + pathURL))
                .GET()
                .build();


        // Send the request and get the response

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) { //Вариант для массива объектов

            // Type token is only used for generic types, such as Lists
            Type todoListType = new TypeToken<List<Todo>>() {}.getType();
            List<Todo> todos = gson.fromJson(response.body(), todoListType);

            System.out.println("Todos retrieved successfully:");
            for (Todo todo : todos) {
                System.out.println("Title: " + todo.title);
                System.out.println("Description: " + todo.description);
                System.out.println("Done: " + todo.done);
            }}
        else {

                System.out.printf("\nUnexpected Status Code: %d%n", response.statusCode());
                var error = JsonParser.parseString(response.body()).getAsJsonObject();
                System.out.println("Error Details: " + error);

            }

    }

}
