CREATE TABLE classes (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE);
CREATE TABLE users (
 id INTEGER PRIMARY KEY AUTOINCREMENT, account TEXT NOT NULL UNIQUE, password_hash TEXT NOT NULL,
 name TEXT NOT NULL, role TEXT NOT NULL CHECK(role IN ('admin','student')), class_id INTEGER REFERENCES classes(id),
 age INTEGER CHECK(age BETWEEN 18 AND 80), sex TEXT CHECK(sex IN ('male','female')),
 height_cm REAL CHECK(height_cm BETWEEN 100 AND 250), weight_kg REAL CHECK(weight_kg BETWEEN 25 AND 250),
 activity_level TEXT CHECK(activity_level IN ('low','medium','high')),
 diet_preference TEXT NOT NULL DEFAULT 'none' CHECK(diet_preference IN ('none','vegetarian')),
 avoided_ingredients TEXT NOT NULL DEFAULT '[]'
);
CREATE TABLE class_schedules (id INTEGER PRIMARY KEY AUTOINCREMENT, class_id INTEGER NOT NULL REFERENCES classes(id), course_name TEXT NOT NULL, teacher TEXT NOT NULL, weekday INTEGER NOT NULL CHECK(weekday BETWEEN 1 AND 7), start_time TEXT NOT NULL, end_time TEXT NOT NULL CHECK(end_time > start_time), classroom TEXT NOT NULL);
CREATE TABLE leave_requests (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL REFERENCES users(id), start_at TEXT NOT NULL, end_at TEXT NOT NULL CHECK(end_at > start_at), reason TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'pending' CHECK(status IN ('pending','approved','rejected')));
CREATE TABLE books (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, author TEXT NOT NULL, summary TEXT NOT NULL, available_count INTEGER NOT NULL CHECK(available_count >= 0));
CREATE TABLE borrow_records (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL REFERENCES users(id), book_id INTEGER NOT NULL REFERENCES books(id), borrowed_at TEXT NOT NULL, returned_at TEXT);
-- 同书同时只能借一本；归还后允许再次借阅。
CREATE UNIQUE INDEX unique_active_borrow ON borrow_records(user_id, book_id) WHERE returned_at IS NULL;
CREATE TABLE dishes (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, category TEXT NOT NULL CHECK(category IN ('staple','protein','vegetable')), portion_g REAL NOT NULL CHECK(portion_g > 0), energy_kcal REAL NOT NULL CHECK(energy_kcal > 0), protein_g REAL NOT NULL CHECK(protein_g >= 0), fat_g REAL NOT NULL CHECK(fat_g >= 0), carbohydrate_g REAL NOT NULL CHECK(carbohydrate_g >= 0), ingredient_tags TEXT NOT NULL DEFAULT '[]', is_vegetarian INTEGER NOT NULL CHECK(is_vegetarian IN (0,1)), is_available INTEGER NOT NULL CHECK(is_available IN (0,1)));
CREATE TABLE orders (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL REFERENCES users(id), ordered_at TEXT NOT NULL);
CREATE TABLE order_items (id INTEGER PRIMARY KEY AUTOINCREMENT, order_id INTEGER NOT NULL REFERENCES orders(id), dish_id INTEGER NOT NULL REFERENCES dishes(id), dish_name TEXT NOT NULL, portion_g REAL NOT NULL, energy_kcal REAL NOT NULL, protein_g REAL NOT NULL, fat_g REAL NOT NULL, carbohydrate_g REAL NOT NULL, UNIQUE(order_id, dish_id));
CREATE TABLE knowledge_entries (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, category TEXT NOT NULL CHECK(category IN ('rule','course','activity','other')), content TEXT NOT NULL, embedding TEXT NOT NULL, embedding_model TEXT NOT NULL);
CREATE INDEX users_class ON users(class_id);
CREATE INDEX schedules_class_weekday ON class_schedules(class_id,weekday,start_time);
CREATE INDEX leave_user ON leave_requests(user_id);
CREATE INDEX leave_status ON leave_requests(status);
CREATE INDEX borrow_user_time ON borrow_records(user_id,borrowed_at);
CREATE INDEX borrow_book ON borrow_records(book_id);
CREATE INDEX orders_user_time ON orders(user_id,ordered_at);
CREATE INDEX items_dish ON order_items(dish_id);
