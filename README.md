# CommissioningScripts

WebCTRL is a trademark of Automated Logic Corporation. Any other trademarks mentioned herein are the property of their respective owners.

- [CommissioningScripts](#commissioningscripts)
  - [About](#about)
  - [Basic Usage](#basic-usage)
  - [Scheduled Tests](#scheduled-tests)

## About

This add-on allows WebCTRL server administrators to upload and execute scripts, which are written using the Java programming language. Scripts are compiled using [Janino 3.1.7](http://janino-compiler.github.io/janino/), so a few small syntax [limitations](http://janino-compiler.github.io/janino/#limitations) apply. The add-on does not come prepackaged with any scripts; however, a few examples may be found within [*./samples*](./samples).

The original purpose of this add-on was to be a customizable alternative to the ACxelerate commissioning tool provided by Automated Logic. Semantic tagging is one concept borrowed from ACxelerate. Before execution, each script is paired with a mapping that associates control program microblock nodes to tag names referenced by the script. Primary script procedures are executed once on each control program specified by a mapping.

The use-case considered while developing this add-on was VAV box commissioning. One VAV box test locks the damper to various positions and records the resulting airflow. If all VAV dampers are closed at once, the high static pressure safety may trip on the RTU serving the VAV boxes. Thus, it is necessary to limit how many control programs are tested concurrently under a given RTU.

Within each mapping, control programs are separated into groups. For VAV commissioning, there should be one group for each RTU. Before script execution, a parameter is specified for the maximum percentage of tests which can be ran concurrently within each group. Another parameter, *Allotted Threads*, specifies a maximum number of tests which can be ran concurrently across all groups.

Another possible use for this add-on is advanced reporting and automatic data collection. Since each script has the full expressive power of the Java programming language, there is much more flexibility for data presentation and analysis as compared to existing WebCTRL reporting tools.

## Basic Usage

1. Navigate to the add-on's main page.
2. Click the *Upload Script* button and select your script. An error will be thrown if any compilation errors occur. For debugging purposes, the stack trace of any compilation error is recorded in the *Error Log*.
3. Click *Semantic Mappings* and then *New Mapping* in order to select which parts of the WebCTRL system are accessible to the script.
4. Click *Add Group* and then *Add Equipment* to select which control programs should be tested.
5. Enter in a few semantic tags and mapping expressions to be referenced by the script. The *Examine* button on each control program can help to determine the expression to use for mapping a given semantic tag. Note that each tag mapping is actually a form of [regular expression](https://docs.oracle.com/en/java/javase/18/docs/api/java.base/java/util/regex/Pattern.html).
6. Press the *Save Changes* button as the last step to create your mapping. Note that all groups and mappings should be named.
7. The *Preview Mapping Resolution* button should be used to determine whether each tag mapping is resolving correctly.
8. Navigate back to the *Scripts* page.
9. Select your newly created mapping, and specify the *Maximum Active Tests Per Group* and *Allotted Threads* parameters appropriately. Also toggle any desired script-specific options in the parameters column of the script table.
10. Press *Start* to initiate the corresponding script. You will be redirected to an output page upon successful initiation.
11. When completed, the final script output is recorded in the *Archived Tests* section.

## Scheduled Tests

[Cron expressions](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/support/CronExpression.html#parse-java.lang.String-) can be used to schedule scripts to execute at regular intervals. Scheduled script output can be automatically emailed to a set of recipients if necessary.