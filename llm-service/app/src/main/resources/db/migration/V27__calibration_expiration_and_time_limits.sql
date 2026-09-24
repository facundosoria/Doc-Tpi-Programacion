ALTER TYPE calibration_state ADD VALUE 'EXPIRED';

ALTER TABLE calibration_runs ADD COLUMN expiration_reason VARCHAR(40);

ALTER TABLE institutional_calibration_profiles ADD COLUMN expiration_days INT;

CREATE TABLE course_calibration_profiles (
    course_id UUID PRIMARY KEY,
    expiration_days INT,
    configured_by_user_id UUID NOT NULL,
    configured_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
