# Akka Serverless - Wireless Mesh Example App

A Spring-based example app for [Akka Serverless](https://developer.lightbend.com/docs/akka-serverless/)

Features include:

* Customer locations with wireless mesh devices
* Connectivity to Google Cloud Pubsub

## What is this example?

To help you get started with Akka Serverless, we've built some example apps that showcase the capabilities of the platform. This example application mimics a company that uses Akka Serverless to keep track of the wireless mesh devices their customers have installed and the devices connected to the meshes.

In this example app you can interact with the devices, assign them to different rooms in the house, and turn them on or off. To make this example even more interactive, you can add an actual nightlight and switch the lights on or off.

## Prerequisites

To build and deploy this example application, you'll need to have:

* An [Akka Serverless account](https://docs.cloudstate.com/getting-started/lightbend-account.html)
* Java 11 or higher installed
* Maven 3.x or higher installed
* The Docker CLI installed
* A [service account](https://cloud.google.com/docs/authentication/production) that can connect to Google Cloud Pubsub

## Build, Deploy, and Test

### LIFX integration for toggling nightlight

If you have an LIFX bulb and would like it to stand in for a wirelessmesh device and have it light on/off when you toggle the device nightlight, you simply have to:
* Have an operational bulb
* When you create your customer location, be sure to set the access token to the authorizaton token you requested with LIFX.
* When you activate the device in this app, make sure it has the same device id as your bulb.
* More information [here][https://api.developer.lifx.com]

### Prepare your Google Cloud Pubsub

To connect to Google Cloud Pubsub, the easiest method is authenticate using a service account. To create your [service account](https://cloud.google.com/docs/authentication/production#cloud-console). After creating your service account, you need to download the service account key as a JSON file called `mycreds.json`.

To publish events to google pubsub locally, use the 'export PUBLISH_EVENTS='ON' environment variable. Make sure to add this environment variable when you deploy to akka serverless if you want to publish to google. You will also need to set the environment variable GOOGLE_PROJECT_ID and set that to your project id. You will need to create a the topic '"'wirelessmesh'

examples to set local variables for testing (mac os):
* export GOOGLE_APPLICATION_CREDENTIALS='/Users/memyselfandI/Downloads/mycreds.json'
* export GOOGLE_PROJECT_ID='diesel-broccoli-266021'
* export PUBLISH_EVENTS='ON'

Be sure to set GOOGLE_PROJECT_ID and PUBLISH_EVENTS environment variables during your akkaserverless deploy as well.

Next, you'll need to build a base image that contains the `mycreds.json` file and sets the environment variable `GOOGLE_APPLICATION_CREDENTIALS` to the service account key. You can build the docker image with by running:

```bash
docker build -f prereq.Dockerfile . -t mybaseimage
```

_the new container will be named `mybaseimage` and will be used in subsequent steps. Note that pushing containers with service account keys to a public docker registry is a potentially dangerous situation._

### Build your container

To build your own container, follow the below steps

1. If you haven't completed the previous step, because you don't need to connect to Google Cloud Pubsub, change `mybaseimage` on [line 58](https://github.com/lightbend-labs/akkaserverless-wirelessmesh-java/blob/main/pom.xml#L58) to `adoptopenjdk/openjdk8`
1. Update [line 56](https://github.com/lightbend-labs/akkaserverless-wirelessmesh-java/blob/main/pom.xml#L56) of the `pom.xml` file with your Docker Hub username.
1. Run `mvn clean install`

The command `mvn clean install` will create a new Docker image based on `adoptopenjdk/openjdk8`.

The result of the command should be

```bash
$ mvn clean install

...

[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:31 min
[INFO] Finished at: 2021-01-20T16:20:29-08:00
[INFO] ------------------------------------------------------------------------
```

### Deploy your container

To deploy the container as a service in Akka Serverless, you'll need to:

1. Push the container to a container registry: `docker push -t <registry url>/<registry username>/akkaserverless-wirelessmesh-java:latest`
1. Deploy the service in Akka Serverless: `akkasls svc deploy wirelessmesh <registry url>/<registry username>/akkaserverless-wirelessmesh-java:latest`

_The above command will deploy your container to your default project with the name `wirelessmesh`. If you want to have a different name, you can change that._

### Testing your service

To test using Postman.
* First install Postman, [found here](https://www.postman.com)
* Assuming you have deployed to akkaserverless and exposed your service to 'winter-mountain-2372.us-east1.apps.akkaserverless.com'...
* Create a Postman POST request to 'https://winter-mountain-2372.us-east1.apps.akkaserverless.com/wirelessmesh/add-customer-location' with the json body '{"customerLocationId": "my-first-location", "accessToken": "my lifx access token if applicable"}'
* You should see a response of '200(OK) {}', this will be the response of any POST
* You can now create a GET request to 'https://winter-mountain-2372.us-east1.apps.akkaserverless.com/wirelessmesh/get-customer-location?customerLocationId=my-first-location'
* You should see a json response containing your customer location and no devices.
* Create a POST request to 'https://winter-mountain-2372.us-east1.apps.akkaserverless.com/wirelessmesh/activate-device' with the body '{"customerLocationId": "my-first-location", "deviceId": "my-first-device"}'
* Create a POST requset to 'https://winter-mountain-2372.us-east1.apps.akkaserverless.com/wirelessmesh/assign-room' with the body '{"customerLocationId": "my-first-location", "deviceId": "my-first-device", "room": "office"}'
* Create a POST requset to 'https://winter-mountain-2372.us-east1.apps.akkaserverless.com/wirelessmesh/toggle-nightlight' with the body '{"customerLocationId": "my-first-location", "deviceId": "my-first-device"}'
* Rerun your get-customer-location request
* You should see a json response with your customer location and a collection of your single device with the room assigned and the nightlight on
* Create a POST request to 'https://winter-mountain-2372.us-east1.apps.akkaserverless.com/wirelessmesh/remove-device' with the body '{"customerLocationId": "my-first-location", "deviceId": "my-first-device"}'
* You should see a json response no longer containing any devices
* Create a POST request to 'https://winter-mountain-2372.us-east1.apps.akkaserverless.com/wirelessmesh/remove-customer-location' with the body '{"customerLocationId": "my-first-location"}'
* Rerun your get-customer-location request and you will see a server error since it no longer exists.

## Contributing

We welcome all contributions! [Pull requests](https://github.com/lightbend-labs/akkaserverless-wirelessmesh-java/pulls) are the preferred way to share your contributions. For major changes, please open [an issue](https://github.com/lightbend-labs/akkaserverless-wirelessmesh-java/issues) first to discuss what you would like to change.

## Support

This project is provided on an as-is basis and is not covered by the Lightbend Support policy.

## License

See the [LICENSE](./LICENSE).
