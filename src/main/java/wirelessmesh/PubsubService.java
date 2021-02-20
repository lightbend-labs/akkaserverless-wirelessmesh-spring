package wirelessmesh;

import com.google.protobuf.ByteString;

import java.io.IOException;

public interface PubsubService {
    void publish(ByteString event);
}
