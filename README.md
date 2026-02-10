# Knowlex

A document ingestion service built with Spring Boot that extracts text from documents, chunks it using a sliding window algorithm, and persists the results for downstream knowledge management use cases.

## Tech Stack

- **Java 17** / **Spring Boot 3.2.2**
- **Spring Data JPA** with PostgreSQL (H2 for local/test)
- **Apache PDFBox 2.0.30** for PDF text extraction
- **Lombok** for boilerplate reduction
- **Maven** build tool

## Architecture

```
Document Upload → Text Extraction → Preprocessing → Chunking → Storage
```

The processing pipeline follows a layered architecture with interfaces at each stage:

| Layer | Interface | Implementation | Status |
|-------|-----------|----------------|--------|
| Extraction | `DocumentTextExtractor` | `PdfBoxDocumentExtractor` | ✅ Implemented |
| Preprocessing | `TextPreprocessor` | `DefaultTextPreprocessor` | 🔲 Skeleton |
| Chunking | `TextChunker` | `SlidingWindowChunker` | ✅ Implemented |
| Storage | `ChunkStorage` | `FileSystemChunkStorage` | 🔲 Skeleton |
| API | `DocumentIngestionController` | — | 🔲 Skeleton |
| Service | `DocumentIngestionService` | `DocumentIngestionServiceImpl` | 🔲 Skeleton |

## Project Structure

```
src/main/java/com/symphony/docweave/
├── KnowlexApplication.java          # Entry point
├── api/                              # REST controllers
├── service/                          # Business logic
├── domain/                           # Domain models & JPA entities
├── repository/                       # Spring Data JPA repositories
├── extractor/                        # Document text extraction
├── chunker/                          # Text chunking algorithms
├── preprocessor/                     # Text preprocessing
├── storage/                          # Chunk persistence
├── config/                           # Spring configuration
└── exception/                        # Custom exceptions
```

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL (or use H2 for local development)

### Database Setup

Create a PostgreSQL database:

```sql
CREATE DATABASE roms_db;
CREATE USER roms_user WITH PASSWORD 'roms_password';
GRANT ALL PRIVILEGES ON DATABASE roms_db TO roms_user;
```

### Build & Run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

# Run tests
mvn test

# Package as JAR
mvn clean package
java -jar target/Knowlex-1.0-SNAPSHOT.jar
```

The application starts on **http://localhost:8080**.

## Configuration

Configuration is managed via `src/main/resources/application.yaml`:

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8080` | Server port |
| `ingestion.chunk-size` | `200` | Words per chunk |
| `ingestion.chunk-overlap` | `40` | Overlapping words between chunks |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/roms_db` | Database URL |
| `spring.jpa.hibernate.ddl-auto` | `update` | Schema generation strategy |

## Data Model

### Documents

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `source` | String | Document source path |
| `original_filename` | String | Original file name |
| `checksum` | String | Deduplication checksum |
| `created_at` | Instant | Creation timestamp |

### Document Chunks

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `document_id` | UUID | Foreign key → documents |
| `chunk_index` | int | Positional index within document |
| `content` | Text | Chunk text content |

## Roadmap

- [ ] Implement REST API endpoints for document ingestion
- [ ] Complete service layer orchestration
- [ ] Add text preprocessing (normalization, cleaning)
- [ ] Support additional document types (DOCX, HTML)
- [ ] Add comprehensive unit and integration tests
- [ ] Configure environment profiles (dev, test, prod)
- [ ] Externalize database credentials
- [ ] Add Docker support
- [ ] Set up CI/CD pipeline
- [ ] Add OpenAPI/Swagger documentation

## License

This project is proprietary.
