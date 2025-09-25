# 📚 BookHoard API – Documentación

## 📂 Estructura de la base de datos

### Tabla `users`
- `id` (PK, int, autoincrement)  
- `username` (string, único)  
- `email` (string, único)  
- `password` (string, hash)  
- `created_at` (timestamp)  
- `updated_at` (timestamp)  

### Tabla `books`
- `id` (PK, int, autoincrement)  
- `title` (string)  
- `created_at` (timestamp)  
- `updated_at` (timestamp)  

### Tabla `authors`
- `id` (PK, int, autoincrement)  
- `name` (string)  
- `created_at` (timestamp)  
- `updated_at` (timestamp)  

### Tabla `user_books`
- `id` (PK, int, autoincrement)  
- `user_id` (FK → users.id)  
- `book_id` (FK → books.id)  
- `status` (string, opcional: leído / pendiente / etc.)  
- `wishlist` (boolean, opcional)  
- `created_at` (timestamp)  
- `updated_at` (timestamp)  

---

## 🌐 Endpoints REST

Todos los endpoints devuelven JSON.  
Autenticación con `Authorization: Bearer <token>` cuando sea requerido.  

### 🔐 Auth
- `POST /auth/register`
  ```json
  { "username": "alice", "email": "alice@mail.com", "password": "1234" }
  ```
  → devuelve usuario creado.

- `POST /auth/login`
  ```json
  { "username": "alice", "password": "1234" }
  ```
  → devuelve `{ "token": "...jwt..." }`

- `GET /auth/me` → perfil autenticado.
- `POST /auth/logout` → confirma logout.

### 📖 Books
- `GET /books` → lista de libros.
  ```json
  [ { "id":1, "title":"Libro A" } ]
  ```
- `GET /books/{id}` → un libro.
- `POST /books`
  ```json
  { "title": "Nuevo Libro" }
  ```
- `PUT /books/{id}`
  ```json
  { "title": "Título actualizado" }
  ```
- `DELETE /books/{id}` → `{ "message": "Book deleted" }`

### ✍️ Authors
- `GET /authors` → lista de autores.
  ```json
  [ { "id":1, "name":"Autor X" } ]
  ```
- `POST /authors`
  ```json
  { "name": "Nuevo Autor" }
  ```
- `PUT /authors/{id}`
  ```json
  { "name": "Nombre actualizado" }
  ```

### 👤 Users
- `GET /users` → lista de usuarios.
  ```json
  [ { "id":1, "username":"alice", "email":"alice@mail.com" } ]
  ```
- `POST /users`
  ```json
  { "username": "bob", "email": "bob@mail.com", "password": "abcd" }
  ```
- `PUT /users/{id}`
  ```json
  { "username": "bob_new" }
  ```

### 📘 UserBooks
- `GET /user_books` → lista de relaciones.
  ```json
  [ { "id":1, "user_id":1, "book_id":2, "status":"leído", "wishlist":false } ]
  ```
- `POST /user_books`
  ```json
  { "user_id": 1, "book_id": 2, "status": "pendiente", "wishlist": true }
  ```
- `PUT /user_books/{id}`
  ```json
  { "status": "leído", "wishlist": false }
  ```
- `DELETE /user_books/{id}` → `{ "message": "UserBook deleted" }`

### 🩺 System
- `GET /health`
  ```json
  { "status": "healthy", "php_version": "8.x.x" }
  ```