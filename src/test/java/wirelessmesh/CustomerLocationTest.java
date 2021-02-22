//package wirelessmesh;
//
//import io.cloudstate.javasupport.eventsourced.CommandContext;
//import org.mockito.*;
//import org.testng.Assert;
//import org.testng.annotations.*;
//import wirelessmesh.domain.CustomerLocationEntity;
//
//import wirelessmesh.DeviceService;
//import wirelessmesh.PubsubService;
//import wirelessmesh.Wirelessmesh.*;
//import domain.Domain.*;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.List;
//
//import static java.util.stream.Collectors.toList;
//
//public class CustomerLocationTest {
//
//    String customerLocationId = "customerId1";
//    String accessToken = "accessToken";
//    String room = "person-cave";
//
//    @Test
//    public void addCustomerLocationTest() {
//        createAndAddCustomerLocation();
//    }
//
//    private void createAndActivateDevice(CustomerLocationEntity customerLocation, String deviceId) {
//        CommandContext context = Mockito.mock(CommandContext.class);
//        DeviceActivated activated = DeviceActivated.newBuilder()
//                .setCustomerLocationId(customerLocationId)
//                .setDeviceId(deviceId)
//                .build();
//
//        customerLocation.activateDevice(ActivateDeviceCommand.newBuilder()
//                .setCustomerLocationId(customerLocationId)
//                .setDeviceId(deviceId)
//                .build(), context);
//
//        customerLocation.deviceActivated(activated); // Simulate event callback to drive state change.
//
//        Mockito.verify(context).emit(activated);
//    }
//
//    @Test
//    public void activateDevicesTest() {
//        CustomerLocationEntity entity = createAndAddCustomerLocation();
//        createAndActivateDevice(entity, "deviceId1");
//        createAndActivateDevice(entity, "deviceId2");
//        createAndActivateDevice(entity, "deviceId3");
//
//        // Test get.
//        GetCustomerLocationCommand command = GetCustomerLocationCommand.newBuilder()
//                .setCustomerLocationId(customerLocationId)
//                .build();
//
//        CustomerLocation customerLocation = entity.getCustomerLocation(command, null);
//        Assert.assertEquals(customerLocation.getDevicesList().size(), 3);
//        Assert.assertEquals(customerLocation.getDevices(0).getDeviceId(), "deviceId1");
//        Assert.assertEquals(customerLocation.getDevices(1).getDeviceId(), "deviceId2");
//        Assert.assertEquals(customerLocation.getDevices(2).getDeviceId(), "deviceId3");
//    }
//
//    @Test
//    public void removeCustomerLocationTest() {
//        CommandContext context = Mockito.mock(CommandContext.class);
//        CustomerLocationEntity entity = createAndAddCustomerLocation();
//        CustomerLocationRemoved removed = CustomerLocationRemoved.newBuilder()
//                .setCustomerLocationId(customerLocationId)
//                .build();
//
//        entity.removeCustomerLocation(RemoveCustomerLocationCommand.newBuilder()
//            .setCustomerLocationId(customerLocationId).build(), context);
//
//        Mockito.verify(context).emit(removed);
//        entity.customerLocationRemoved(removed); // Simulate event callback to drive state change.
//        Mockito.reset(context);
//        entity.getCustomerLocation(GetCustomerLocationCommand.newBuilder().setCustomerLocationId(customerLocationId).build(), context);
//        Mockito.verify(context).fail("customerLocation does not exist.");
//    }
//
//    @Test
//    public void assignRoomTest() {
//        CommandContext context = Mockito.mock(CommandContext.class);
//        CustomerLocationEntity entity = createAndAddCustomerLocation();
//        createAndActivateDevice(entity, "deviceId1");
//        createAndActivateDevice(entity, "deviceId2");
//        createAndActivateDevice(entity, "deviceId3");
//
//        RoomAssigned assigned = RoomAssigned.newBuilder()
//                .setDeviceId("deviceId2")
//                .setCustomerLocationId(customerLocationId)
//                .setRoom(room)
//                .build();
//
//        entity.assignRoom(AssignRoomCommand.newBuilder()
//                .setDeviceId("deviceId2")
//                .setRoom(room)
//                .build()
//                , context);
//
//        Mockito.verify(context).emit(assigned);
//        entity.roomAssigned(assigned); // Simulate event callback to drive state change.
//        GetCustomerLocationCommand command = GetCustomerLocationCommand.newBuilder()
//                .setCustomerLocationId(customerLocationId)
//                .build();
//
//        List<Device> expected = new ArrayList<Device>();
//        expected.add(defaultDevice("deviceId1"));
//        expected.add(defaultDevice("deviceId2").toBuilder().setRoom(room).build());
//        expected.add(defaultDevice("deviceId3"));
//        CustomerLocation customerLocation = entity.getCustomerLocation(command, context);
//
//        List<Device> sorted = customerLocation.getDevicesList().stream().sorted(Comparator
//                .comparing(Device::getDeviceId)).collect(toList());
//
//        Assert.assertEquals(sorted, expected);
//    }
//
//    @Test
//    public void toggleNightlightTest() throws IOException {
//        CommandContext context = Mockito.mock(CommandContext.class);
//        CustomerLocationEntity entity = createAndAddCustomerLocation();
//        createAndActivateDevice(entity, "deviceId1");
//        createAndActivateDevice(entity, "deviceId2");
//        createAndActivateDevice(entity, "deviceId3");
//
//        NightlightToggled toggled = NightlightToggled.newBuilder()
//                .setDeviceId("deviceId2")
//                .setCustomerLocationId(customerLocationId)
//                .setNightlightOn(true)
//                .build();
//
//        entity.toggleNightlight(ToggleNightlightCommand.newBuilder()
//                        .setDeviceId("deviceId2")
//                        .build()
//                , context);
//
//        Mockito.verify(context).emit(toggled);
//        entity.nightlightToggled(toggled); // Simulate event callback to drive state change.
//        GetCustomerLocationCommand command = GetCustomerLocationCommand.newBuilder()
//                .setCustomerLocationId(customerLocationId)
//                .build();
//
//        List<Device> expected = new ArrayList<Device>();
//        expected.add(defaultDevice("deviceId1"));
//        expected.add(defaultDevice("deviceId2").toBuilder().setNightlightOn(true).build());
//        expected.add(defaultDevice("deviceId3"));
//        CustomerLocation customerLocation = entity.getCustomerLocation(command, context);
//        List<Device> sorted = customerLocation.getDevicesList().stream().sorted(Comparator.comparing(Device::getDeviceId)).collect(toList());
//        Assert.assertEquals(sorted, expected);
//    }
//
//    private CustomerLocationEntity createAndAddCustomerLocation() {
//        CommandContext context = Mockito.mock(CommandContext.class);
//        CustomerLocationEntity customerLocation = new CustomerLocationEntity(customerLocationId);
//
//        CustomerLocationAdded added = CustomerLocationAdded.newBuilder()
//                .setCustomerLocationId(customerLocationId)
//                .setAccessToken(accessToken)
//                .build();
//
//        customerLocation.addCustomerLocation(AddCustomerLocationCommand.newBuilder()
//                .setCustomerLocationId(customerLocationId)
//                .setAccessToken(accessToken)
//                .build(), context);
//
//        Mockito.verify(context).emit(added);
//        customerLocation.customerLocationAdded(added); // Simulate event callback to drive state change.
//
//        return customerLocation;
//    }
//
//    private Device defaultDevice(String deviceId) {
//        return Device.newBuilder()
//                .setDeviceId(deviceId)
//                .setCustomerLocationId(customerLocationId)
//                .setActivated(true).setRoom("")
//                .setNightlightOn(false)
//                .build();
//    }
//}
