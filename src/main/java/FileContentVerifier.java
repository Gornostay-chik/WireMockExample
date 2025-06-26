import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FileContentVerifier {
    public static void check(String noteName) {
          try (BufferedReader reader = new BufferedReader(new FileReader(noteName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            System.out.println("File error occurred: " + e.getMessage());
        }
    }
}