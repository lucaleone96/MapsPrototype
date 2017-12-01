package lucaleone.mapsprototype;


import android.content.Context;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

public class ServerInterface {
    private final static String SERVER_URL = "http://vforvaiano.altervista.org/";

    /**
     * Restituisce i dati dal server
     */
    public static String getData(String[] field, String[] value, String scriptName) {
        String data = "";
        try{
            for(int i = 0; i < field.length; i++){
                data += field[i] + "=" + URLEncoder.encode(value[i],"UTF-8") + "&";
            }
            data = data.substring(0,data.length() - 1);
        }catch(Exception ex) {
            data = null;
            return data;
        }
        return executeHttpRequest(data, scriptName);
    }

    /**
     * Restituisce la risposta del server
     */
    private static String executeHttpRequest(String data, String scriptName) {
        String result = "";
        try{
            // Apre e setta la connessione
            URL url = new URL(SERVER_URL + scriptName);
            URLConnection connection = url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-type", "application/x-www-form-urlencoded");

            // Invia dati REQUEST
            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(data);
            outputStream.flush();
            outputStream.close();

            // Recupera risposta del server
            DataInputStream inputStream = new DataInputStream(connection.getInputStream());
            String inputLine;
            while((inputLine = inputStream.readLine()) != null) {
                result += inputLine;
            }
            inputStream.close();
        }catch(Exception ex) {
            result = null;
            return result;
        }
        return result;
    }
}