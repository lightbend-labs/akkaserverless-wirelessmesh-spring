package wirelessmesh;

import io.cloudstate.javasupport.eventsourced.CommandContext;
import org.junit.Test;
import org.mockito.*;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import wirelessmesh.domain.CustomerLocationEntity;

import wirelessmesh.Wirelessmesh.*;
import domain.Domain.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CustomerLocationTest {

    String defaultCustomerLocationId = "customerId1";
    String accessToken = "accessToken";
    String room = "person-cave";

    @Test
    public void addCustomerLocationTest() {
        PubsubService pubsubService = Mockito.mock(GooglePubsubService.class);
        createAndAddCustomerLocation(defaultCustomerLocationId, null, pubsubService);
    }

    @Test
    public void addCustomerLocationWithNonAlphaNumericLocationIdTest() {
        String badCustomerLocationId = "bad/id";
        PubsubService pubsubService = Mockito.mock(GooglePubsubService.class);
        CommandContext context = Mockito.mock(CommandContext.class);
        CustomerLocationEntity customerLocation = new CustomerLocationEntity(null,
                pubsubService);

        customerLocation.addCustomerLocation(AddCustomerLocationCommand.newBuilder()
                .setCustomerLocationId(badCustomerLocationId)
                .setAccessToken(accessToken)
                .build(), context);

        Mockito.verify(context).fail("Customer location id must be alphanumeric");
        Mockito.verifyNoInteractions(pubsubService);
    }

    @Test
    public void addCustomerLocationWithBadAccessTokenTest() {
        PubsubService pubsubService = Mockito.mock(GooglePubsubService.class);
        CommandContext context = Mockito.mock(CommandContext.class);
        CustomerLocationEntity customerLocation = new CustomerLocationEntity(null,
                pubsubService);

        customerLocation.addCustomerLocation(AddCustomerLocationCommand.newBuilder()
                .setCustomerLocationId(defaultCustomerLocationId)
                .setAccessToken("bad/token")
                .build(), context);

        Mockito.verify(context).fail("Access token must be alphanumeric");
        Mockito.verifyNoInteractions(pubsubService);
    }

    @Test
    public void activateDevicesTest() {
        PubsubService pubsubService = Mockito.mock(GooglePubsubService.class);
        CustomerLocationEntity entity = createAndAddCustomerLocation(defaultCustomerLocationId,null, pubsubService);
        createAndActivateDevice(entity, "deviceId1", pubsubService);
        createAndActivateDevice(entity, "deviceId2", pubsubService);
        createAndActivateDevice(entity, "deviceId3", pubsubService);

        // Test get
        GetCustomerLocationCommand command = GetCustomerLocationCommand.newBuilder()
                .setCustomerLocationId(defaultCustomerLocationId)
                .build();

        CustomerLocation customerLocation = entity.getCustomerLocation(command, null);
        assertEquals(customerLocation.getDevicesList().size(), 3);
        assertEquals(customerLocation.getDevices(0).getDeviceId(), "deviceId1");
        assertEquals(customerLocation.getDevices(1).getDeviceId(), "deviceId2");
        assertEquals(customerLocation.getDevices(2).getDeviceId(), "deviceId3");
    }

    @Test
    public void removeCustomerLocationTest() {
        PubsubService pubsubService = Mockito.mock(GooglePubsubService.class);
        CommandContext context = Mockito.mock(CommandContext.class);
        CustomerLocationEntity entity = createAndAddCustomerLocation(defaultCustomerLocationId,null, pubsubService);
        CustomerLocationRemoved removed = CustomerLocationRemoved.newBuilder()
                .setCustomerLocationId(defaultCustomerLocationId)
                .build();

        entity.removeCustomerLocation(RemoveCustomerLocationCommand.newBuilder()
            .setCustomerLocationId(defaultCustomerLocationId).build(), context);

        Mockito.verify(context).emit(removed);
        Mockito.verify(pubsubService).publish(removed.toByteString());
        entity.customerLocationRemoved(removed); // Simulate event callback to drive state change.
        Mockito.reset(context);
        entity.getCustomerLocation(GetCustomerLocationCommand.newBuilder()
                .setCustomerLocationId(defaultCustomerLocationId).build(), context);

        Mockito.verify(context).fail("customerLocation does not exist.");
    }

    @Test
    public void assignRoomTest() {
        PubsubService pubsubService = Mockito.mock(GooglePubsubService.class);
        CommandContext context = Mockito.mock(CommandContext.class);
        CustomerLocationEntity entity = createAndAddCustomerLocation(defaultCustomerLocationId,null, pubsubService);
        createAndActivateDevice(entity, "deviceId1", pubsubService);
        createAndActivateDevice(entity, "deviceId2", pubsubService);
        createAndActivateDevice(entity, "deviceId3", pubsubService);

        RoomAssigned assigned = RoomAssigned.newBuilder()
                .setDeviceId("deviceId2")
                .setCustomerLocationId(defaultCustomerLocationId)
                .setRoom(room)
                .build();

        entity.assignRoom(AssignRoomCommand.newBuilder()
                .setDeviceId("deviceId2")
                .setCustomerLocationId(defaultCustomerLocationId)
                .setRoom(room)
                .build()
                , context);

        Mockito.verify(context).emit(assigned);
        Mockito.verify(pubsubService).publish(assigned.toByteString());
        entity.roomAssigned(assigned); // Simulate event callback to drive state change.
        GetCustomerLocationCommand command = GetCustomerLocationCommand.newBuilder()
                .setCustomerLocationId(defaultCustomerLocationId)
                .build();

        List<Device> expected = new ArrayList<Device>();
        expected.add(defaultDevice(defaultCustomerLocationId,"deviceId1"));
        expected.add(defaultDevice(defaultCustomerLocationId,"deviceId2").toBuilder().setRoom(room).build());
        expected.add(defaultDevice(defaultCustomerLocationId,"deviceId3"));
        CustomerLocation customerLocation = entity.getCustomerLocation(command, context);

        List<Device> sorted = customerLocation.getDevicesList().stream().sorted(Comparator
                .comparing(Device::getDeviceId)).collect(toList());

        assertEquals(sorted, expected);
    }

    @Test
    public void toggleNightlightTest() throws IOException {
        PubsubService pubsubService = Mockito.mock(GooglePubsubService.class);
        DeviceService deviceService = Mockito.mock(LifxDeviceService.class);
        CommandContext context = Mockito.mock(CommandContext.class);
        CustomerLocationEntity entity = createAndAddCustomerLocation(defaultCustomerLocationId, deviceService, pubsubService);
        createAndActivateDevice(entity, "deviceId1", pubsubService);
        createAndActivateDevice(entity, "deviceId2", pubsubService);
        createAndActivateDevice(entity, "deviceId3", pubsubService);

        NightlightToggled toggled = NightlightToggled.newBuilder()
                .setDeviceId("deviceId2")
                .setCustomerLocationId(defaultCustomerLocationId)
                .setNightlightOn(true)
                .build();

        entity.toggleNightlight(ToggleNightlightCommand.newBuilder()
                        .setDeviceId("deviceId2")
                        .setCustomerLocationId(defaultCustomerLocationId)
                        .build()
                , context);

        Mockito.verify(context).emit(toggled);
        Mockito.verify(pubsubService).publish(toggled.toByteString());
        Mockito.verify(deviceService).toggleNightlight(accessToken, "deviceId2");
        entity.nightlightToggled(toggled); // Simulate event callback to drive state change.
        GetCustomerLocationCommand command = GetCustomerLocationCommand.newBuilder()
                .setCustomerLocationId(defaultCustomerLocationId)
                .build();

        List<Device> expected = new ArrayList<Device>();
        expected.add(defaultDevice(defaultCustomerLocationId, "deviceId1"));
        expected.add(defaultDevice(defaultCustomerLocationId,"deviceId2").toBuilder().setNightlightOn(true)
                .build());

        expected.add(defaultDevice(defaultCustomerLocationId,"deviceId3"));
        CustomerLocation customerLocation = entity.getCustomerLocation(command, context);
        List<Device> sorted = customerLocation.getDevicesList().stream().sorted(Comparator.comparing(Device::getDeviceId)).collect(toList());
        assertEquals(sorted, expected);
    }

    private CustomerLocationEntity createAndAddCustomerLocation(String customerLocationId, DeviceService deviceService,
                                                                PubsubService pubsubService) {
        CommandContext context = Mockito.mock(CommandContext.class);
        CustomerLocationEntity customerLocation = new CustomerLocationEntity(deviceService,
                pubsubService);

        CustomerLocationAdded added = CustomerLocationAdded.newBuilder()
                .setCustomerLocationId(customerLocationId)
                .setAccessToken(accessToken)
                .build();

        customerLocation.addCustomerLocation(AddCustomerLocationCommand.newBuilder()
                .setCustomerLocationId(customerLocationId)
                .setAccessToken(accessToken)
                .build(), context);

        Mockito.verify(context).emit(added);
        Mockito.verify(pubsubService).publish(added.toByteString());

        customerLocation.customerLocationAdded(added); // Simulate event callback to drive state change.

        return customerLocation;
    }

    private void createAndActivateDevice(CustomerLocationEntity customerLocation, String deviceId,
                                         PubsubService pubsubService) {
        CommandContext context = Mockito.mock(CommandContext.class);
        DeviceActivated activated = DeviceActivated.newBuilder()
                .setCustomerLocationId(defaultCustomerLocationId)
                .setDeviceId(deviceId)
                .build();

        customerLocation.activateDevice(ActivateDeviceCommand.newBuilder()
                .setCustomerLocationId(defaultCustomerLocationId)
                .setDeviceId(deviceId)
                .build(), context);

        customerLocation.deviceActivated(activated); // Simulate event callback to drive state change.

        Mockito.verify(context).emit(activated);
        Mockito.verify(pubsubService).publish(activated.toByteString());
    }

    private Device defaultDevice(String customerLocationId, String deviceId) {
        return Device.newBuilder()
                .setDeviceId(deviceId)
                .setCustomerLocationId(customerLocationId)
                .setActivated(true).setRoom("")
                .setNightlightOn(false)
                .build();
    }
}
