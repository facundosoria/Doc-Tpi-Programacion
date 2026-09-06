---
name: ui-ux-design-guide
description: >-
  Knowledge base with 180+ topics on UI/UX design, extracted from the SixArm
  UI/UX Design Guide book. Use this skill whenever the user asks about user
  interface or user experience concepts, usability, accessibility (WCAG, ARIA,
  screen readers), design systems, design thinking, user research (personas,
  journeys, focus groups, interviews), wireframes, mockups, prototypes,
  usability heuristics, cognitive load, internationalization/localization,
  UI/UX testing (A/B, accessibility, heatmaps), AI in UX, or asks for a design
  review, UX audit, or help preparing user research — even if they don't
  mention this guide explicitly. Also use it to define or explain any UX/design
  term, or when reviewing an interface for usability problems.
---

# UI/UX Design Guide

Reference knowledge base built from the book *UI/UX Design Guide* (SixArm,
edited by Joel Parker Henderson). One topic per file, in `references/`,
organized by chapter directory.

## How to use this skill

1. **Find the topic** in the index below (or `grep -ri "keyword" references/`
   when unsure of the name).
2. **Read only the file(s) you need** — each is a short standalone page.
   Never load the whole `references/` tree into context.
3. **Cite the topic name** when you use its content, so the user knows the
   source (e.g., "According to the guide's page on Cognitive Load…").
4. If the user's question spans several topics, read 2–4 related files and
   synthesize. Chapter `00-overview.md` files give quick orientation.

## Common workflows

**Define/explain a term** → look it up in the index, read that one file,
answer in your own words adapted to the user's context.

**Design review / UX audit** → read `product-design/jakobs-ten-usability-heuristics.md`,
`product-design/cognitive-load.md` + `-recommendations`, and
`product-design/usability-friction.md` + `-recommendations`. Use them as the
evaluation checklist against the user's interface or screenshots. Add
`affordance/dark-pattern.md` and `user-centered-design-ucd/accessibility.md`
when relevant.

**Accessibility check** → `user-centered-design-ucd/accessibility.md`,
`affordance/web-content-accessibility-guidelines-wcag.md`,
`affordance/aria-attributes.md`, `affordance/screen-reader.md`,
`affordance/alternative-text-attribute.md`, plus
`ui-ux-testing/accessibility-testing.md` and `ui-ux-testing/screen-reader-testing.md`.

**Prepare user research** → `soft-skills/how-to-interview-a-user.md`,
`soft-skills/how-to-run-a-focus-group.md`, plus `design-thinking/` topics
(personas, journeys, voice of the customer).

**Choose a method or artifact** (wireframe vs mockup, use case vs user story,
lo-fi vs hi-fi prototype) → read both topic files and compare.

## Topic index

Paths are relative to `references/`.

## What is this book? [what-is-this-book/]
- What is this book? (overview) -> what-is-this-book/00-overview.md
- Who is this for? -> what-is-this-book/who-is-this-for.md
- Why am I creating this? -> what-is-this-book/why-am-i-creating-this.md
- Are there more guides? -> what-is-this-book/are-there-more-guides.md

## UI/UX [ui-ux/]
- UI/UX (overview) -> ui-ux/00-overview.md
- User Interface (UI) -> ui-ux/user-interface-ui.md
- User Interface (UI) - benefits -> ui-ux/user-interface-ui-benefits.md
- User Experience (UX) -> ui-ux/user-experience-ux.md
- User Experience (UX) - benefits -> ui-ux/user-experience-ux-benefits.md
- Customer Experience (CX) -> ui-ux/customer-experience-cx.md
- Customer Experience (CX) - benefits -> ui-ux/customer-experience-cx-benefits.md
- Developer Experience (DX) -> ui-ux/developer-experience-dx.md
- Developer Experience (DX) - benefits -> ui-ux/developer-experience-dx-benefits.md

## User-centered design (UCD) [user-centered-design-ucd/]
- User-centered design (UCD) (overview) -> user-centered-design-ucd/00-overview.md
- Usability -> user-centered-design-ucd/usability.md
- Accessibility -> user-centered-design-ucd/accessibility.md
- Information architecture (IA) -> user-centered-design-ucd/information-architecture-ia.md

## Design thinking [design-thinking/]
- Design thinking (overview) -> design-thinking/00-overview.md
- Focus group -> design-thinking/focus-group.md
- Personas -> design-thinking/personas.md
- Journeys -> design-thinking/journeys.md
- Voice of the Customer (VoC) -> design-thinking/voice-of-the-customer-voc.md
- Subject Matter Expert (SME) -> design-thinking/subject-matter-expert-sme.md
- Design charrette -> design-thinking/design-charrette.md
- Mind map -> design-thinking/mind-map.md
- Decision tree -> design-thinking/decision-tree.md
- Gamification -> design-thinking/gamification.md

## Design management [design-management/]
- Design management (overview) -> design-management/00-overview.md
- Design system -> design-management/design-system.md
- Style guide -> design-management/style-guide.md
- Pattern library -> design-management/pattern-library.md
- UK Government Design Principles -> design-management/uk-government-design-principles.md
- Apple Human Interface Guidelines (HIG) -> design-management/apple-human-interface-guidelines-hig.md
- Google Material Design -> design-management/google-material-design.md

## Task analysis [task-analysis/]
- Task analysis (overview) -> task-analysis/00-overview.md
- Cognitive Task Analysis (CTA) -> task-analysis/cognitive-task-analysis-cta.md
- Hierarchical Task Analysis (HTA) -> task-analysis/hierarchical-task-analysis-hta.md
- Workflow Analysis -> task-analysis/workflow-analysis.md
- Critical Incident Technique (CIT) -> task-analysis/critical-incident-technique-cit.md
- Diary study -> task-analysis/diary-study.md
- Anticipatory design -> task-analysis/anticipatory-design.md

## Ideation [ideation/]
- Ideation (overview) -> ideation/00-overview.md
- Creative thinking techniques -> ideation/creative-thinking-techniques.md
- Brainstorming -> ideation/brainstorming.md
- Active listening -> ideation/active-listening.md
- Thinking Hats -> ideation/thinking-hats.md
- SCAMPER -> ideation/scamper.md
- Futurespective -> ideation/futurespective.md
- Storyboard -> ideation/storyboard.md
- Storyboard steps -> ideation/storyboard-steps.md
- Mental model -> ideation/mental-model.md
- Dendrogram -> ideation/dendrogram.md
- The map is not the territory -> ideation/the-map-is-not-the-territory.md
- Vision board -> ideation/vision-board.md
- Oblique Strategies -> ideation/oblique-strategies.md
- Abramović Method -> ideation/abramovi-method.md

## Product design [product-design/]
- Product design (overview) -> product-design/00-overview.md
- Design canvas -> product-design/design-canvas.md
- Mockups -> product-design/mockups.md
- Wireframes -> product-design/wireframes.md
- Use cases -> product-design/use-cases.md
- User stories -> product-design/user-stories.md
- Use cases and user stories -> product-design/use-cases-and-user-stories.md
- MoSCoW method -> product-design/moscow-method.md
- Low-fidelity prototype -> product-design/low-fidelity-prototype.md
- High-fidelity prototype -> product-design/high-fidelity-prototype.md
- WYSIWYG -> product-design/wysiwyg.md
- Kaizen (continuous improvement) -> product-design/kaizen-continuous-improvement.md
- Cognitive load -> product-design/cognitive-load.md
- Cognitive load - recommendations -> product-design/cognitive-load-recommendations.md
- Usability friction -> product-design/usability-friction.md
- Usability friction - recommendations -> product-design/usability-friction-recommendations.md
- Jakob’s Ten Usability Heuristics -> product-design/jakobs-ten-usability-heuristics.md

## Modeling diagrams [modeling-diagrams/]
- Modeling diagrams (overview) -> modeling-diagrams/00-overview.md
- Activity diagram -> modeling-diagrams/activity-diagram.md
- Sequence diagram -> modeling-diagrams/sequence-diagram.md
- Use case diagram -> modeling-diagrams/use-case-diagram.md
- State diagram -> modeling-diagrams/state-diagram.md
- Timing diagram -> modeling-diagrams/timing-diagram.md
- Cause-and-effect diagram -> modeling-diagrams/cause-and-effect-diagram.md
- Unified Modeling Language (UML) -> modeling-diagrams/unified-modeling-language-uml.md
- PlantUML -> modeling-diagrams/plantuml.md
- Mermaid.js -> modeling-diagrams/mermaidjs.md

## North Star [north-star/]
- North Star (overview) -> north-star/00-overview.md
- Big Hairy Audacious Goal (BHAG) -> north-star/big-hairy-audacious-goal-bhag.md
- Strategic Balanced Scorecard (SBS) -> north-star/strategic-balanced-scorecard-sbs.md
- Big design up front (BDUF) -> north-star/big-design-up-front-bduf.md
- Domain-Driven Design (DDD) -> north-star/domain-driven-design-ddd.md
- Behavior Driven Development (BDD) -> north-star/behavior-driven-development-bdd.md
- Test-driven development (TDD) -> north-star/test-driven-development-tdd.md

## Affordance [affordance/]
- Affordance (overview) -> affordance/00-overview.md
- Gibson’s affordance theory -> affordance/gibsons-affordance-theory.md
- Accordion UI -> affordance/accordion-ui.md
- Drawer UI -> affordance/drawer-ui.md
- Ribbon UI -> affordance/ribbon-ui.md
- Tree UI -> affordance/tree-ui.md
- Wizard UI -> affordance/wizard-ui.md
- Progress indicator -> affordance/progress-indicator.md
- Header and footer -> affordance/header-and-footer.md
- Site map -> affordance/site-map.md
- ARIA attributes -> affordance/aria-attributes.md
- Model-View-Controller (MVC) -> affordance/model-view-controller-mvc.md
- Paper cut bug -> affordance/paper-cut-bug.md
- Dark pattern -> affordance/dark-pattern.md
- Cross-cultural communication -> affordance/cross-cultural-communication.md
- Communication styles -> affordance/communication-styles.md
- Screen reader -> affordance/screen-reader.md
- Keyboard shortcut (a.k.a. hotkey) -> affordance/keyboard-shortcut-aka-hotkey.md
- Alternative text attribute -> affordance/alternative-text-attribute.md
- Web Content Accessibility Guidelines (WCAG) -> affordance/web-content-accessibility-guidelines-wcag.md
- UI for color blindness -> affordance/ui-for-color-blindness.md

## UI/UX implementation [ui-ux-implementation/]
- UI/UX implementation (overview) -> ui-ux-implementation/00-overview.md
- Typography -> ui-ux-implementation/typography.md
- Copywriting -> ui-ux-implementation/copywriting.md
- Microcopy -> ui-ux-implementation/microcopy.md
- Iconography -> ui-ux-implementation/iconography.md
- Grid system -> ui-ux-implementation/grid-system.md
- Mobile-first design -> ui-ux-implementation/mobile-first-design.md
- Reactive UI -> ui-ux-implementation/reactive-ui.md
- Low-code / no-code -> ui-ux-implementation/low-code-no-code.md
- Text-To-Speech (TTS) and Speech-To-Text (STT) -> ui-ux-implementation/text-to-speech-tts-and-speech-to-text-stt.md
- Progressive enhancement -> ui-ux-implementation/progressive-enhancement.md
- Graceful degradation -> ui-ux-implementation/graceful-degradation.md
- Data schema -> ui-ux-implementation/data-schema.md
- Object-Relational Mapper (ORM) -> ui-ux-implementation/object-relational-mapper-orm.md

## Internationalization and localization [internationalization-and-localization/]
- Internationalization and localization (overview) -> internationalization-and-localization/00-overview.md
- Internationalization and localization: steps -> internationalization-and-localization/internationalization-and-localization-steps.md
- Locale -> internationalization-and-localization/locale.md
- Locale codes -> internationalization-and-localization/locale-codes.md
- Bidirectional text (bidi) -> internationalization-and-localization/bidirectional-text-bidi.md

## UI/UX testing [ui-ux-testing/]
- UI/UX testing (overview) -> ui-ux-testing/00-overview.md
- Split testing -> ui-ux-testing/split-testing.md
- End-to-end testing -> ui-ux-testing/end-to-end-testing.md
- Acceptance testing -> ui-ux-testing/acceptance-testing.md
- Localization testing -> ui-ux-testing/localization-testing.md
- Accessibility testing -> ui-ux-testing/accessibility-testing.md
- Screen reader testing -> ui-ux-testing/screen-reader-testing.md
- Benchmark testing -> ui-ux-testing/benchmark-testing.md
- Shift-left testing -> ui-ux-testing/shift-left-testing.md
- Heatmap -> ui-ux-testing/heatmap.md

## Artificial Intelligence (AI) [artificial-intelligence-ai/]
- Artificial Intelligence (AI) (overview) -> artificial-intelligence-ai/00-overview.md
- AI UI/UX -> artificial-intelligence-ai/ai-ui-ux.md
- AI form fill -> artificial-intelligence-ai/ai-form-fill.md
- AI for product development -> artificial-intelligence-ai/ai-for-product-development.md
- AI content generators -> artificial-intelligence-ai/ai-content-generators.md
- AI image generation -> artificial-intelligence-ai/ai-image-generation.md
- AI internationalization/localization -> artificial-intelligence-ai/ai-internationalization-localization.md

## Books about UI/UX [books-about-ui-ux/]
- Books about UI/UX (overview) -> books-about-ui-ux/00-overview.md
- Universal Principles of Design by William Lidwell et al. -> books-about-ui-ux/universal-principles-of-design-by-william-lidwell-et-al.md
- The Design of Everyday Things by Donald Norman -> books-about-ui-ux/the-design-of-everyday-things-by-donald-norman.md
- Envisioning Information by Edward R. Tufte -> books-about-ui-ux/envisioning-information-by-edward-r-tufte.md
- Don’t Make Me Think by Steve Krug -> books-about-ui-ux/dont-make-me-think-by-steve-krug.md
- Forms that Work by Caroline Jarrett et al. -> books-about-ui-ux/forms-that-work-by-caroline-jarrett-et-al.md
- The Humane Interface by Jef Raskin -> books-about-ui-ux/the-humane-interface-by-jef-raskin.md

## UI/UX quotations [ui-ux-quotations/]
- UI/UX quotations (overview) -> ui-ux-quotations/00-overview.md
- Learn early, learn often -> ui-ux-quotations/learn-early-learn-often.md
- Make mistakes faster -> ui-ux-quotations/make-mistakes-faster.md
- Perfect is the enemy of good -> ui-ux-quotations/perfect-is-the-enemy-of-good.md
- Data beats emotions -> ui-ux-quotations/data-beats-emotions.md

## Idioms [idioms/]
- Idioms (overview) -> idioms/00-overview.md
- Quick wins -> idioms/quick-wins.md
- Think outside of the box -> idioms/think-outside-of-the-box.md
- Out of scope -> idioms/out-of-scope.md
- Over the horizon -> idioms/over-the-horizon.md

## Aphorisms [aphorisms/]
- Aphorisms (overview) -> aphorisms/00-overview.md
- The Law of Demos -> aphorisms/the-law-of-demos.md
- The Law of Conservation of Complexity -> aphorisms/the-law-of-conservation-of-complexity.md
- The Pareto Principle (The 80/20 Rule) -> aphorisms/the-pareto-principle-the-80-20-rule.md
- Chesterton’s fence -> aphorisms/chestertons-fence.md

## Soft skills [soft-skills/]
- Soft skills (overview) -> soft-skills/00-overview.md
- How to create a UI/UX portfolio -> soft-skills/how-to-create-a-ui-ux-portfolio.md
- How to sketch a user interface -> soft-skills/how-to-sketch-a-user-interface.md
- How to run a focus group -> soft-skills/how-to-run-a-focus-group.md
- How to give a demo -> soft-skills/how-to-give-a-demo.md
- How to interview a user -> soft-skills/how-to-interview-a-user.md
- How to collaborate -> soft-skills/how-to-collaborate.md
- How to lead a meeting -> soft-skills/how-to-lead-a-meeting.md
- How to work with stakeholders -> soft-skills/how-to-work-with-stakeholders.md
- How to get feedback -> soft-skills/how-to-get-feedback.md
- How to give feedback -> soft-skills/how-to-give-feedback.md

## Conclusion [conclusion/]
- Conclusion (overview) -> conclusion/00-overview.md
- Thanks -> conclusion/thanks.md
- About the editor -> conclusion/about-the-editor.md
- About the AI -> conclusion/about-the-ai.md
- About the ebook PDF -> conclusion/about-the-ebook-pdf.md
- About related projects -> conclusion/about-related-projects.md


## Notes

- Content is a concise glossary-style summary, not a substitute for primary
  sources (WCAG spec, Apple HIG, Material Design docs). For authoritative or
  current details of external standards, verify with the official source.
- Source book: https://github.com/SixArm/ui-ux-design-guide
