# Commit024 - Dynamic room management

## Added
- Add rooms from the manager home screen or settings.
- Enable/disable rooms without deleting saved settings.
- Delete rooms when they are not running.
- Move rooms up/down to change display order.
- Persist dynamic room list, order, settings and enabled state.
- Hide disabled rooms from manager home, widget, notification and guest selection.
- Send an active-room catalog over TCP so removed/disabled rooms disappear from guest devices.

## Safety rules
- A running room cannot be disabled or deleted.
- The final remaining room cannot be deleted.
- Disabled rooms remain saved and can be enabled again later.
