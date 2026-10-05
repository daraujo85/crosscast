# Implementation Plan: Professional UI Redesign for Campilot

This plan outlines the steps to transform the Campilot app interface into a professional, modern, and elegant "Apple-style" camera app, as described in `@app/ui.md`.

## 1. Foundation and Theming
- [x] Create `com.campilot.ui.theme` package.
- [x] Define `CampilotTheme` with a dark color scheme, modern typography (San Francisco style), and custom shapes.
- [x] Define common color constants (Glass background, Yellow highlight, Red Live).

## 2. Reusable UI Components
- [x] Create `com.campilot.ui.components` package.
- [x] Implement `GlassPanel`: A container with background blur and subtle borders.
- [x] Implement `StatusPill`: For "Online", "Offline", "LIVE", and "OBS Connected" statuses.
- [x] Implement `ZoomSelector`: A segmented control/pill for quick zoom levels (`0.6x`, `1x`, `2x`).
- [x] Implement `ZoomSlider`: A minimal and elegant slider for fine-grained zoom.
- [x] Implement `RecordButton`: A professional, animated button for starting/stopping transmission.
- [x] Implement `IconActionButton`: Circular, blurred buttons for QR Code and Camera Switch.

## 3. Main Interface Refactoring (`MainActivity.kt`)
- [x] **Zona Superior (Status & Connection):**
    - [x] Create a floating `GlassPanel` for the top bar.
    - [x] Include Connection Indicator, "Live Camera" title, IP Address, Device Model.
    - [x] Add Flash and Settings (or Quality) icons to the right.
- [x] **Zona Central (Focus & Feedback):**
    - [x] Refine `GridOverlay` (thinner lines, lower opacity).
    - [x] Add `FocusIndicator` (animated square/circle on tap).
    - [x] Add `LivePill` (floating "LIVE" badge with pulse animation).
- [x] **Zona Inferior (Main Controls):**
    - [x] Implement the bottom control area using a `GlassPanel`.
    - [x] Integrate `ZoomSelector`, `ZoomSlider`, and the row of action buttons (`QR Code`, `RecordButton`, `Switch Camera`).

## 4. Connection Modal (OBS / Campilot)
- [x] Replace the current `AlertDialog` with a modern `ModalBottomSheet` (using `ConnectionSheet` component).
- [x] Design the sheet with a blur background, large QR Code, and a styled IP address card with a "Copy" button.

## 5. Animations and Refinement
- [x] Implement transition animations for UI panels (fade/slide).
- [x] Add pulse animation to the LIVE indicator.
- [x] Add feedback animations to buttons.
- [x] Ensure full-screen/safe area compliance.

## 6. Verification and Testing
- [x] Verify UI on different screen sizes.
- [x] Ensure all existing features (Zoom, Flash, Server Start/Stop, Camera Switch, QR Code) work correctly with the new UI.
- [x] Check for any performance regressions due to blur/glassmorphism.
