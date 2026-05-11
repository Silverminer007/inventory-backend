ALTER TABLE categories ADD COLUMN hue INT;
UPDATE categories SET hue = 210 WHERE short_code = 'XX';
ALTER TABLE categories ALTER COLUMN hue SET NOT NULL;

-- Patch the seeded system command payload to include hue
UPDATE commands
SET payload = payload || jsonb_build_object('hue', c.hue)
FROM categories c
WHERE commands.command_type = 'CATEGORY_CREATE'
  AND commands.user_id = 'system'
  AND (commands.payload->>'id')::uuid = c.id;
