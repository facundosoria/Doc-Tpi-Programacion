---
name: frontend-ui-kit-compliance
description: >-
  Build or review Angular frontend UI using the official @2026-p4-fe/ui library,
  its published API, tokens, and theme behavior. Use whenever a frontend page,
  component, template, or style is created or changed.
---

# Official UI Kit compliance

Apply this skill only in `2026-PIV-TPI-FE`. The authoritative installed package is
`@2026-p4-fe/ui`; inspect its installed exports and existing imports before selecting a
component. Do not infer an import path from a component name or from an older library
version.

## Required approach

Use the official library for generic visual primitives and feedback before writing any
custom markup or CSS. The UI Kit includes, among other components, buttons, navbar,
modal, table, badges, avatars, inputs, selects, checkbox, radio group, spinner,
empty state, callout, card, tabs, tooltip, drawer, breadcrumb, chat, progress, charts,
and theme support.

Import from the secondary entry point that the installed package exposes, for example:

```ts
import { TpiButtonComponent } from '@2026-p4-fe/ui/button';
import { TpiBadgeComponent } from '@2026-p4-fe/ui/badge';
```

Keep institutional-library imports after Angular and third-party imports, and before
application-core and feature-relative imports.

Use the supplied design tokens rather than hardcoded visual-system values. Preserve
Light/Dark theme behavior and do not introduce color, spacing, focus, loading, error,
or empty-state conventions that conflict with the UI Kit.

## What may be custom

Feature-specific composition and domain presentation may be implemented in the feature
`ui/` folder when the library has no equivalent. It must compose the official primitives
rather than recreate buttons, badges, spinners, modals, error feedback, or empty states.

The UI Kit is presentational only: do not place HTTP calls, backend DTOs, domain state,
or business rules in it or model feature code as though it did.

## Verification

Before concluding a UI change:

1. Check that every chosen import exists in the package version installed by the frontend.
2. Check the affected template at narrow and wide layouts and in both theme modes when
   the change uses color or surface styles.
3. Provide accessible names, keyboard reachability, visible focus, and feedback for
   loading, success, empty, and error states as applicable.
4. Run `project-quality-gate` and attach screenshots or a recording to the PR when the
   UI behavior changed.

If the requested primitive is absent from the installed UI Kit, report the missing API.
Do not silently create a parallel base component or modify the UI Kit dependency.
