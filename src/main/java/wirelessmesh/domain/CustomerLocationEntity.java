 package wirelessmesh.domain;

 import com.google.protobuf.Empty;
 import io.cloudstate.javasupport.EntityId;
 import io.cloudstate.javasupport.eventsourced.*;
 import io.cloudstate.springboot.starter.CloudstateContext;
 import io.cloudstate.springboot.starter.CloudstateEntityBean;

 import org.springframework.beans.factory.annotation.Autowired;
 import wirelessmesh.DeviceService;
 import wirelessmesh.PubsubService;
 import wirelessmesh.Wirelessmesh.*;
 import domain.Domain.*;

 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Optional;
 import java.util.stream.Collectors;

 /**
  * A customer location entity.
  *
  * As an entity, I will be seeded with my current state upon loading and thereafter will completely serve the
  * backend needs for a particular device. There is no practical limit to how many of these entities you can have,
  * across the application cluster. Look at each instance of this entity as being roughly equivalent to a row in a
  * database, only each one is completely addressable and in memory.
  *
  * Event sourcing was selected in order to have complete traceability into the behavior of devices for the purposes
  * of security, analytics and simulation.
  */
 @CloudstateEntityBean
 @EventSourcedEntity
 public class CustomerLocationEntity {

     @CloudstateContext
     private EventSourcedContext context;

     private final DeviceService deviceService;

     private final PubsubService pubsubService;

     /**
      * This section contains the private state variables necessary for this entity.
      */

     @EntityId
     private String customerLocationId;

     private boolean added = false;

     private boolean removed = false;

     private String accessToken = "";

     private List<Device> devices = new ArrayList<Device>();

     /**
      * Constructor with service autowiring.
      * @param deviceService DeviceService to communicate with physical devices
      * @param pubsubService PubsubService to publish events to external
      */
     @Autowired
     public CustomerLocationEntity(DeviceService deviceService, PubsubService pubsubService) {
         this.deviceService = deviceService;
         this.pubsubService = pubsubService;
     }

     /**
      * This is the command handler for adding a customer location as defined in protobuf.
      * @param addCustomerLocationCommand the command message from protobuf
      * @param ctx the application context
      * @return Empty (unused)
      */
     @CommandHandler
     public Empty addCustomerLocation(AddCustomerLocationCommand addCustomerLocationCommand, CommandContext ctx) {
         if (added) {
             ctx.fail("Customer location already added");
         }

         CustomerLocationAdded event = CustomerLocationAdded.newBuilder()
                 .setCustomerLocationId(addCustomerLocationCommand.getCustomerLocationId())
                 .setAccessToken(addCustomerLocationCommand.getAccessToken())
                 .build();

         ctx.emit(event);
         pubsubService.publish(event.toByteString());
         return Empty.getDefaultInstance();
     }

     /**
      * This is the event handler for adding a customer location. It is here we update current state due to
      * successful storage to the eventlog.
      *
      * @param customerLocationAdded the event previously emitted in the command handler, now safely stored.
      */
     @EventHandler
     public void customerLocationAdded(CustomerLocationAdded customerLocationAdded) {
         this.added = true;
         this.removed = false;
         this.accessToken = customerLocationAdded.getAccessToken();
     }

     /**
      * This is the command handler for removing a customer location as defined in protobuf.
      * @param removeCustomerLocationCommand the command message from protobuf
      * @param ctx the application context
      * @return Empty (unused)
      */
     @CommandHandler
     public Empty removeCustomerLocation(RemoveCustomerLocationCommand removeCustomerLocationCommand, CommandContext ctx) {
         if (!added) {
             ctx.fail("Customer location does not exist");
         }
         if (removed) {
             ctx.fail("Customer location already removed");
         }

         CustomerLocationRemoved event = CustomerLocationRemoved.newBuilder()
                 .setCustomerLocationId(removeCustomerLocationCommand.getCustomerLocationId())
                 .build();

         ctx.emit(event);
         pubsubService.publish(event.toByteString());
         return Empty.getDefaultInstance();
     }

     /**
      * This is the event handler for removing a customer location. It is here we update current state due to
      * successful storage to the eventlog.
      *
      * @param customerLocationRemoved the event previously emitted in the command handler, now safely stored.
      */
     @EventHandler
     public void customerLocationRemoved(CustomerLocationRemoved customerLocationRemoved) {
         this.removed = true;
         this.added = false;
         devices = new ArrayList<Device>();
     }

     /**
      * This is the command handler for activating a wirelessmesh device as defined in protobuf.
      * @param activateDeviceCommand the command message from protobuf
      * @param ctx the application context
      * @return Empty (unused)
      */
     @CommandHandler
     public Empty activateDevice(ActivateDeviceCommand activateDeviceCommand, CommandContext ctx) {
         if (removed) {
             ctx.fail("customerLocation does not exist.");
         }

         if (findDevice(activateDeviceCommand.getDeviceId()).isPresent()) {
             ctx.fail("Device already activated");
         }

         DeviceActivated event = DeviceActivated.newBuilder()
                 .setDeviceId(activateDeviceCommand.getDeviceId())
                 .setCustomerLocationId(customerLocationId)
                 .build();

         ctx.emit(event);
         pubsubService.publish(event.toByteString());
         return Empty.getDefaultInstance();
     }

     /**
      * This is the event handler for activating a wirelessmesh device. It is here we update current state due to
      * successful storage to the eventlog.
      *
      * @param deviceActivated the event previously emitted in the command handler, now safely stored.
      */
     @EventHandler
     public void deviceActivated(DeviceActivated deviceActivated) {
         devices.add(Device.newBuilder()
                 .setDeviceId(deviceActivated.getDeviceId())
                 .setCustomerLocationId(customerLocationId)
                 .setActivated(true)
                 .setNightlightOn(false)
                 .build());
     }

     /**
      * This is the command handler for removing a wirelessmesh device as defined in protobuf.
      * @param removeDeviceCommand the command message from protobuf
      * @param ctx the application context
      * @return Empty (unused)
      */
     @CommandHandler
     public Empty removeDevice(RemoveDeviceCommand removeDeviceCommand, CommandContext ctx) {
         if (!added || removed) {
             ctx.fail("customerLocation does not exist.");
         }

         if (!findDevice(removeDeviceCommand.getDeviceId()).isPresent()) {
             ctx.fail("Device does not exist");
         }

         DeviceRemoved event = DeviceRemoved.newBuilder()
                 .setDeviceId(removeDeviceCommand.getDeviceId())
                 .setCustomerLocationId(customerLocationId).build();

         ctx.emit(event);
         pubsubService.publish(event.toByteString());
         return Empty.getDefaultInstance();
     }

     /**
      * This is the event handler for removing a wirelessmesh device. It is here we update current state due to
      * successful storage to the eventlog.
      *
      * @param deviceRemoved the event previously emitted in the command handler, now safely stored.
      */
     @EventHandler
     public void deviceRemoved(DeviceRemoved deviceRemoved) {
         devices = devices.stream().filter(d -> !d.getDeviceId().equals(deviceRemoved.getDeviceId()))
                 .collect(Collectors.toList());
     }

     /**
      * This is the command handler for assigning a wirelessmesh device to a room as defined in protobuf.
      * @param assignRoomCommand the command message from protobuf
      * @param ctx the application context
      * @return Empty (unused)
      */
     @CommandHandler
     public Empty assignRoom(AssignRoomCommand assignRoomCommand, CommandContext ctx) {
         if (removed) {
             ctx.fail("customerLocation does not exist.");
         }

         if (!findDevice(assignRoomCommand.getDeviceId()).isPresent()) {
             ctx.fail("Device does not exist");
         }

         RoomAssigned event = RoomAssigned.newBuilder()
                 .setDeviceId(assignRoomCommand.getDeviceId())
                 .setCustomerLocationId(customerLocationId)
                 .setRoom(assignRoomCommand.getRoom()).build();

         ctx.emit(event);
         pubsubService.publish(event.toByteString());
         return Empty.getDefaultInstance();
     }

     /**
      * This is the event handler for assigning a wirelessmesh device to a room. It is here we update current state due to
      * successful storage to the eventlog.
      *
      * @param roomAssigned the event previously emitted in the command handler, now safely stored.
      */
     @EventHandler
     public void roomAssigned(RoomAssigned roomAssigned) {
         Device old = findDevice(roomAssigned.getDeviceId()).get();
         Device device = old.toBuilder()
                 .setRoom(roomAssigned.getRoom())
                 .build();

         replaceDevice(device);
     }

     /**
      * This is the command handler for toggling the wirelessmesh device nightlight as defined in protobuf.
      * @param toggleNightlightCommand the command message from protobuf
      * @param ctx the application context
      * @return Empty (unused)
      */
     @CommandHandler
     public Empty toggleNightlight(ToggleNightlightCommand toggleNightlightCommand, CommandContext ctx) throws IOException {
         if (removed) {
             ctx.fail("customerLocation does not exist.");
         }

         Optional<Device> deviceMaybe = findDevice(toggleNightlightCommand.getDeviceId());

         if (!deviceMaybe.isPresent()) {
             ctx.fail("Device does not exist");
         }

         NightlightToggled event = NightlightToggled.newBuilder()
                 .setDeviceId(toggleNightlightCommand.getDeviceId())
                 .setCustomerLocationId(customerLocationId)
                 .setNightlightOn(!deviceMaybe.get().getNightlightOn()).build();

         ctx.emit(event);
         deviceService.toggleNightlight(accessToken, toggleNightlightCommand.getDeviceId());
         pubsubService.publish(event.toByteString());
         return Empty.getDefaultInstance();
     }

     /**
      * This is the event handler for toggling the wirelessmesh device nightlight. It is here we update current state due to
      * successful storage to the eventlog.
      *
      * @param nightlightToggled the event previously emitted in the command handler, now safely stored.
      */
     @EventHandler
     public void nightlightToggled(NightlightToggled nightlightToggled) {
         Device old = findDevice(nightlightToggled.getDeviceId()).get();
         Device device = old.toBuilder()
                 .setNightlightOn(nightlightToggled.getNightlightOn())
                 .build();

         replaceDevice(device);
     }

     /**
      * This is the command handler geting the current state of the devices as defined in protobuf.
      * @param GetCustomerLocationCommand the command message from protobuf
      * @param ctx the application context
      * @return Empty (unused)
      */
     @CommandHandler
     public CustomerLocation getCustomerLocation(GetCustomerLocationCommand getCustomerLocationCommand, CommandContext ctx) {
         if (removed || !added) {
             ctx.fail("customerLocation does not exist.");
         }

         return CustomerLocation.newBuilder().setCustomerLocationId(customerLocationId)
                 .setAccessToken(accessToken)
                 .setAdded(added)
                 .setRemoved(removed)
                 .addAllDevices(devices).build();
     }

     /**
      * Helper function to find a device in the device collection.
      */
     private Optional<Device> findDevice(String deviceId) {
         List<Device> filtered = devices.stream()
                 .filter(d -> d.getDeviceId().equals(deviceId))
                 .collect(Collectors.toList());

         if (filtered.size() == 0) {
             return Optional.empty();
         }
         else {
             return Optional.of(filtered.get(0));
         }
     }

     /**
      * Helper function to replace the state of a given device within the device collection.
      */
     private void replaceDevice(Device device) {
         List<Device> filtered = devices.stream()
                 .filter(d -> !d.getDeviceId().equals(device.getDeviceId()))
                 .collect(Collectors.toList());

         filtered.add(device);
         devices = filtered;
     }
 }