# Commissioning Scripts

WebCTRL is a trademark of Automated Logic Corporation. Any other trademarks mentioned herein are the property of their respective owners.

- [Commissioning Scripts](#commissioning-scripts)
  - [About](#about)
  - [Basic Usage](#basic-usage)
  - [Scheduled Tests](#scheduled-tests)
  - [Script Development](#script-development)
    - [Displaying Results](#displaying-results)
    - [Janino Limitations](#janino-limitations)

## About

This add-on allows WebCTRL server administrators to upload and execute scripts, which are written in the Java programming language. Scripts may be compiled and packaged into *.jar* archives (recommended), or they may be interpreted at runtime using [Janino 3.1.7](http://janino-compiler.github.io/janino/) (easy for small scripts, but note that Janino has some [limitations](#janino-limitations)). The add-on does not come prepackaged with any scripts; however, sample scripts may be found within [*./samples*](./samples). Also see the [terminal unit commissioning script](https://github.com/automatic-controls/terminal-unit-script).

The original purpose of this add-on was to be a customizable alternative to the ACxelerate commissioning tool provided by Automated Logic. Semantic tagging is one concept borrowed from ACxelerate. Before execution, each script is paired with a mapping that associates control program microblock nodes to tag names referenced by the script. Primary script procedures are executed once on each control program specified by a mapping.

The use-case considered while developing this add-on was VAV box commissioning. One such VAV box test locks the damper to various positions and records the resulting airflow. If all VAV dampers are closed at once, the high static pressure safety may trip on the RTU serving the VAV boxes. Thus, it is necessary to limit how many control programs are tested concurrently under a given RTU.

Within each mapping, control programs are separated into groups. For VAV commissioning, there should be one group for each air source (e.g, RTU). Before script execution, a parameter is specified for the maximum percentage of tests which can be ran concurrently within each group. Another parameter specifies a maximum number of tests which can be ran concurrently across all groups (technically, the number of allotted threads).

Another possible use for this add-on is advanced reporting with automatic data collection. Since each script has the full expressive power of the Java programming language, there is much more flexibility for data presentation and analysis as compared to existing WebCTRL reporting tools.

## Basic Usage

1. If signed add-ons are required, copy the authenticating certificate [*ACES.cer*](https://github.com/automatic-controls/addon-dev-script/blob/main/ACES.cer?raw=true) to the *./addons* directory of your *WebCTRL* installation folder.
2. Install [*CommissioningScripts.addon*](https://github.com/automatic-controls/commissioning-scripts/releases/latest/download/CommissioningScripts.addon) using the *WebCTRL* interface.
3. Navigate to the add-on's main page.
4. Click the *Upload Script* button and select your script. An error will be thrown if any compilation/loading errors occur. For debugging purposes, the stack trace of any error is recorded in the *Error Log*.
5. Click *Semantic Mappings* and then *New Mapping* in order to select which parts of the WebCTRL system are accessible to the script.
6. Click *Add Group* and then *Add Equipment* to select which control programs should be tested.
7. Enter in a few semantic tags and mapping expressions to be referenced by the script. The *Examine* button on each control program can help to determine the expression to use for mapping a given semantic tag. Note that each tag mapping is actually a [regular expression](https://docs.oracle.com/en/java/javase/18/docs/api/java.base/java/util/regex/Pattern.html).
8. Press the *Save Changes* button as the last step to create your mapping. Note that all groups and mappings should be named.
9.  The *Preview Mapping Resolution* button should be used to determine whether each tag mapping is resolved correctly.
10. Navigate back to the *Scripts* page.
11. Select your newly created mapping, and specify the *Max. Active Tests Per Group* and *Total Max. Active Tests* parameters appropriately. Also toggle any desired script-specific options in the parameters column of the script table.
12. Press *Start* to initiate the corresponding script. You will be redirected to an output page upon successful initiation.
13. When completed, the final script output is recorded in the *Archived Tests* section.

## Scheduled Tests

[Cron expressions](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/support/CronExpression.html#parse-java.lang.String-) can be used to schedule scripts to execute at regular intervals. Scheduled script output can be automatically emailed to a set of recipients if necessary. This add-on relies upon the email server configured in general WebCTRL settings: *System Settings / General / Email Server Configuration*.

To prevent inadvertent schedule modification, each schedule stores a hash of the mapping and script it references. When the mapping or script is modified, hash validation for the schedule will fail, and it will be effectively disabled until someone intervenes. Hashes are recomputed each time an operator saves a schedule.

## Script Development

If a single *.java* file is uploaded as a script, then it will be interpreted using [Janino 3.1.7](http://janino-compiler.github.io/janino/) at runtime. **You do not have to configure a development environment for compiling Janino scripts.** In principle, you could develop scripts using only a text editor like *Notepad*. Another advantage of this approach is that you can easily download, edit, and re-upload script source code from any computer with a connection to the WebCTRL server.

However, it is recommended that scripts are compiled and packaged into *.jar* archives. Pre-compiled scripts do not suffer from Janino's limitations. Essentially, just throw all your *.class* files into a *.jar* archive. The only special requirement is that a manifest file exist in the *.jar*, and that the `Main-Class` header specifies which class to use as the primary script (this class should extend `aces.webctrl.scripts.commissioning.core.Script`).

Be mindful to compile your script with a version of Java compatible with the target WebCTRL version of your server. For instance, you should use the build flag `--release 8` if you intend the script to run on WebCTRL7.0. See https://jdk.java.net/ to download the latest version of the java development kit.

To ease the development process, it is suggested to use an IDE (e.g, [Visual Studio Code](https://code.visualstudio.com/)), and then add the following dependencies: [CommissioningScripts.jar](https://github.com/automatic-controls/commissioning-scripts/releases/latest/download/CommissioningScripts.jar) and [CommissioningScripts-sources.jar](https://github.com/automatic-controls/commissioning-scripts/releases/latest/download/CommissioningScripts-sources.jar). These dependencies will provide you with intellisense and Javadocs for script development. You are also welcome to reference any other dependencies provided by WebCTRL at runtime (e.g, the add-on API, or `javax.servlet`).

This add-on does not enforce any security restrictions on what scripts can do. Scripts are executed with the same privilege set as the add-on. As such, you must be careful to **use scripts at your own risk.** Note that only WebCTRL server administrators are allowed to manage scripts.

### Displaying Results

Scripts dynamically generate an output HTML page to show data collected during execution. The output HTML page must be self-contained to a single document. For example, there is currently no support for creating and referencing an external JavaScript file within a *.jar* script. However, you can reference existing external resources (e.g, the CSS file used for this add-on: `<link rel="stylesheet" type="text/css" href="main.css"/>`). For automated data export, you can schedule scripts to regularly execute and email the results in the form of an HTML or CSV document. When emailing a HTML document, all resources should be self-contained (i.e, not even the link to this add-on's CSS file will work).

AJAX requests can be sent from the output HTML back to active scripts. AJAX requests should be submitted to `window.location.href` with an extra request parameter: `AJAX`. Note that AJAX requests will fail whenever the script completes and becomes inactive. In such case (HTTP response status of `404`), you should use `window.location.reload()` to retrieve the final archived output of the script.

Despite not being able to create links to static resources within *.jar* scripts that work out-of-the-box, it is possible to access them nonetheless. From within the script AJAX handler method, you could try returning data from something like `SampleScript.class.getClassLoader().getResourceAsStream("resources/static_data.txt")`.

### Janino Limitations

The official list of limitations may be found [here](http://janino-compiler.github.io/janino/#limitations). The most annoying limitation I've come across is a lack of support for generic types. Pretty much all generic type parameters should be treated as `Object`, and you'll have to cast generic objects back to their intended type when you reference them.

Janino doesn't like having unitialized variables. Janino's compiler seems unable to correctly determine if a variable is initialized. As a result, it will throw cryptic errors at odd times. I suggest initializing variables as soon as you declare them (e.g, use `null` for objects). I've created two issues ([172](https://github.com/janino-compiler/janino/issues/172) and [178](https://github.com/janino-compiler/janino/issues/178)) in the [Janino repository](https://github.com/janino-compiler/janino) regarding this problem.

See [177](https://github.com/janino-compiler/janino/issues/177) for another issue I came across. Feel free to report other errors you uncover at the [Janino repository](https://github.com/janino-compiler/janino/issues).