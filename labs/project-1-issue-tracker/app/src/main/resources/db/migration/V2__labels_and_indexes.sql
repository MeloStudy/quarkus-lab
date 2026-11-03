-- V2: labels, and the indexes the listing and the detail pages need.
-- A second migration on purpose: the schema evolves by adding files, never by editing V1 (Flyway would refuse:
-- the checksum of an applied migration must not change).

CREATE TABLE issue_label (
    issue_id BIGINT      NOT NULL REFERENCES issue,
    label    VARCHAR(20) NOT NULL CHECK (label ~ '^[a-z0-9-]{1,20}$'),
    PRIMARY KEY (issue_id, label)
);

-- the filters of GET /api/issues
CREATE INDEX issue_project_status_idx ON issue (project_id, status);
CREATE INDEX issue_assignee_idx       ON issue (assignee);
-- label search starts from the label, not from the issue
CREATE INDEX issue_label_label_idx    ON issue_label (label);
-- foreign keys are NOT indexed by PostgreSQL on their own side: the detail pages read by issue_id
CREATE INDEX comment_issue_idx        ON comment (issue_id, id);
CREATE INDEX issue_event_issue_idx    ON issue_event (issue_id, id);
