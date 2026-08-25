ALTER TABLE recipients RENAME COLUMN name TO designation;

ALTER TABLE recipients ADD COLUMN code VARCHAR(30);

ALTER INDEX idx_recipients_name RENAME TO idx_recipients_designation;

CREATE UNIQUE INDEX uq_recipients_code ON recipients (code) WHERE code IS NOT NULL;
