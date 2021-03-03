package wirelessmesh;

import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.stereotype.Service;

/**
 * A GCP/Spring implementation of PubSubService.
 */
@Service
public class GooglePubsubService implements PubsubService {

    private final String TOPIC_NAME = "wirelessmesh";

    private final PubSubTemplate pubsubTemplate;

    @Autowired
    public GooglePubsubService(PubSubTemplate pubSubTemplate) {
        this.pubsubTemplate = pubSubTemplate;
    }

    /**
     * Publish to google pubsub.
     * @param event the event to publish as a ByteString
     */
    @Override
    public void publish(ByteString event) {
        pubsubTemplate.publish(TOPIC_NAME, event);
    }
}
