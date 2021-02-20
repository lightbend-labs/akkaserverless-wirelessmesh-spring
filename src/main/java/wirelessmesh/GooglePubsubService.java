package wirelessmesh;

import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class GooglePubsubService implements PubsubService {

    private final String topicName = "wirelessmesh";

    // Check if publishing is on or off.
    boolean publishOn = System.getenv().containsKey("GOOGLE_PROJECT_ID")
            && System.getenv().containsKey("PUBLISH_EVENTS")
            && (System.getenv("PUBLISH_EVENTS").equals("ON"));

    /**
     * Publish to google pubsub.
     * @param event the event to publish as a ByteString
     */
    @Override
    public void publish(ByteString event) {
        if (publishOn) {
            TopicName topic = TopicName.of(System.getenv("GOOGLE_PROJECT_ID"), topicName);
            Publisher publisher = null;

            try {
                // Create a publisher instance with default settings bound to the topic
                publisher = Publisher.newBuilder(topic).build();
                PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(event).build();

                // Once published, returns a server-assigned message id (unique within the topic)
                ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);
                String messageId = messageIdFuture.get();
            } catch (Exception ex) {
                Logger.getAnonymousLogger().log(Level.WARNING, "Unable to publish to google pubsub-" + ex.getMessage());
            } finally {
                if (publisher != null) {
                    // When finished with the publisher, shutdown to free up resources.
                    publisher.shutdown();

                    try {
                        publisher.awaitTermination(1, TimeUnit.MINUTES);
                    } catch (Exception ex) {
                        Logger.getAnonymousLogger().log(Level.WARNING, "Unable shutdown google pubsub-" + ex.getMessage());
                    }
                }
            }
        }
    }
}
