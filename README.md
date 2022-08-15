# Commissioning Scripts

WebCTRL is a trademark of Automated Logic Corporation. Any other trademarks mentioned herein are the property of their respective owners.

- [Commissioning Scripts](#commissioning-scripts)
  - [About](#about)
  - [Basic Usage](#basic-usage)
  - [Scheduled Tests](#scheduled-tests)
  - [Script Development](#script-development)
    - [Janino Limitations](#janino-limitations)

## About

This add-on allows WebCTRL server administrators to upload and execute scripts, which are written using the Java programming language. Scripts are compiled using [Janino 3.1.7](http://janino-compiler.github.io/janino/), so a few [limitations](#janino-limitations) apply. The add-on does not come prepackaged with any scripts; however, a few examples may be found within [*./samples*](./samples).

The original purpose of this add-on was to be a customizable alternative to the ACxelerate commissioning tool provided by Automated Logic. Semantic tagging is one concept borrowed from ACxelerate. Before execution, each script is paired with a mapping that associates control program microblock nodes to tag names referenced by the script. Primary script procedures are executed once on each control program specified by a mapping.

The use-case considered while developing this add-on was VAV box commissioning. One VAV box test locks the damper to various positions and records the resulting airflow. If all VAV dampers are closed at once, the high static pressure safety may trip on the RTU serving the VAV boxes. Thus, it is necessary to limit how many control programs are tested concurrently under a given RTU.

Within each mapping, control programs are separated into groups. For VAV commissioning, there should be one group for each RTU. Before script execution, a parameter is specified for the maximum percentage of tests which can be ran concurrently within each group. Another parameter specifies a maximum number of tests which can be ran concurrently across all groups (technically, the number of allotted threads).

Another possible use for this add-on is advanced reporting and automatic data collection. Since each script has the full expressive power of the Java programming language, there is much more flexibility for data presentation and analysis as compared to existing WebCTRL reporting tools.

## Basic Usage

1. If signed add-ons are required, copy the authenticating certificate [*ACES.cer*](https://github.com/automatic-controls/addon-dev-script/blob/main/ACES.cer?raw=true) to the *./addons* directory of your *WebCTRL* installation folder.
2. Install [*CommissioningScripts.addon*](https://github.com/automatic-controls/commissioning-scripts/releases/latest/download/CommissioningScripts.addon) using the *WebCTRL* interface.
3. Navigate to the add-on's main page.
4. Click the *Upload Script* button and select your script. An error will be thrown if any compilation errors occur. For debugging purposes, the stack trace of any compilation error is recorded in the *Error Log*.
5. Click *Semantic Mappings* and then *New Mapping* in order to select which parts of the WebCTRL system are accessible to the script.
6. Click *Add Group* and then *Add Equipment* to select which control programs should be tested.
7. Enter in a few semantic tags and mapping expressions to be referenced by the script. The *Examine* button on each control program can help to determine the expression to use for mapping a given semantic tag. Note that each tag mapping is actually a form of [regular expression](https://docs.oracle.com/en/java/javase/18/docs/api/java.base/java/util/regex/Pattern.html).
8. Press the *Save Changes* button as the last step to create your mapping. Note that all groups and mappings should be named.
9.  The *Preview Mapping Resolution* button should be used to determine whether each tag mapping is resolving correctly.
10. Navigate back to the *Scripts* page.
11. Select your newly created mapping, and specify the *Max. Active Tests Per Group* and *Total Max. Active Tests* parameters appropriately. Also toggle any desired script-specific options in the parameters column of the script table.
12. Press *Start* to initiate the corresponding script. You will be redirected to an output page upon successful initiation.
13. When completed, the final script output is recorded in the *Archived Tests* section.

## Scheduled Tests

[Cron expressions](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/support/CronExpression.html#parse-java.lang.String-) can be used to schedule scripts to execute at regular intervals. Scheduled script output can be automatically emailed to a set of recipients if necessary. This add-on relies upon the email server configured in general WebCTRL settings: *System Settings / General / Email Server Configuration*.

To prevent inadvertent schedule modification, each schedule stores a hash of the mapping and script it references. When the mapping or script is modified, hash validation for the schedule will fail, and it will be effectively disabled until someone intervenes. Hashes are recomputed each time an operator saves a schedule.

## Script Development

Scripts are compiled using [Janino 3.1.7](http://janino-compiler.github.io/janino/) at runtime. Script source code is uploaded directly into the WebCTRL server. **You do not have to configure a development environment for script compilation.** In principle, you could develop scripts using only a text editor like *Notepad*. Another advantage of this approach is that you can download, edit, and re-upload script source code from any computer with a connection to the WebCTRL server.

To ease the development process, it is suggested to use an IDE (e.g, [Visual Studio Code](https://code.visualstudio.com/)), and then add the following dependencies: [CommissioningScripts.jar](https://github.com/automatic-controls/commissioning-scripts/releases/latest/download/CommissioningScripts.jar) and [CommissioningScripts-sources.jar](https://github.com/automatic-controls/commissioning-scripts/releases/latest/download/CommissioningScripts-sources.jar). These dependencies will provide you will intellisense and Javadocs for script development. You are also welcome to reference any other dependencies provided by WebCTRL at runtime (e.g, the add-on API, or `javax.servlet`).

All scripts should extend `aces.webctrl.scripts.commissioning.core.Script`. See [SummaryReport.java](./samples/SummaryReport/SummaryReport.java) for a relatively complete example. This add-on does not enforce any security restrictions on what scripts can do. Scripts are executed with the same privilege set as the add-on. As such, you must be careful to **use scripts at your own risk.** Note that only WebCTRL server administrators are allowed to manage scripts.

### Janino Limitations

The official list of limitations may be found [here](http://janino-compiler.github.io/janino/#limitations). The most annoying limitation I've come across is a lack of support for generic types. Pretty much all generic type parameters should be treated as `Object`, and you'll have to cast generic objects back to their intended type when you reference them.

Janino doesn't like having unitialized variables. Janino's compiler seems unable to correctly determine if a variable is initialized. As a result, it will throw cryptic errors at odd times. I suggest initializing variables as soon as you declare them (e.g, use `null` for objects). I've created two issues ([172](https://github.com/janino-compiler/janino/issues/172) and [178](https://github.com/janino-compiler/janino/issues/178)) in the [Janino repository](https://github.com/janino-compiler/janino) regarding this problem.

See [177](https://github.com/janino-compiler/janino/issues/177) for another issue I came across. Feel free to report other errors you uncover at the [Janino repository](https://github.com/janino-compiler/janino/issues).