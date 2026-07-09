-- Core task data. Optional features can be added through later schema changes.
CREATE TABLE IF NOT EXISTS tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date_created TEXT NOT NULL DEFAULT (datetime('now')),
    person TEXT NOT NULL,
    task_description TEXT NOT NULL,
    deadline TEXT,
    completed INTEGER NOT NULL DEFAULT 0,
    CHECK (completed IN (0, 1))
);

-- These indexes support the completed-task and nearest-deadline queries.
CREATE INDEX IF NOT EXISTS idx_tasks_completed ON tasks(completed);
CREATE INDEX IF NOT EXISTS idx_tasks_deadline ON tasks(deadline);
