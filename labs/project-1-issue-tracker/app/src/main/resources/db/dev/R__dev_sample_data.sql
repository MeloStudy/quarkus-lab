-- Dev mode only (location db/dev is added in the %dev profile): a small tracker to click around in Swagger UI.
-- Repeatable and idempotent: it runs again whenever this file changes, and never duplicates rows.

INSERT INTO project (code, name, next_number, created_at)
VALUES ('TRK', 'Tracker itself', 5, now())
ON CONFLICT (code) DO NOTHING;

INSERT INTO issue (project_id, number, title, description, type, priority, status, assignee, version, created_at, updated_at)
SELECT p.id, v.number, v.title, v.description, v.type, v.priority, v.status, v.assignee, 0, now(), now()
FROM project p,
     (VALUES (1, 'Login page returns 500', 'Happens when the password has a trailing space', 'BUG', 'CRITICAL', 'IN_PROGRESS', 'alice'),
             (2, 'Add dark mode', NULL, 'STORY', 'LOW', 'OPEN', NULL),
             (3, 'Upgrade the database driver', 'Patch release only', 'TASK', 'MEDIUM', 'RESOLVED', 'bob'),
             (4, 'Typo in the footer', NULL, 'BUG', 'LOW', 'CLOSED', 'alice')
     ) AS v(number, title, description, type, priority, status, assignee)
WHERE p.code = 'TRK'
ON CONFLICT (project_id, number) DO NOTHING;

INSERT INTO issue_label (issue_id, label)
SELECT i.id, l.label
FROM issue i JOIN project p ON p.id = i.project_id,
     (VALUES (1, 'security'), (1, 'auth'), (2, 'ui')) AS l(number, label)
WHERE p.code = 'TRK' AND i.number = l.number
ON CONFLICT DO NOTHING;
