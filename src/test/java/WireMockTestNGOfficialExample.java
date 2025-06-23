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
    public void textGetMinStub() {
        // Настраиваем заглушку: для GET-запроса по адресу "/some/thing" вернуть "Hello world!"
        stubFor(get(urlEqualTo("/some/thing"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Hello world!")));

        // Отправляем запросы с помощью RestAssured
        int statusCodeValid = RestAssured.get("http://localhost:8080/some/thing").getStatusCode();
        int statusCodeInvalid = RestAssured.get("http://localhost:8080/some/thing/else").getStatusCode();

        // Проверяем: валидному URL должен соответствовать HTTP-статус 200, а невалидному — 404
        assertThat(statusCodeValid, is(200));
        assertThat(statusCodeInvalid, is(404));
    }
}
