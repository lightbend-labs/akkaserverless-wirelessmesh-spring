package wirelessmesh;

import org.springframework.stereotype.Service;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;

/**
 * A LIFX restful client, for a bulb standing in for an addressable wireless mesh device.
 */
@Service
public class LifxDeviceService implements DeviceService {

    /**
     * This will set the device nightlight to the opposite value on the physical device (bulb).
     *
     * @param accessToken Necessary for secure communication to the external API.
     * @param deviceId The deviceId that also much match the LIFX deviceId in order to control the bulb.
     * @throws IOException
     */
    @Override
    public void toggleNightlight(String accessToken, String deviceId) throws IOException {
        URL url = new URL("https://api.lifx.com/v1/lights/" + deviceId + "/toggle");
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization","Bearer " + accessToken);
        conn.setRequestProperty("Content-Type","application/json");
        conn.setRequestMethod("POST");

        conn.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
        wr.flush();
        wr.close();

        conn.getResponseCode();
        conn.disconnect();
    }
}
