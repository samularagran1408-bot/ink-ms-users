USE user_ms;

ALTER TABLE user_profile
    ADD COLUMN quiz_disciplines VARCHAR(500) NULL,
    ADD COLUMN trainer_quiz_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN organizer_quiz_attempts INT NOT NULL DEFAULT 0;
