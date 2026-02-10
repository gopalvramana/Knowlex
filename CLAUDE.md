# CLAUDE.md - Knowlex Project Guide

## Project Overview

Knowlex is a document ingestion service built with Spring Boot. It extracts text from documents (currently PDFs), chunks the text using a sliding window algorithm, and stores the results for downstream knowledge management use cases.

**Package:** `com.symphony.docweave`
**Java:** 17
**Spring Boot:** 3.2.2
**Build Tool:** Maven

## Build & Run Commands

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Package as JAR
mvn clean package

# Run tests
mvn test
```

## Project Structure

```
src/main/java/com/symphony/docweave/
├── KnowlexApplication.java        # Main entry point
├── api/                            # REST controllers
│   └── DocumentIngestionController.java
├── service/                        # Business logic
│   ├── DocumentIngestionService.java (interface)
│   └── DocumentIngestionServiceImpl.java
├── domain/                         # Domain models & JPA entities
│   ├── Document.java               # DTO
│   ├── DocumentEntity.java         # JPA entity (table: documents)
│   ├── DocumentChunk.java          # Chunk representation
│   └── DocumentType.java           # Enum: PDF, DOCX, HTML
├── extractor/                      # Text extraction from documents
│   ├── DocumentTextExtractor.java  (interface)
│   └── PdfBoxDocumentExtractor.java
├── chunker/                        # Text chunking algorithms
│   ├── TextChunker.java            (interface)
│   └── SlidingWindowChunker.java
├── preprocessor/                   # Text preprocessing (skeleton)
├── storage/                        # Chunk persistence (skeleton)
├── repository/                     # Data access layer (skeleton)
├── config/                         # Spring configuration
│   └── IngestionProperties.java
└── exception/                      # Custom exceptions
    └── DocumentProcessingException.java
```

## Key Configuration

**File:** `src/main/resources/application.yaml`

- **Server port:** 8080
- **Database:** PostgreSQL at `localhost:5432/roms_db` (H2 also on classpath for testing)
- **Chunking defaults:** chunk-size=200, chunk-overlap=40
- **JPA:** Hibernate ddl-auto=update, SQL logging enabled

## Dependencies

- **Spring Boot Starters:** web, data-jpa, test
- **Apache PDFBox 2.0.30** - PDF text extraction
- **Commons Lang3** - String utilities
- **Lombok** - Boilerplate reduction (@Getter, @Setter, etc.)
- **H2** (runtime) - Embedded database
- **PostgreSQL** - Primary database driver

## Architecture Notes

- Processing pipeline: Extract text -> Preprocess -> Chunk -> Store
- Text extraction currently supports PDF only (via PDFBox)
- Chunking uses a sliding window algorithm (word-based splits)
- Many layers are scaffolded but not yet implemented (controller endpoints, services, repositories, preprocessing, storage)
- No tests exist yet
- No CI/CD or Docker configuration

## Code Conventions

- Interfaces defined for each processing layer (extractor, chunker, preprocessor, storage)
- Lombok annotations for boilerplate reduction on domain classes
- UUID-based entity IDs
- Configuration via `application.yaml` (not .properties)

## Known Issues

- Duplicate `spring-boot-starter-web` dependency in pom.xml
- Database credentials are hardcoded in application.yaml (no profiles for dev/test/prod)
