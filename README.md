# CogniTrack

CogniTrack turns digital behavior into fitness-style analytics:

- Workout -> app session
- Distance -> time spent
- Pace -> intensity from app switches and interruptions
- Heart rate -> cognitive load from notifications and multitasking
- Route -> app flow sequence

The project is split into:

- `capture-core` for collectors and permission routing
- `analytics-core` for session stitching and digital fitness metrics
- `storage` for Room persistence and retention jobs
- `ui-dashboard` for the athletic-editorial Compose dashboard
- `app` for the Android entry point
