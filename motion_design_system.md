# DIAMONDS MOTION DESIGN SYSTEM
## High-End Kinetic Guidelines for 60 FPS Experiences

Motion in the Diamonds application is not decoration; it establishes physical depth, tactile continuity, and operational efficiency. This system ensures buttery-smooth **60 FPS** transitions on modern mobile devices.

---

### 1. INTERACTION DURATIONS & CURVES

We prioritize lightweight, high-speed micro-movements to avoid causing lag or delaying the user interface.

| Interaction Type | Duration | Standard Curve / Interpolator | Custom Bezier Values (`cubic-bezier`) |
| :--- | :--- | :--- | :--- |
| **Micro-Interactions (Ripples, Switches)** | `150ms` | Fast deceleration | `cubic-bezier(0.1, 0.9, 0.2, 1.0)` |
| **Bento Card Scale / Touch Press** | `200ms` | Standard Out | `cubic-bezier(0.0, 0.0, 0.2, 1.0)` |
| **Bottom Sheet Slide-In** | `350ms` | Bouncy spring / Decelerate | `cubic-bezier(0.175, 0.885, 0.32, 1.1)` |
| **Shared Element Transitions** | `400ms` | Path / Arc Interpolator | `cubic-bezier(0.4, 0.0, 0.2, 1.0)` |

---

### 2. PHYSICS-BASED SPRING SPECIFICATIONS

For gestures and physical sheet dragging, we bypass traditional linear durations and rely entirely on spring forces.

#### A. Tight Spring (Switches, Badge toggles)
*   **Stiffness / Tension:** `220` (equivalent to `700` in React Native animated)
*   **Damping / Friction:** `25`
*   **Mass:** `0.8` (feels crisp and immediate)

#### B. Heavy Fluid Spring (Bottom sheets, Card expansion)
*   **Stiffness / Tension:** `140`
*   **Damping / Friction:** `18`
*   **Mass:** `1.0` (feels luxurious, premium, and soft)

---

### 3. CRITICAL MOTION SCHEMES

#### Shared Element Transitions (SETs)
When opening a bento card (e.g., a Fleet Yacht card) into its fullscreen details screen:
1.  **Arc Path**: The bounding box moves along a 2D arc rather than a strict diagonal line.
2.  **Shared Image Scaling**: The asset image scales without interpolation flashes.
3.  **Crossfade Secondary Elements**: Non-shared labels on the parent card fade out (`duration = 100ms`), and new detailed rows slide up synchronously from the bottom (`duration = 250ms`, `offsetY = 16dp`).

#### Bottom Sheet Drag-to-Dismiss
*   **Elastic Boundary**: Dragging past sheet limits triggers standard logarithmic resistance (Friction multiplier: `y_visual = ln(y_drag)`).
*   **Velocity Handshake**: If dragging velocity exceeds `1200 dp/s` upon release, dismiss the sheet instantly; otherwise, settle back to snap anchors using **Heavy Fluid Spring**.

#### Micro-Interactions
*   **The Tactile Click (Scale Ripple)**: Pressing an item triggers a scale response down to `0.97` combined with a central Material 3 ripple expand. Upon touch release, the element snaps back to `1.0` with a tight spring.
