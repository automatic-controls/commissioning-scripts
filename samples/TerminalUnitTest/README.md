# Terminal Unit Commissioning Script v0.1.0

- [Terminal Unit Commissioning Script v0.1.0](#terminal-unit-commissioning-script-v010)
  - [Overview](#overview)
  - [High-Level Pseudocode](#high-level-pseudocode)
    - [Supply Fan, Damper, and Hot Water Valve](#supply-fan-damper-and-hot-water-valve)
    - [Supply Fan and Heat Pump](#supply-fan-and-heat-pump)
  - [Interpreting Results](#interpreting-results)
    - [Supply Fan Tests](#supply-fan-tests)
    - [Damper Airflow Tests](#damper-airflow-tests)
    - [Sample Output](#sample-output)
  - [Mappings](#mappings)

## Overview

This script is intended to evaluate performance of control programs which optionally include a supply fan, damper, and heating element. Evaluating more than one of each component in a single control program is not yet supported. For example, this script should not be used to evaluate a program which controls two heat pumps at once.

Supply fan evaluation requires a binary output to command the fan and a binary input to monitor status. Damper evaluation requires an airflow microblock in the control program (works with controllers [ZN141A](https://www.automatedlogic.com/en/products/webctrl-building-automation-system/building-controllers/zn141a/), [ZN341A](https://www.automatedlogic.com/en/products/webctrl-building-automation-system/building-controllers/zn341a/), [OF141-E2](https://www.automatedlogic.com/en/products/webctrl-building-automation-system/building-controllers/OF141-E2/), and [OF342-E2](https://www.automatedlogic.com/en/products/webctrl-building-automation-system/building-controllers/OF342-E2/)). There are two heating configurations which can be evaluated. The first configuration requires a single analog output to vary heating output between 0 and 100 (e.g, hot water valves and SCR electric heat). The second configuration is intended for heat pumps (requires a binary output to command the compressor, a binary input to monitor compressor status, and another binary output to command the reversing valve). Note that cooling performance is also evaluated for heat pumps.

To evaluate performance of any heating component, a leaving air temperature sensor is required. An entering air temperature sensor is also recommended but not required. The idea is to monitor the temperature differential across a heating element over time. We expect the temperature differential to increase when heating is increased. If entering air temperature cannot be monitored, then the program operates under the assumption that it is constant.

If your control program does not match these specifications exactly, there are workarounds which can be implemented in the logic. For example, if a supply fan uses an analog input to monitor status by measuing amp draw, then you could throw an *BACnet Binary Value Status* microblock into the logic which turns on when the amp draw is above a certain threshold. Then you would map the `sfst` tag to this microblock instead.

You should ensure air and water sources are activated appropriately before running this test. For instance, someone should turn on the RTU's which serve the terminal units. If there are any hot water valves, then the boiler system should also be turned on, and the hot water temperature setpoint should be set to an appropriate value. Due to these considerations, it is not recommended to run this script on a schedule. In the future, I may add functionality which addresses this.

## High-Level Pseudocode

### Supply Fan, Damper, and Hot Water Valve

1. Lock the hot water valve to 0% open.
2. Sleep for 30 seconds.
3. Lock the supply fan command to off.
4. Wait up to 3 minutes for supply fan status to indicate the fan is off. If the time limit is exceeded, throw an error: *Unresponsive*.
5. Lock the supply fan command to on.
6. Wait up to 3 minutes for supply fan status to indicate the fan is on. If the time limit is exceeded, throw an error: *Unresponsive*.
7. Lock damper position to 0%.
8. Wait up to 4 minutes for actual damper position to reach the locked position. If the time limit is exceeded, throw an error: *Unresponsive*.
9. Record the actual airflow (*cfm*) 5 times, waiting 2 seconds between each measurement.
10. Repeat steps 7-9, incrementing the locked damper position by 5% until 100% is reached. So we have 5 airflow measurements for each damper position which is a multiple of 5%, resulting in 105 total airflow measurements. For each damper position, the 5 corresponding measurements are averaged.
11. Lock the damper airflow setpoint to the maximum heating *cfm* design parameter or 150 *cfm*, whichever is larger.
12. Wait up to 4 minutes for actual airflow to come within 50 *cfm* of the heating maximum. An error will **not** be thrown if the time limit is exceeded.
13. Record the current temperature differential. All future temperature measurements will be adjusted by this value (this reading is treated as the origin of the temperature graph).
14. Lock the hot water valve to 100% open.
15. Sleep for 10 seconds.
16. Record the current temperature differential.
17. Check for sufficient airflow (either supply fan status is on, or airflow is reading above 90 *cfm*). In the case of insufficient airflow, throw an error: *Loss of Airflow*.
18. If leaving air temperature exceeds 120&deg;F, skip to step 20.
19. Repeat steps 15-18 at most 60 times, lasting approximately 10 minutes. After 3.5 minutes, the test may terminate prematurely if the program detects the temperature differential has stabilized before 10 minutes are up.
20. Revert all node values modified by the script to their original values.

### Supply Fan and Heat Pump

1. Lock the heat pump compressor command to off.
2. Wait up to 3 minutes for compressor status to indicate the heat pump is off. If the time limit is exceeded, do **not** throw an error.
3. Lock the supply fan command to off.
4. Wait up to 3 minutes for supply fan status to indicate the fan is off. If the time limit is exceeded, throw an error: *Unresponsive*.
5. Lock the supply fan command to on.
6. Wait up to 3 minutes for supply fan status to indicate the fan is on. If the time limit is exceeded, throw an error: *Unresponsive*.
7. Lock the heat pump reversing valve command to off.
8. Wait up to 3 minutes for compressor status to indicate the heat pump is off. If the time limit is exceeded, throw an error: *Compressor Stop Failure*.
9. Sleep for 8 minutes.
10. Measure the temperature differential 4 times at 3 second intervals. The average is treated as a baseline (e.g, used as the origin of the temperature graph).
11. Lock the heat pump compressor command to on.
12. Wait up to 3 minutes for compressor status to indicate the heat pump is on. If the time limit is exceeded, throw an error: *Compressor Start Failure*.
13. Sleep for 10 seconds.
14. Record the current temperature differential.
15. Check for sufficient airflow (i.e, that supply fan status is on). In the case of insufficient airflow, throw an error: *Loss of Airflow*.
16. If leaving air temperature is less than 40&deg;F or greater than 120&deg;F, lock the reversing valve command to on if you have not already done so; otherwise, jump to step 18.
17. Repeat steps 13-16 at most 180 times, lasting approximately 30 minutes. The reversing valve command will be locked to on about half way through the process. After 5 minutes of having the reversing valve locked in either configuration, the test may terminate prematurely if the program detects the temperature differential has stabilized.
18. Revert all node values modified by the script to their original values.

## Interpreting Results

The location column provides a link which navigates to the selected control program in WebCTRL. When you hover over a cell in the duration column, a tooltip tells you the precise start and end time for that test. When you hover over a non-graph cell in the damper airflow column, a tooltip tells you the maximum cooling *cfm* design parameter for that damper. When you click on a cell in the damper airflow column, the cell will expand into a graph showing airflow (*cfm*) vs. damper position (*%*). When you click on a cell in the temperature differential column, the cell will expand into a graph showing temperature vs. time. The *toggle graph visibility* button can be used to show or hide all graphs at once. The *export data* button will download all the raw data as a *.json* file.

Results are color coded. If no problems are detected, green is used with a message: *success*. If there is any sort of communication error or the script is unable to get and set node values, magenta is used with a message: *error*. Red is used for any other sort of problem detected during the test or the subsequent data analysis. The magenta error message can show up anywhere, so we refrain from specifically mentioning it in the following sections. If anything unexpected occurs, it is recommended that you check the error log page of the commissioning scripts add-on.

### Supply Fan Tests

The supply fan test columns will display either *success* or *unresponsive*. If a fan is commanded to start, but status remains off, then the *fan start* test has a result of *unreponsive*. If a fan is commanded to stop, but status remains on, then the *fan stop* test has a result of *unreponsive*. Refer to the [pseudocode](#high-level-pseudocode) to see exactly when *unresponsive* errors are thrown.

### Damper Airflow Tests

The *damper airflow* test column will display either *success*, *unresponsive*, or *failure*.

### Sample Output

![](./report1.png)
![](./report2.png)

## Mappings

Control programs with dampers should be grouped by air source. By limiting the percentage of active tests per group, we can avoid tripping the high static safety alarm on the RTU air source (for a worst case scenario, imagine all VAV dampers locked to 0% while the RTU supply fan is still pumping air into the system). If there are hot water valves, you should also consider grouping by water source. See the following table for a list of mapping tags for this script. Also see [./tags.json](./tags.json) for the tag mappings I used while testing this script.

| Semantic Tag | Sample Expression | Description |
| - | - | - |
| `eat` | `eat/present_value` | Monitors entering air temperature (*&deg;F*). |
| `lat` | `lat/present_value` | Monitors leaving air temperature (*&deg;F*). |
| `sfst` | `sfst/present_value` | Monitors supply fan status (either `true` or `false`). |
| `sfss_lock_flag` | `sfss/locked` | Controls whether the supply fan command is locked or unlocked. |
| `sfss_lock_value` | `sfss/locked_value` | When the supply fan command is locked, it assumes this value. |
| `damper_position` | `airflow/flow_tab/damper_position` | Monitors the actual damper position (between `0` and `100`). |
| `damper_lock_flag` | `airflow/flow_tab/lock_flags/damper` | Controls whether damper position is locked or unlocked. |
| `damper_lock_value` | `airflow/flow_tab/damper_lock` | When damper position is locked, it assumes this value. |
| `airflow` | `airflow/flow_tab/actual_flow` | Monitors airflow in units of *cfm*. |
| `airflow_lock_flag` | `airflow/flow_tab/lock_flags/flowsetp` | Controls whether the airflow setpoint is locked or unlocked. |
| `airflow_lock_value` | `airflow/flow_tab/flowsetp_lock` | When the airflow setpoint is locked, it assumes this value. In the absence of other control, the damper position automatically fluctuates in an attempt to maintain airflow setpoint. |
| `airflow_max_cool` | `airflow/flow_tab/max_cool` | Monitors the maximum cooling airflow design parameter (*cfm*). |
| `airflow_max_heat` | `airflow/flow_tab/max_heat` | Monitors the maximum heating airflow design parameter (*cfm*). |
| `heating_AO_position` | `heat/present_value` | Monitors the percentage of active heating (between `0` and `100`). |
| `heating_AO_lock_flag` | `heat/locked` | Whether the active heating percentage is locked or unlocked. |
| `heating_AO_lock_value` | `heat/locked_value` | When the active heating percentage is locked, it assumes this value. |
| `pump_status` | `comp_st/present_value` | Monitors heat pump compressor status (either `true` or `false`). |
| `pump_cmd_lock_flag` | `comp_ss/locked` | Whether the heat pump compressor command is locked or unlocked. |
| `pump_cmd_lock_value` | `comp_ss/locked_value` | When the heat pump compressor command is locked, it assumes this value. |
| `pump_rev_lock_flag` | `pump_vlv/locked` | Whether the heat pump reversing valve is locked or unlocked |
| `pump_rev_lock_value` | `pump_vlv/locked_value` | When the heat pump reversing valve is locked, it assumes this value. |