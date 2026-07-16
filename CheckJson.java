import java.io.FileReader;
import com.google.gson.Gson;
import java.io.File;

public class CheckJson {
    public static void main(String[] args) {
        try {
            Gson gson = new Gson();
            File file = new File("src/main/resources/assets/xeb/lang/en_us.json");
            System.out.println("Checking en_us.json: " + file.getAbsolutePath());
            Object obj = gson.fromJson(new FileReader(file), Object.class);
            System.out.println("en_us.json is VALID Gson!");

            File fileEs = new File("src/main/resources/assets/xeb/lang/es_mx.json");
            System.out.println("Checking es_mx.json: " + fileEs.getAbsolutePath());
            Object objEs = gson.fromJson(new FileReader(fileEs), Object.class);
            System.out.println("es_mx.json is VALID Gson!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
