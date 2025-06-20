import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class Main {

    public static void main(String[] args) {

        configureFor("localhost", 8181);

        stubFor(get(urlEqualTo("/some/thing"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Hello world!")));

    }
}
