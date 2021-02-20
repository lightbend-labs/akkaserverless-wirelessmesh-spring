package wirelessmesh;

import com.google.protobuf.Descriptors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DescriptorsConfiguration {
    @Bean
    public Descriptors.ServiceDescriptor customerLocationEntityServiceDescriptor() {
        return Wirelessmesh.getDescriptor().findServiceByName("WirelessmeshService");
    }

    @Bean
    public Descriptors.FileDescriptor[] customerLocationEntityFileDescriptors() {
        return new Descriptors.FileDescriptor[]{domain.Domain.getDescriptor()};
    }
}
