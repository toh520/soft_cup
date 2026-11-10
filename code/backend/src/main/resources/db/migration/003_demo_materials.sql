-- 所有内容均为虚构演示资料。迁移只应用一次，已有同名账号或资料不覆盖。
INSERT INTO users(account,password_hash,password,name,role,class_id,age,sex,height_cm,weight_kg,activity_level,diet_preference,avoided_ingredients)
SELECT '20260001','','123456','张晨','student',1,22,'male',175,65,'medium','none','[]' WHERE NOT EXISTS (SELECT 1 FROM users WHERE account='20260001');
INSERT INTO users(account,password_hash,password,name,role,class_id,age,sex,height_cm,weight_kg,activity_level,diet_preference,avoided_ingredients)
SELECT '20260002','','123456','林悦','student',1,21,'female',165,55,'medium','vegetarian','["花生"]' WHERE NOT EXISTS (SELECT 1 FROM users WHERE account='20260002');
INSERT INTO users(account,password_hash,password,name,role,class_id)
SELECT '20260003','','123456','王宁','student',1 WHERE NOT EXISTS (SELECT 1 FROM users WHERE account='20260003');

INSERT INTO books(title,author,summary,available_count)
SELECT '算法设计与分析','刘青','【演示图书】分治、动态规划与贪心算法。',5 WHERE NOT EXISTS (SELECT 1 FROM books WHERE title='算法设计与分析' AND author='刘青');
INSERT INTO books(title,author,summary,available_count)
SELECT 'Vue 前端开发','许宁','【演示图书】Vue 组件、路由与状态管理。',3 WHERE NOT EXISTS (SELECT 1 FROM books WHERE title='Vue 前端开发' AND author='许宁');
INSERT INTO books(title,author,summary,available_count)
SELECT '人工智能导论','唐悦','【演示图书】搜索、机器学习与自然语言处理。',1 WHERE NOT EXISTS (SELECT 1 FROM books WHERE title='人工智能导论' AND author='唐悦');
INSERT INTO books(title,author,summary,available_count)
SELECT '软件工程实践','赵远','【演示图书】需求分析、设计、测试与项目协作。',0 WHERE NOT EXISTS (SELECT 1 FROM books WHERE title='软件工程实践' AND author='赵远');

INSERT INTO class_schedules(class_id,course_name,teacher,weekday,start_time,end_time,classroom)
SELECT 1,'算法设计与分析','刘老师',1,'14:00','15:35','教学楼 B301' WHERE NOT EXISTS (SELECT 1 FROM class_schedules WHERE class_id=1 AND weekday=1 AND start_time<'15:35' AND end_time>'14:00');
INSERT INTO class_schedules(class_id,course_name,teacher,weekday,start_time,end_time,classroom)
SELECT 1,'Web 前端开发','许老师',2,'14:00','15:35','实验楼 C202' WHERE NOT EXISTS (SELECT 1 FROM class_schedules WHERE class_id=1 AND weekday=2 AND start_time<'15:35' AND end_time>'14:00');
INSERT INTO class_schedules(class_id,course_name,teacher,weekday,start_time,end_time,classroom)
SELECT 1,'人工智能导论','唐老师',3,'14:00','15:35','教学楼 B302' WHERE NOT EXISTS (SELECT 1 FROM class_schedules WHERE class_id=1 AND weekday=3 AND start_time<'15:35' AND end_time>'14:00');
INSERT INTO class_schedules(class_id,course_name,teacher,weekday,start_time,end_time,classroom)
SELECT 1,'软件工程','赵老师',4,'14:00','15:35','教学楼 B303' WHERE NOT EXISTS (SELECT 1 FROM class_schedules WHERE class_id=1 AND weekday=4 AND start_time<'15:35' AND end_time>'14:00');
INSERT INTO class_schedules(class_id,course_name,teacher,weekday,start_time,end_time,classroom)
SELECT 1,'综合项目实践','陈老师',5,'14:00','15:35','实验楼 C201' WHERE NOT EXISTS (SELECT 1 FROM class_schedules WHERE class_id=1 AND weekday=5 AND start_time<'15:35' AND end_time>'14:00');

INSERT INTO dishes(name,category,portion_g,energy_kcal,protein_g,fat_g,carbohydrate_g,ingredient_tags,is_vegetarian,is_available)
SELECT '南瓜燕麦饭','staple',250,360,9,4,72,'["燕麦","南瓜"]',1,1 WHERE NOT EXISTS (SELECT 1 FROM dishes WHERE name='南瓜燕麦饭');
INSERT INTO dishes(name,category,portion_g,energy_kcal,protein_g,fat_g,carbohydrate_g,ingredient_tags,is_vegetarian,is_available)
SELECT '毛豆炒鸡蛋','protein',200,260,21,16,10,'["大豆","鸡蛋"]',1,1 WHERE NOT EXISTS (SELECT 1 FROM dishes WHERE name='毛豆炒鸡蛋');
INSERT INTO dishes(name,category,portion_g,energy_kcal,protein_g,fat_g,carbohydrate_g,ingredient_tags,is_vegetarian,is_available)
SELECT '清炒油麦菜','vegetable',200,110,4,6,10,'["油麦菜"]',1,1 WHERE NOT EXISTS (SELECT 1 FROM dishes WHERE name='清炒油麦菜');

-- pending 是明确的未向量化状态，空数组不是有效向量；不得参与 RAG 检索。
INSERT INTO knowledge_entries(title,category,content,embedding,embedding_model)
SELECT '演示规则：宿舍用电','rule','【课设演示规则】宿舍电器最大功率为300W。请勿使用大功率电器。','[]','pending' WHERE NOT EXISTS (SELECT 1 FROM knowledge_entries WHERE title='演示规则：宿舍用电');
INSERT INTO knowledge_entries(title,category,content,embedding,embedding_model)
SELECT '演示规则：图书借阅期限','rule','【课设演示规则】图书借阅期限为30天。','[]','pending' WHERE NOT EXISTS (SELECT 1 FROM knowledge_entries WHERE title='演示规则：图书借阅期限');
INSERT INTO knowledge_entries(title,category,content,embedding,embedding_model)
SELECT '演示规则：图书馆开放','rule','【课设演示规则】图书馆周一至周五08:00至22:00开放。','[]','pending' WHERE NOT EXISTS (SELECT 1 FROM knowledge_entries WHERE title='演示规则：图书馆开放');
INSERT INTO knowledge_entries(title,category,content,embedding,embedding_model)
SELECT '演示规则：自习室','rule','【课设演示规则】自习室内请保持安静，离开时带走个人物品。','[]','pending' WHERE NOT EXISTS (SELECT 1 FROM knowledge_entries WHERE title='演示规则：自习室');
INSERT INTO knowledge_entries(title,category,content,embedding,embedding_model)
SELECT '演示课程：Java','course','【课设演示资料】Java 程序设计介绍面向对象、集合、异常与文件操作。','[]','pending' WHERE NOT EXISTS (SELECT 1 FROM knowledge_entries WHERE title='演示课程：Java');
INSERT INTO knowledge_entries(title,category,content,embedding,embedding_model)
SELECT '演示课程：数据结构','course','【课设演示资料】数据结构介绍线性表、树、图和排序算法。','[]','pending' WHERE NOT EXISTS (SELECT 1 FROM knowledge_entries WHERE title='演示课程：数据结构');
INSERT INTO knowledge_entries(title,category,content,embedding,embedding_model)
SELECT '演示课程：数据库','course','【课设演示资料】数据库原理介绍关系模型、SQL、事务与索引。','[]','pending' WHERE NOT EXISTS (SELECT 1 FROM knowledge_entries WHERE title='演示课程：数据库');
INSERT INTO knowledge_entries(title,category,content,embedding,embedding_model)
SELECT '演示课程：计算机网络','course','【课设演示资料】计算机网络介绍网络分层、TCP/IP及应用层协议。','[]','pending' WHERE NOT EXISTS (SELECT 1 FROM knowledge_entries WHERE title='演示课程：计算机网络');
INSERT INTO knowledge_entries(title,category,content,embedding,embedding_model)
SELECT '演示活动：阅读分享','activity','【课设演示活动】阅读分享会于2026年11月6日15:00在图书馆二楼举行。','[]','pending' WHERE NOT EXISTS (SELECT 1 FROM knowledge_entries WHERE title='演示活动：阅读分享');
INSERT INTO knowledge_entries(title,category,content,embedding,embedding_model)
SELECT '演示活动：编程交流','activity','【课设演示活动】编程交流会于2026年11月10日19:00在教学楼A201举行。','[]','pending' WHERE NOT EXISTS (SELECT 1 FROM knowledge_entries WHERE title='演示活动：编程交流');
INSERT INTO knowledge_entries(title,category,content,embedding,embedding_model)
SELECT '演示指南：宿舍生活','other','【课设演示资料】宿舍生活中请维护公共卫生，尊重室友作息。本文没有功率限制信息。','[]','pending' WHERE NOT EXISTS (SELECT 1 FROM knowledge_entries WHERE title='演示指南：宿舍生活');
INSERT INTO knowledge_entries(title,category,content,embedding,embedding_model)
SELECT '演示指南：失物招领','other','【课设演示资料】拾到物品可交至学生服务中心失物招领处。','[]','pending' WHERE NOT EXISTS (SELECT 1 FROM knowledge_entries WHERE title='演示指南：失物招领');
