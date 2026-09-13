-- A rubric title is part of the immutable version, not of its versioning family.
ALTER TABLE llm.rubric_version_v2 ADD COLUMN name VARCHAR(160);

-- The backfill is a one-time schema migration. Re-enable immutability before commit.
ALTER TABLE llm.rubric_version_v2 DISABLE TRIGGER rubric_version_v2_immutable;

UPDATE llm.rubric_version_v2 version
SET name = family.name
FROM llm.rubric_families family
WHERE family.id = version.family_id;

ALTER TABLE llm.rubric_version_v2 ENABLE TRIGGER rubric_version_v2_immutable;

ALTER TABLE llm.rubric_version_v2 ALTER COLUMN name SET NOT NULL;

-- Different course rubrics can originate from the same institutional template.
DROP INDEX llm.rubric_families_course_name_unique;

-- Kept only as legacy family metadata. New application writes use rubric_version_v2.name.
ALTER TABLE llm.rubric_families ALTER COLUMN name DROP NOT NULL;
