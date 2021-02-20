// package wirelessmesh;

// import io.cloudstate.javasupport.eventsourced.CommandContext;
// import org.testng.Assert;
// import org.testng.annotations.*;
// import org.mockito.*;
// import wirelessmesh.domain.DeviceClient;
// import wirelessmesh.domain.DeviceEntity;
// import domain.Devicedomain.*;
// import service.Wirelessmeshservice.*;

// import java.io.IOException;

// public class DeviceEntityTest {

//     String deviceId = "deviceId1";
//     String customerId = "customerId1";
//     String room = "man-cave";
//     DeviceEntity entity;
//     CommandContext context = Mockito.mock(CommandContext.class);

//     @Test
//     public void activateDeviceTest() {
//         // Instantiate entity and send activation command.
//         entity = new DeviceEntity(deviceId);
//         DeviceActivated activated = DeviceActivated.newBuilder().setDeviceId(deviceId).setCustomerId(customerId).build();
//         entity.activateDevice(ActivateDeviceCommand.newBuilder().setDeviceId(deviceId).setCustomerId(customerId).build(), context);
//         Mockito.verify(context).emit(activated);

//         // Simulate event callback.
//         entity.deviceActivated(activated);

//         // Test get device.
//         GetDeviceCommand command = GetDeviceCommand.newBuilder().setDeviceId(deviceId).build();
//         Device device = entity.getDevice(command, context);
//         Assert.assertEquals(device.getDeviceId(), deviceId);
//         Assert.assertEquals(device.getActivated(), true);
//         Assert.assertEquals(device.getCustomerId(), customerId);
//         Assert.assertEquals(device.getRoom(), "");
//     }

//     @Test
//     public void assignRoomTest() {
//         RoomAssigned assigned = RoomAssigned.newBuilder().setDeviceId(deviceId).setRoom(room).build();
//         entity.assignRoom(AssignRoomCommand.newBuilder().setDeviceId(deviceId).setRoom(room).build(), context);
//         Mockito.verify(context).emit(assigned);

//         // Simulate event callback.
//         entity.roomAssigned(assigned);

//         GetDeviceCommand command = GetDeviceCommand.newBuilder().setDeviceId(deviceId).build();
//         Device device = entity.getDevice(command, context);
//         Assert.assertEquals(device.getDeviceId(), deviceId);
//         Assert.assertEquals(device.getActivated(), true);
//         Assert.assertEquals(device.getCustomerId(), customerId);
//         Assert.assertEquals(device.getRoom(), room);
//     }

//     @Test
//     public void toggleNightlightTest() throws IOException {
//         NightlightToggled toggled = NightlightToggled.newBuilder().setDeviceId(deviceId).setNightlightOn(true).build();
//         entity.toggleNightlight(ToggleNightlightCommand.newBuilder().setDeviceId(deviceId).build(), context);
//         Mockito.verify(context).emit(toggled);

//         // Simulate event callback.
//         entity.nightlightToggled(toggled);

//         GetDeviceCommand command = GetDeviceCommand.newBuilder().setDeviceId(deviceId).build();
//         Device device = entity.getDevice(command, context);
//         Assert.assertEquals(device.getDeviceId(), deviceId);
//         Assert.assertEquals(device.getActivated(), true);
//         Assert.assertEquals(device.getCustomerId(), customerId);
//         Assert.assertEquals(device.getRoom(), room);
//         Assert.assertEquals(device.getNightlightOn(), true);
//     }

//     @Test
//     public void togglePhysicalNightlight() throws IOException {
//         DeviceClient client = new DeviceClient();
//         client.toggleNightlight("d073d55b9b42");
//     }
// }
