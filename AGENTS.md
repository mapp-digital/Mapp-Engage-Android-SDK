## Skills

A skill is a set of local instructions to follow that is stored in a `SKILL.md` file.

### Available skills

- skill-creator: Guide for creating effective skills. Use when users want to create or update a skill that extends Codex capabilities. (file: /Users/semsudin.tafilovic/.codex/skills/.system/skill-creator/SKILL.md)
- skill-installer: Install Codex skills into `$CODEX_HOME/skills` from curated lists or GitHub repo paths. (file: /Users/semsudin.tafilovic/.codex/skills/.system/skill-installer/SKILL.md)
- mapp-android-client-integration: Provide implementation help and best practices for integrating Mapp Engage SDK into client Android apps, with Kotlin-first implementation and Java guidance for legacy apps. (file: /Users/semsudin.tafilovic/StudioProjects/mapp-engage-android-v7/skills/mapp-android-client-integration/SKILL.md)

### How to use skills

- Trigger rules: If the user names a skill (with `$SkillName` or plain text) OR the task clearly matches a skill description above, use that skill for that turn.
- Multiple mentions: Use the minimal set of skills needed and state the order.
- Missing/blocked: If a named skill cannot be read, say so briefly and continue with fallback.
- Progressive disclosure:
  1. Open `SKILL.md` and read only what is needed.
  2. Resolve relative paths from the skill directory first.
  3. Load only specific reference files needed for the task.
  4. Prefer bundled scripts/assets when present.
