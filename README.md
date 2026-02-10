# Knowlex

A document ingestion service built with Spring Boot that extracts text from documents, chunks it using a sliding window algorithm, and persists the results for downstream knowledge management use cases.

## Tech Stack

- **Java 17** / **Spring Boot 3.2.2**
- **Spring Data JPA** with H2 (default) or PostgreSQL
- **Apache PDFBox 2.0.30** for PDF text extraction
- **Lombok 1.18.42** for boilerplate reduction
- **Maven** build tool

## Architecture

```
PDF Upload → Text Extraction → Chunking → Persistence
     ↓              ↓              ↓            ↓
  Controller    PdfBox        Sliding       JPA/H2
  (REST API)   Extractor      Window       Repository
```

The processing pipeline follows a layered architecture with interfaces at each stage:

| Layer | Interface | Implementation | Status |
|-------|-----------|----------------|--------|
| API | `DocumentIngestionController` | REST endpoints | ✅ Implemented |
| Service | `DocumentIngestionService` | `DocumentIngestionServiceImpl` | ✅ Implemented |
| Extraction | `DocumentTextExtractor` | `PdfBoxDocumentExtractor` | ✅ Implemented |
| Chunking | `TextChunker` | `SlidingWindowChunker` | ✅ Implemented |
| Persistence | `DocumentRepository` | Spring Data JPA | ✅ Implemented |
| Persistence | `DocumentChunkRepository` | Spring Data JPA | ✅ Implemented |
| Preprocessing | `TextPreprocessor` | `DefaultTextPreprocessor` | 🔲 Skeleton |
| Storage | `ChunkStorage` | `FileSystemChunkStorage` | 🔲 Skeleton |

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/documents` | Upload a PDF for ingestion (multipart/form-data) |
| `GET` | `/api/v1/documents` | List all ingested documents |
| `GET` | `/api/v1/documents/{id}` | Get document metadata by ID |
| `GET` | `/api/v1/documents/{id}/chunks` | Get all text chunks for a document |
| `DELETE` | `/api/v1/documents/{id}` | Delete a document and its chunks |

### Example Usage

```bash
# Upload a PDF
curl -X POST http://localhost:8080/api/v1/documents -F "file=@document.pdf"

# List all documents
curl http://localhost:8080/api/v1/documents

# Get chunks for a document
curl http://localhost:8080/api/v1/documents/{document-id}/chunks

# Delete a document
curl -X DELETE http://localhost:8080/api/v1/documents/{document-id}
```

### Response Examples

**POST** `/api/v1/documents` (201 Created):
```json
{
  "documentId": "c768c61c-0a95-459b-9414-f5d0782b1bc6",
  "filename": "document.pdf",
  "totalChunks": 47,
  "status": "COMPLETED",
  "createdAt": "2026-02-10T03:27:31.533224Z"
}
```

**GET** `/api/v1/documents/{id}/chunks` (200 OK):
```json
[
  {
    "id": "e1f38814-c580-4edd-afd7-8fea41788004",
    "documentId": "c768c61c-0a95-459b-9414-f5d0782b1bc6",
    "chunkIndex": 0,
    "content": "First chunk of extracted text..."
  }
]
```

## Project Structure

```
src/main/java/com/symphony/docweave/
├── KnowlexApplication.java          # Entry point
├── api/                              # REST controllers
│   ├── DocumentIngestionController.java
│   ├── GlobalExceptionHandler.java
│   └── dto/                          # Request/Response DTOs
│       ├── IngestionResponse.java
│       ├── DocumentResponse.java
│       ├── ChunkResponse.java
│       └── ErrorResponse.java
├── service/                          # Business logic
│   ├── DocumentIngestionService.java (interface)
│   └── impl/
│       └── DocumentIngestionServiceImpl.java
├── domain/                           # Domain models & JPA entities
│   ├── Document.java
│   ├── DocumentEntity.java
│   ├── DocumentChunkEntity.java
│   ├── DocumentChunk.java
│   └── DocumentType.java
├── repository/                       # Spring Data JPA repositories
│   ├── DocumentRepository.java
│   └── DocumentChunkRepository.java
├── extractor/                        # Document text extraction
│   ├── DocumentTextExtractor.java (interface)
│   └── PdfBoxDocumentExtractor.java
├── chunker/                          # Text chunking algorithms
│   ├── TextChunker.java (interface)
│   └── SlidingWindowChunker.java
├── preprocessor/                     # Text preprocessing (skeleton)
├── storage/                          # Chunk persistence (skeleton)
├── config/                           # Spring configuration
│   └── IngestionProperties.java
└── exception/                        # Custom exceptions
    └── DocumentProcessingException.java
```

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+

### Build & Run

```bash
# Build the project
mvn clean install

# Run the application (uses H2 in-memory database by default)
mvn spring-boot:run

# Run with PostgreSQL
mvn spring-boot:run -Dspring-boot.run.profiles=postgres

# Run tests
mvn test

# Package as JAR
mvn clean package
java -jar target/Knowlex-1.0-SNAPSHOT.jar
```

The application starts on **http://localhost:8080**.

### Database

**Default (H2):** No setup required. In-memory database starts automatically.
- H2 Console: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:knowlexdb`
- Username: `sa` / Password: _(empty)_

**PostgreSQL profile:** Create the database, then run with the `postgres` profile.

```sql
CREATE DATABASE roms_db;
CREATE USER roms_user WITH PASSWORD 'roms_password';
GRANT ALL PRIVILEGES ON DATABASE roms_db TO roms_user;
```

## Configuration

Configuration is managed via `src/main/resources/application.yaml`:

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8080` | Server port |
| `ingestion.chunk-size` | `200` | Words per chunk |
| `ingestion.chunk-overlap` | `40` | Overlapping words between chunks |
| `spring.datasource.url` | `jdbc:h2:mem:knowlexdb` | Database URL |
| `spring.servlet.multipart.max-file-size` | `50MB` | Max upload size |

## Data Model

### Documents

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `source` | String | Content type of the uploaded file |
| `original_filename` | String | Original file name |
| `checksum` | String | SHA-256 checksum for deduplication |
| `created_at` | Instant | Creation timestamp |

### Document Chunks

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `document_id` | UUID | Foreign key → documents |
| `chunk_index` | int | Positional index within document |
| `content` | Text | Chunk text content |

## Key Features

- **PDF text extraction** via Apache PDFBox
- **Sliding window chunking** with configurable size and overlap
- **SHA-256 deduplication** — rejects duplicate file uploads
- **Structured error handling** — maps exceptions to proper HTTP status codes (400, 404, 409, 413, 422)
- **Multi-profile database** — H2 for development, PostgreSQL for production

## Roadmap

- [x] Implement REST API endpoints for document ingestion
- [x] Complete service layer orchestration
- [x] Configure environment profiles (H2 default, PostgreSQL opt-in)
- [x] Add JPA repositories with query methods
- [ ] Add text preprocessing (normalization, cleaning)
- [ ] Support additional document types (DOCX, HTML)
- [ ] Add comprehensive unit and integration tests
- [ ] Externalize database credentials
- [ ] Add Docker support
- [ ] Set up CI/CD pipeline
- [ ] Add OpenAPI/Swagger documentation

## License

This project is proprietary.
