# CampusDocAI

校园制度文档智能问答后端（Spring Boot 3 + MyBatis + Redis Stack Vector + JWT）。

## 环境要求

- JDK 17+
- MySQL 8.4+（库名 `campus_doc_assistant`）
- Redis Stack（向量检索）
- Maven 3.9+

## 本地启动

1. 复制环境变量模板并填写敏感配置（勿提交 `.env`）：
   ```bash
   cp .env.example .env
   ```
   在 `.env` 中设置：
   - `DB_PASSWORD` — MySQL 密码
   - `JWT_SECRET` — JWT 密钥（至少 32 字符）
   - `AI_API_KEY` — 通义 / DashScope API Key

   上述变量由 `src/main/resources/application.yml` 通过 `${DB_PASSWORD}`、`${JWT_SECRET}`、`${AI_API_KEY}` 读取。Spring Boot **不会**自动加载 `.env`，需在启动前注入环境变量（见下一步）。

2. 创建数据库：
   ```sql
   CREATE DATABASE campus_doc_assistant CHARACTER SET utf8mb4;
   ```

3. 启动 Redis Stack 与 MySQL 后，在项目根目录执行：
   ```bash
   set -a && source .env && set +a
   mvn spring-boot:run
   ```

4. 默认地址：`http://localhost:8080`  
   预置账号：`admin` / `123456`（首次启动自动创建）。

### IntelliJ / Cursor 运行

在 Run Configuration 的 **Environment variables** 中配置与 `.env` 相同的三个变量，或安装 EnvFile 类插件并指向 `.env`，然后直接运行 `CampusDocApplication`（无需 `spring.profiles.active=local`）。

## API 文档

- Apifox 说明：`docs/apifox接口文档.md`
- OpenAPI 导入：`docs/openapi.yaml`

## 测试

```bash
mvn test
```

集成测试使用 Testcontainers（MySQL + Redis Stack），需本机 Docker。
