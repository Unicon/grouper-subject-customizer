Subject Customizer
==================

### Prevent Protected Users Details from Leaking
Admins wants to ensure that students information is not released to individuals that should not be able to determine said information. In other cases, individuals
 should be restricted from the search result altogether.

This is done with two strategies:

1. Subjects are redacted so that if the searcher is not in a privileged group and the user is in a protected group, their attributes are redacted.
1. Subject are filtered out if the searcher is not in a privileged group and the user is in a protected group.

Expressions can also be used to flag the subject as protected.

## Requirements
This subject customizer requires the following resources:

* A configured Grouper UI and/or Web Services instance. (It generally does not make sense to run under the daemon/loader.)

## Build
To build the jar, run:

```
gradle clean jar
```

## Installation
Installing the library:

1. Build the jar (see above).
1. Copy the jar file to `WEB-INF/lib`.
1. Set the grouper.properties (in `WEB-INF/classes`).

## Execution
(There is nothing to directly execute.)

### Grouper Settings
The `GROUPER_HOME/conf/grouper.properties` file is used to specify the hooks settings.

|Property Name|Default Value|Notes|
|-------------|-------------|-----|
|subjects.customizer.className|(none)|edu.nd.middleware.grouper.SubjectCustomizer||
|subjectCustomizer.filterSubjectOut.ifSubjectInGroup.{0}|(optional if `subjectCustomizer.filterSubjectOut.ifSubjectExpressionIsTrue` is specified)|A fully qualified group path (e.g. app:SubjectSecurity:groups:protectedGroups) to a group containing protected subjects that should be removed from the search results.|
|subjectCustomizer.filterSubjectOut.ifSubjectExpressionIsTrue.{0}|(optional if `subjectCustomizer.filterSubjectOut.ifSubjectInGroup` is specified)|An expression that if returns true, filters out the subject from the results. (See below)|
|subjectCustomizer.filterSubjectOut.unlessSearcherIsInGroup.{0}|(required, if one or both of the preceding properties are set)|A fully qualified group path (e.g. app:SubjectSecurity:groups:privilegedGroups) to a grouper of privileged users that will retain the protected subject.|
|subjectCustomizer.redactSubjectAttributes.ifSubjectInGroup.{0}|(optional if `subjectCustomizer.redactSubjectAttributes.ifSubjectExpressionIsTrue` is specified)|A fully qualified group path (e.g. app:SubjectSecurity:groups:protectedGroups) to a group containing protected subjects that should be redacted.|
|subjectCustomizer.redactSubjectAttributes.ifSubjectExpressionIsTrue.{0}|(optional if `subjectCustomizer.redactSubjectAttributes.ifSubjectInGroup` is specified)|An expression that if returns true, redacts the subject from the results. (See below)|
|subjectCustomizer.redactSubjectAttributes.unlessSearcherIsInGroup.{0}|(required, if one or both of the preceding properties are set)|A fully qualified group path (e.g. app:SubjectSecurity:groups:privilegedGroups) to a grouper of privileged users that will not have redacted protected subjects.|
|subjectCustomizer.uidField|uid|The attribute to use as a replacement name if the results are restricted.|
|subjectCustomizer.restrictedAttributeName|cn|The attribute name to add to the subject that displays the restricted value.|
|subjectCustomizer.restrictedAttributeValue|"(restricted)"|The attribute value to set the `subjectCustomizer.restrictedAttributeName` to.|

`ifSubjectExpressionIsTrue` is an expression where the subject object is passed in. The resulting response (true or false) indicates whether the action is taken. Some examples include:

- `${subject.name.endsWith("Gasper")}`
- `${subject.getAttributeValue("ou").endsWidth("ou=alumni,dc=school,dc=edu")}`

### Logging
The logging properties follow standard log4j settings.

> If the logging properties are not setup then no output will be returned from the program.

The following example can be appended to the `conf/log4j.properties` and will output to the console
(previously defined in a baseline grouper log4j.properties) and to a static file (`logs/customHooks.log`).

```

## Logger
## Subject Customizer
log4j.logger.net.unicon.grouper                      = DEBUG, grouper_stdout
```

## Local Development
This project has been supplemented with Docker. Docker's usage allows for quickly deploying the deployed artifact to a
consistent, repeatable, local Grouper environment, which facilitates consistent testing. Docker (or Docker for Windows and OS X) should be locally installed.

Running `./gradlew clean; ./gradlew up` will compile the jar, build the on top of the `unicon/grouper-demo` image (this could take 10-20 minutes
 the first time depending upon the bandwidth speed), and start an image. `docker ps` will display info about the running container. Running
 `docker exec -it grouper-dev bash` will allow one to connect into the running image. The image can be connected to from a browser
 by going to the port listed in the `docker ps` 8080 mapping (probably 8080). The customHooks.log can be dumped with
 `docker exec -t grouper-dev cat /logs/xxxxx.log`.

When testing is complete, `exit` to leave the running container. Then run `./gradlew clean` to clean
  the environment. Now you are ready to make the necessary code changes and start over again.

The following test work against this container:

1. Login as `banderson`:
    1. Go to `app:test`.
    1. Search for `jgasper`, `alewis`, `Langenberg`, `gasper`. They should all be there and fully populated.
    1. Add jsmith as a group admin.
1. Login as `jsmith`:
    1. Go to `app:test`.
    1. Search for `jgasper` (should not be resolvable), `alewis` (no populated name), `Langenberg` (none should be found), `gasper` (none should have names populated)
