# Terminal Unit Test

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

