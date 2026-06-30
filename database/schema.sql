CREATE TABLE IF NOT EXISTS tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date_created TEXT NOT NULL DEFAULT (datetime('now')),
    person TEXT NOT NULL,
    task_description TEXT NOT NULL,
    deadline TEXT,
    completed INTEGER NOT NULL DEFAULT 0,
    CHECK (completed IN (0, 1))
);

CREATE INDEX IF NOT EXISTS idx_tasks_completed ON tasks(completed);
CREATE INDEX IF NOT EXISTS idx_tasks_deadline ON tasks(deadline);
