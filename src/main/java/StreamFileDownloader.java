import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.nio.file.Files;
import java.util.stream.Stream;

public class StreamFileDownloader {

    private static HttpRequest.BodyPublisher ofMimeMultipartData(Path filePath) throws IOException {
        var byteArrays = List.of(
                "--boundary\r\n".getBytes(),
                "Content-Disposition: form-data; name=\"file\"; filename=\"".getBytes(),
                filePath.getFileName().toString().getBytes(),
                "\"\r\nContent-Type: text/plain\r\n\r\n".getBytes(),
                Files.readAllBytes(filePath),
                "\r\n--boundary--\r\n".getBytes());



        return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
    }

    public static void main(String[] args) {
        String baseUrl = "https://raw.githubusercontent.com/SeleniumHQ/selenium/refs/heads/trunk/javascript/selenium-webdriver/";
        String noteName = "CHANGES.md";

        try {
            // Create HttpClient
            HttpClient client = HttpClient.newHttpClient();

            // Build GET Request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + noteName))
                    .build();

            // Send request and print response
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            // Handle response for successful download
            if (response.statusCode() == 200) {
                Path path = Paths.get("./blob/" + noteName);
                try (InputStream inputStream = response.body();
                     FileOutputStream fileOutputStream = new FileOutputStream(path.toFile())) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, bytesRead);
                    }
                }
                FileContentVerifier.check(path.toFile().toString());

            } else {
                System.out.println("HTTP error occurred: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("An error occurred: " + e.getMessage());
        }
    }
}