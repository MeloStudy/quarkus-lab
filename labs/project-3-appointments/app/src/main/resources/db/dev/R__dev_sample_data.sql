-- Sample data for DEV MODE ONLY (%dev.quarkus.flyway.locations adds this folder; production never sees it).
-- Repeatable: it runs again whenever this file changes, so every statement checks that it has nothing to do.

INSERT INTO practitioner (name, created_at)
SELECT v.name, now() FROM (VALUES ('Dr. Ana Ruiz'), ('Dr. Ben Soto')) AS v(name)
WHERE NOT EXISTS (SELECT 1 FROM practitioner);

INSERT INTO treatment (name, duration_minutes)
SELECT v.name, v.minutes FROM (VALUES ('Checkup', 30), ('Therapy session', 60)) AS v(name, minutes)
WHERE NOT EXISTS (SELECT 1 FROM treatment);

INSERT INTO patient (full_name, email, created_at)
SELECT v.full_name, v.email, now() FROM (VALUES ('Bea Mora', 'bea@example.com'), ('Carl Vega', 'carl@example.com')) AS v(full_name, email)
WHERE NOT EXISTS (SELECT 1 FROM patient);

-- Monday to Friday, 09:00-13:00 and 15:00-18:00, for everybody
INSERT INTO working_hours (practitioner_id, weekday, start_time, end_time)
SELECT p.id, d.weekday, b.start_time, b.end_time
FROM practitioner p
CROSS JOIN generate_series(1, 5) AS d(weekday)
CROSS JOIN (VALUES (TIME '09:00', TIME '13:00'), (TIME '15:00', TIME '18:00')) AS b(start_time, end_time)
WHERE NOT EXISTS (SELECT 1 FROM working_hours);
