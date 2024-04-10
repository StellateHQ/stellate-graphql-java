# How to develop this plugin

A prerequisite for any of the actions described here is to have Maven installed. On a Mac this can be done easily using `brew`:

```sh
brew install maven

# Check that mvn has been installed successfully
mvn --version
```

## Local development

To get started, first make sure to package this library into a `jar` file by running:

```sh
mvn clean package
```

_Note: Using `clean` is not strictly required but can be considered best practice as it removes any existing build artifacts before generating new ones._

To set up a local GraphQL Java API, you can follow the [official GraphQL Java Tutorial](https://www.graphql-java.com/tutorials/getting-started-with-spring-boot/).

Then set up the `StellateIntrumentation` like described in [README.md]. In this case it makes most sense to add the `GraphQlConfig` class to the main file, which is `BookDetailsApplication.java`.

Finally, add your local `jar` package as dependency like this in the `build.gradle` file (make sure to replace the path and version number):

```
dependencies {
  implementation 'org.springframework.boot:spring-boot-starter-graphql'
  implementation 'org.springframework.boot:spring-boot-starter-web'
  implementation files('/path/to/your/stellate-graphql-java/target/stellate-x.y.z.jar')
  ...other stuff...
}
```

Now when you boot the server by running `./gradlew bootRun` your project should compile successfully. When you send a request you should see it being reported to your production Stellate service.

_Note: The instrumentation always uses production Stellate URL for logging, so unless you change that in the library code you need to set up a service on production for testing._

## Publishing

We publish this library on the [Maven Central Repository](https://mvnrepository.com/repos/central). We use [Sonatype](https://central.sonatype.com) to distribute the library on the repository. You can find the login to the Sonatype platform in our shared 1password.

Before you can publish from your local machine there are two things you need to to.

First, you need to configure your Maven installation so that it has the credentials in order to publish to our Sonatype account. Maven settings are usually located inside a folder `~/.m2` in your home directory. Open or create a `settings.xml` file in that directory:

```sh
touch ~/.m2/settings.xml
```

Open the file and add the following contents (username and password can be found in our shared 1password as well as "User Token Username" and "User Token Password" on the Sonatype login.)

```xml
<settings>
  <servers>
    <server>
      <id>stellate</id>
      <username>...</username>
      <password>...</password>
    </server>
  </servers>
</settings>
```

Second, you need to import a pair of pgp keys that will be used to sign any published files. You can find both the public and private key as `pgp` files in our shared 1password. Download both files, then run:

```sh
gpg --import public.pgp
gpg --import private.pgp
```

_Note: If you haven't already, you can easily install `gpg` on a Mac with `brew` by running `brew install gnupg`._

Now you should be set up to publish the library. If needed, bump the version in `pom.xml`. Then run the following to package the code and push it to Sonatype:

```sh
mvn clean deploy
```

Once this is completed, head over to [Sonatype](https://central.sonatype.com/publishing/deployments). You should see a list of deployments, with the latest one you just triggered on top. All that is left to do is to press the "Publish" button and wait for the library to be pushed into the Central Repository. (We experienced that this can take some time, potentially even multiple hours or even days.)
