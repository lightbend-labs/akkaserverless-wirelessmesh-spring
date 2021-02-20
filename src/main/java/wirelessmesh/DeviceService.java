package wirelessmesh;

import java.io.IOException;

public interface DeviceService {
    void toggleNightlight(String accessToken, String deviceId) throws IOException;
}
