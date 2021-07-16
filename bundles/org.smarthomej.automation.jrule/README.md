# J Rule
Write OpenHAB Rules using Java

This automation package aims to enable Java development of OpenHAB Rules. The addon will allow the user to create custom OpenHAB rules
in one or several .java-files. The Java Rules will need defined triggers in order for the engine to know how and when to execute them. The triggers
are very similar to the triggers in Rules DSL but expressed using java annotations. In order to execute rules based on items defined in OpenHAB either in .items-files or the GUI. The addon needs to know about these items and this is realized by the Rule Engine where it generates a .java and a .class file for each item in the system. The class files are then packaged in a .jar-file which the user can use as dependency when doing Rules Development.
For the addon to be able to pick up rules, they first need to be compiled by the addon. The source .java rules-files are placed in a specific rules folder and
will be automatically compiled and loaded into OpenHAB when the addon is started. The syntax for rules as well as the design and thinking behind the addon is to provide something that is similar to Rules DSL but more powerful and customizable.

# Limitations
- Currently only working for OpenHAB installations under Linux / Unix Operating Systems, not supported in Windows (for rules development its fine to use windows)
- Not supporting OH3 GUI rules, script actions and script conditions 

# Why?
 - You will be able to use a standard Java IDE to develop your rules. 
 - Full auto completion (Shift space) for all items, less chance of errors and typos
 - Take full advantage of all java design patters
 - Share and reuse code for you rules
 - Advanced timers and locks are built in and can be used without cluttering the code
 - Possibility to write junit-tests to test your rules
 - Use any 3rd party dependencies and libraries in your rules
 - You will be able to use JRule in parallell with any other Rules engine if you want to give it a try

# Who?
This addon is not for beginners, you should have knowledge in writing java-programs or a desire to do so.

# Maturity
Alpha, you can expect big changes in syntax and everything else. Please contribute if you can

# Download
Prebuilt jar file is available in the bin folder under https://github.com/seaside1/jrule

# Java Rule Engine

Input rules files
will be placed under:
/etc/automation/jrule/rules/org/openhab/automation/jrule/rules/user/

Output jar files to be added by the user as dependencies when doing rule development will be located under:
/etc/openhab/automation/jrule/jar

The following jar files can be found under the jrule/jar-folder:

| Jar File                               | Description                                                                                   |
| -------------------------------------- | --------------------------------------------------------------------------------------------- |
| jrule-items.jar                        | Contains all generated items, which will be used when developing rules                        |
| jrule.jar                              | JRule Addon classes neeed as dependency when doing development                              |


# Get started with the JRule Automation Addon
- Install the addon by copying the org.openhab.automation.jrule-3.x.x-ALPHAX.jar to openhab-addons folder
  Download latest release from https://github.com/seaside1/jrule/tree/main/bin
- In default location is /etc/openhab/automation/jrule
- When the addon is started it will:
1. Create JAVA source files for all items 
2. Compile java source files and create a resulting jrule.jar file under /etc/openhab/automation/jrule/jar
3. Compile any java rules file under  /etc/openhab/automation/jrule/rules/org/openhab/automation/jrule/rules/user/
4. Create jar files with dependencies to be used when creating your java-rules (jrule-items.jar).
The two jar files needed for Java rules development can be found under /etc/openhab/automation/jrule/jar

Once the JAVA rule engine has started and compiled items successfully you can either copy the jar files
form /etc/openhab/automation/jrule/jar/* to the place where you intend to develop the Java- Rules, or share that folder
using samba / CIFS / NFS or similar.
- Set up your favourite IDE as a standard java IDE. 
- Create a new empty java project
- Create a package / folder org.openhab.automation.jrule.rules.user
- Place your Java rules file in this folder

NOTE: The rules will be reloaded if they are modified. Any java file you place under /etc/openhab/automation/jrule/rules/org/openhab/automation/jrule/rules/user/
will be compiled or recompiled, you don't have to restart OpenHAB.

Designing your Java Rules File (Hello World)
1. Start by adding an item in Openhab.
Group JRule
Switch MyTestSwitch  "Test Switch" (JRule)
Switch MyTestSwitch2  "Test Switch 2" (JRule)

2. Create the following class

```java
package org.smarthomej.automation.jrule.rules.user;
import static org.openhab.automation.jrule.rules.JRuleOnOffValue.ON;
import org.smarthomej.automation.jrule.items.generated._MyTestSwitch;
import org.smarthomej.automation.jrule.rules.JRule;
import org.smarthomej.automation.jrule.rules.JRuleName;
import org.smarthomej.automation.jrule.rules.JRuleWhen;

public class MySwitchRule extends JRule {

    private static final String LOG_NAME="MY_TEST";
    
    @JRuleName("MySwitchRule")
    @JRuleWhen(item = _MyTestSwitch.ITEM, trigger = _MyTestSwitch.TRIGGER_CHANGED_TO_ON)
    public void execOffToOnRule() {
	logInfo("||||| --> Hello World!");
    }
    
    @Override
    protected String getRuleLogName() {
        return LOG_NAME;
    }
}
```

Make sure you add the Jar-files from /etc/openhab/jrule/jar as dependencies.

# Third Party External Dependencies
You can add any 3rd party library as dependency. Copy the jar files needed to /etc/openhab/automation/jrule/ext-lib
The Automation Engine will automatically pick these dependencies up when it is compiling the rules.

# Core Actions
Built in Core Actions that can be used
| Action                                 | Description                                                                                   |
| -------------------------------------- | --------------------------------------------------------------------------------------------- |
| say                                    | Will use VoiceManager to say action see Example 13                        |
| commandLineExecute                     | See Example 14                               |


# Examples 

## Example 1
Use Case: Invoke another item Switch from rule
```java
    @JRuleName("MyRuleTurnSwich2On")
    @JRuleWhen(item = _MyTestSwitch.ITEM, trigger = _MyTestSwitch.TRIGGER_CHANGED_TO_ON)
    public void execChangedToRule() {
    	logInfo("||||| --> Executing rule MyRule: changed to on");
        _MySwitch2.sendCommand(ON);
    }
```

## Example 2
Use case: Invoke a Doorbell, but only allow the rule to be invoke once every 20 seconds.
This is done by aquiring a lock getTimedLock("MyLockTestRule1", 20).

```java
    @JRuleName("MyLockTestRule1")
    @JRuleWhen(item = _MyTestSwitch2.ITEM, trigger = _MyTestSwitch2.TRIGGER_CHANGED_FROM_OFF_TO_ON)
    public void execLockTestRule() {
        if (getTimedLock("MyLockTestRule1", 20)) {
            _MyDoorBellItem.sendCommand(ON);
            logInfo("||||| --> Got Lock! Ding-dong !");
        } else {
            logInfo("||||| --> Ignoring call to rule it is locked!");
        }
    }
```
## Example 3
Use case: Use the value that caused the trigger
When the rule is triggered, the triggered value is stored in the event.

```java
   @JRuleName("MyEventValueTest")
   @JRuleWhen(item = __MyTestSwitch2.ITEM, trigger = __MyTestSwitch2.TRIGGER_RECEIVED_COMMAND)
   public void myEventValueTest(JRuleEvent event) {
	  logInfo("Got value from event: {}", event.getValue());
   }
```
## Example 4
Use case: Or statement for rule trigger
To add an OR statement we simply add multiple @JRuleWhen statements

```java
   @JRuleName("MyNumberRule1")
   @JRuleWhen(item = _MyTestNumber.ITEM, trigger = _MyTestNumber.TRIGGER_CHANGED, from = "14", to = "10")
   @JRuleWhen(item = _MyTestNumber.ITEM, trigger = _MyTestNumber.TRIGGER_CHANGED, from = "10", to = "12")
   public void myOrRuleNumber(JRuleEvent event) {
	logInfo("Got change number: {}", event.getValue());
   }
```

## Example 5
Use case: Define your own functionality
Create a Rules class that extends: JRuleUser.java
JRuleUser.java should be placed in the same folder as your rules:

```java
package org.smarthomej.automation.jrule.rules.user;

import org.smarthomej.automation.jrule.rules.JRule;

public abstract class JRuleUser extends JRule {

	
}
```

## Example 6
Your class rules can now extend the JRuleUser
package org.smarthomej.automation.jrule.rules.user;
```java
import static org.openhab.automation.jrule.rules.JRuleOnOffValue.ON;
import org.smarthomej.automation.jrule.items.generated._MyTestSwitch;
import org.smarthomej.automation.jrule.rules.JRule;
import org.smarthomej.automation.jrule.rules.user.JRuleUser;
import org.smarthomej.automation.jrule.rules.JRuleName;
import org.smarthomej.automation.jrule.rules.JRuleWhen;

public class MySwitchRule extends JRuleUser {

    private static final String LOG_NAME="MY_RULE";
    
    @Override
    protected String getRuleLogName() {
        return LOG_NAME;
    }
}
```

## Example 7
Let's say we want to add a common function that should be available for all user rules.
We want to add a function that checks if it is ok to send notifications debing on what time it is.
We'll do this:

```java
package org.smarthomej.automation.jrule.rules.user;

import org.smarthomej.automation.jrule.rules.JRule;

public abstract class JRuleUser extends JRule {

	private static final int startDay = 8;
	private static final int endDay = 21;
	
	
	protected boolean timeIsOkforDisturbance() {
		return nowHour() >= startDay && nowHour() <= endDay;
	}
	
}
```

``
We then extend the rule from the Java Rules file:

```java
package org.smarthomej.automation.jrule.rules.user;

import org.smarthomej.automation.jrule.items.generated._MyTestSwitch;
import org.smarthomej.automation.jrule.rules.JRuleEvent;
import org.smarthomej.automation.jrule.rules.JRuleName;
import org.smarthomej.automation.jrule.rules.JRuleWhen;

public class MyTestUserRule extends JRuleUser {
	private static final String MY_LOGNAME = "TEST";

	@JRuleName("TestUserDefinedRule")
	@JRuleWhen(item = _MyTestSwitch.ITEM, trigger = _MyTestSwitch.TRIGGER_RECEIVED_COMMAND)
	public void mySendNotificationRUle(JRuleEvent event) {
		if (timeIsOkforDisturbance()) {
			logInfo("It's ok to send a distrubing notification");
		}
	}
	
    @Override
    protected String getRuleLogName() {
        return MY_LOGNAME;
    }

}
```
## Example 8
Use case create a timer for automatically turning of a light when it is turned on. If it's running cancel it and schedule a new one. 
```java
    @JRuleName("myTimerRule")
    @JRuleWhen(item = _MyLightSwitch.ITEM, trigger = _MyLightSwitch.TRIGGER_CHANGED_TO_ON)
    public synchronized void myTimerRule(JRuleEvent event) {
        logger.info("myTimerRule Turning on light it will be turned off in 2 mins");
        createOrReplaceTimer(_MyLightSwitch.ITEM, 2 * 60, (Void) -> { // Lambda Expression
            logInfo("Time is up! Turning off lights");
            _MyLightSwitch.sendCommand(OFF);
        });
    }
```
## Example 9
Use case: Let's say we have a 433 MHz wall socket with no ON/OFF feedback and a bit of bad radio reception. We can then create a repeating timer
to send multiple ON statements to be sure it actually turns on.
 createOrReplaceRepeatingTimer("myRepeatingTimer", 7, 4, will create a repeating timer that will trigger after 0 seconds, 7s, 14s and 21s 
 If the Timer is already running it will cancel it and create a new one.
 
```java
    @JRuleName("repeatRuleExample")
    @JRuleWhen(item = _MyTestSwitch.ITEM, trigger = _MyTestSwitch.TRIGGER_CHANGED_TO_ON)
    public synchronized void repeatRuleExample(JRuleEvent event) {
        createOrReplaceRepeatingTimer("myRepeatingTimer", 7, 10, (Void) -> { // Lambda Expression
            final String messageOn = "repeatRuleExample Repeating.....";
            logInfo(messageOn);
            _MyBad433Switch.sendCommand(ON);
        });
    }
```

## Example 10
Use case Create a simple timer. When MyTestSwitch turns on it will wait 10 seconds and then turn MyTestSwitch2 to on. Note that
it will not reschedule the timer, if the timer is already running it won't reschedule it.
```java
    @JRuleName("timerRuleExample")
    @JRuleWhen(item = _MyTestSwitch.ITEM, trigger = _MyTestSwitch.TRIGGER_CHANGED_TO_ON)
    public synchronized void timerRuleExample(JRuleEvent event) {
        createTimer("myTimer", 10, (Void) -> { // Lambda Expression
            final String messageOn = "timer example.";
            logInfo(messageOn);
            _MyTestWitch2.sendCommand(ON);
        });
    }
```
## Example 11
Use case trigger a rule at 22:30 in the evening to set initial brightness for a ZwaveDimmer to 30%
```java
  @JRuleName("setDayBrightness")
  @JRuleWhen(hours=22, minutes=30)
  public synchronized void setDayBrightness(JRuleEvent event) {
      logInfo("Setting night brightness to 30%");
      int dimLevel = 30;
      _ZwaveDimmerBrightness.sendCommand(dimLevel);
  }
```

## Example 12
Use case: If temperature is below or equals to 20 degrees send command on to a heating fan 
It is possible to use:
lte = less than or equals
lt = less than
gt = greater than
gte = greater than or equals
eq = equals
```java
  @JRuleName("turnOnFanIfTemperatureIsLow")
  @JRuleWhen(item = _MyTemperatureSensor.ITEM, trigger = _MyTemperatureSensor.TRIGGER_RECEIVED_UPDATE, lte = 20)
  public synchronized void turnOnFanIfTemperatureIsLow(JRuleEvent event) {
      logInfo("Starting fan since temeprature dropped below 20");
      _MyHeatinFanSwitch.sendCommand(ON);
  }
```

## Example 13
Use case: Using say command for tts
```java
    @JRuleName("testSystemTts")
    @JRuleWhen(item = _TestSystemTts.ITEM, trigger = _TestSystemTts.TRIGGER_CHANGED_TO_ON)
    public synchronized void testSystemTts(JRuleEvent event) {
        logInfo("System TTS Test");
        String message = "Testing tts! I hope you can hear it!";
        say(message, null, "sonos:PLAY5:RINCON_XXYY5857B06E0ZZOO");
    }
```

## Example 14
Use case: Using say command line execute
```java
   
```
## Example 15
Use case: A group of switches, see if status is changed, and also which member in the group changed state
```java
    @JRuleName("groupMySwitchesChanged")
    @JRuleWhen(item = _gMySwitchGroup.ITEM, trigger = _gMySwitchGroup.TRIGGER_CHANGED)
    public synchronized void groupMySwitchGroupChanged(JRuleEvent event) {
        final boolean groupIsOnline = event.getValueAsOnOffValue() == ON;
        final String memberThatChangedStatus = event.getMemberName();
	logInfo("Member that changed the status of the Group of switches: {}", memberThatChangedStatus);
    }
```

## Example 16
Use case: A group of switches , trigger when it's changed from OFF to ON
```java
    @JRuleName("groupMySwitchesChangedOffToOn")
    @JRuleWhen(item = _gMySwitchGroup.ITEM, trigger = _gMySwitchGroup.TRIGGER_CHANGED, from="OFF", to="ON")
    public synchronized void groupMySwitchesChangedOffToOn(JRuleEvent event) {
	logInfo("Member that changed the status of the Group from OFF to ON: {}", event.getMemberName());
    }
```


# Changelog
## ALPHA6
- Added group functionality getMember will return who triggered a change for a group
## ALPHA5
- Removed dependencies on slf4japi and eclipse annotations
- Added logInfo logDebug (to wrap slf4j and remove dep)
- Fixed compilation of rules to be more robust with internal dependencies 
## ALPHA4
- Refactored completable futures
- Added 5 seconds of delay for initialization of the rule engine to avoid multiple reloads
- Added support for play & pause for player item
- Added commandLineExecute
## ALPHA3
- Fixed issue when reloading rules if they are changed with monitored items
- Fixed classpath issue when executing rules using 3rd party libraries 
## ALPHA2
- Added possibility to include 3rd party libraries when developing rules
## ALPHA1
- Refactored internal jar dependencies and jar-generation
- Added eq comparator for number triggers in rules

# Roadmap
- Locks and timers by annotation
- Built in expire functionality
