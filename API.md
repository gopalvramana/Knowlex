# Knowlex API Reference

Base URL: `http://localhost:8080`
All request and response bodies are `application/json` unless noted otherwise.

---

## Table of Contents

1. [Document Ingestion](#1-document-ingestion)
2. [Embeddings](#2-embeddings)
3. [RAG (Retrieval-Augmented Generation)](#3-rag-retrieval-augmented-generation)
4. [Error Responses](#4-error-responses)
5. [End-to-End Workflow](#5-end-to-end-workflow)

---

## 1. Document Ingestion

### `POST /api/v1/documents`
Upload and ingest a document (PDF). The service extracts text, splits it into chunks, and persists both to the database.

**Content-Type:** `multipart/form-data`

| Form field | Type | Required | Description |
|---|---|---|---|
| `file` | File | Yes | PDF file to ingest (max 50 MB) |

**Response `201 Created`**
```json
{
  "documentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "filename": "system-design.pdf",
  "totalChunks": 42,
  "status": "INGESTED",
  "createdAt": "2024-03-15T10:30:00Z"
}
```

**Example**
```bash
curl -X POST http://localhost:8080/api/v1/documents \
  -F "file=@/path/to/document.pdf"
```

**Errors**

| Status | Reason |
|---|---|
| `400` | File is empty |
| `409` | Document already ingested (duplicate checksum) |
| `413` | File exceeds 50 MB limit |
| `422` | Text extraction failed |

---

### `GET /api/v1/documents`
List all ingested documents.

**Response `200 OK`**
```json
[
  {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "source": "PDF",
    "originalFilename": "system-design.pdf",
    "checksum": "d41d8cd98f00b204e9800998ecf8427e",
    "createdAt": "2024-03-15T10:30:00Z"
  }
]
```

**Example**
```bash
curl http://localhost:8080/api/v1/documents
```

---

### `GET /api/v1/documents/{documentId}`
Fetch metadata for a single document.

| Path param | Type | Description |
|---|---|---|
| `documentId` | UUID | Document identifier |

**Response `200 OK`**
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "source": "PDF",
  "originalFilename": "system-design.pdf",
  "checksum": "d41d8cd98f00b204e9800998ecf8427e",
  "createdAt": "2024-03-15T10:30:00Z"
}
```

**Example**
```bash
curl http://localhost:8080/api/v1/documents/a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

**Errors**

| Status | Reason |
|---|---|
| `404` | Document not found |

---

### `GET /api/v1/documents/{documentId}/chunks`
Retrieve all text chunks for a document, ordered by position.

| Path param | Type | Description |
|---|---|---|
| `documentId` | UUID | Document identifier |

**Response `200 OK`**
```json
[
  {
    "id": "c3d4e5f6-a1b2-7890-abcd-ef1234567890",
    "documentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "chunkIndex": 0,
    "content": "System design is the process of defining..."
  },
  {
    "id": "d4e5f6a1-b2c3-7890-abcd-ef1234567890",
    "documentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "chunkIndex": 1,
    "content": "Scalability refers to the ability of a system..."
  }
]
```

**Example**
```bash
curl http://localhost:8080/api/v1/documents/a1b2c3d4-e5f6-7890-abcd-ef1234567890/chunks
```

---

### `DELETE /api/v1/documents/{documentId}`
Delete a document and all its associated chunks.

| Path param | Type | Description |
|---|---|---|
| `documentId` | UUID | Document identifier |

**Response `204 No Content`**

**Example**
```bash
curl -X DELETE http://localhost:8080/api/v1/documents/a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

**Errors**

| Status | Reason |
|---|---|
| `404` | Document not found |

---

## 2. Embeddings

### `POST /api/v1/embeddings/generate`
Generate and store vector embeddings for **all** chunks across all documents that do not yet have an embedding. Processes in parallel batches.

**Request body:** none

**Response `200 OK`**
```
Embedded 42 chunk(s)
```

**Example**
```bash
curl -X POST http://localhost:8080/api/v1/embeddings/generate
```

> **Note:** Already-embedded chunks are skipped automatically. Safe to call multiple times.

---

### `POST /api/v1/embeddings/generate/{documentId}`
Generate and store embeddings for all un-embedded chunks of a **specific** document.

| Path param | Type | Description |
|---|---|---|
| `documentId` | UUID | Document identifier |

**Response `200 OK`**
```
Embedded 12 chunk(s) for document a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

**Example**
```bash
curl -X POST http://localhost:8080/api/v1/embeddings/generate/a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

**Errors**

| Status | Reason |
|---|---|
| `404` | Document not found / has no chunks |

---

### `POST /api/v1/embeddings/search`
Perform a semantic similarity search. Embeds the query text and returns the top-K most similar chunks from the database.

| Query param | Type | Default | Description |
|---|---|---|---|
| `k` | int | `5` | Number of results to return (max 50) |

**Request body:** plain text query string

**Response `200 OK`**
```json
[
  {
    "chunkId": "c3d4e5f6-a1b2-7890-abcd-ef1234567890",
    "documentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "chunkIndex": 3,
    "content": "Load balancers distribute incoming traffic across...",
    "score": 0.08731204
  },
  {
    "chunkId": "d4e5f6a1-b2c3-7890-abcd-ef1234567890",
    "documentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "chunkIndex": 7,
    "content": "Horizontal scaling adds more machines to the pool...",
    "score": 0.12045680
  }
]
```

> **`score`** is the cosine distance (lower = more similar). `0.0` means identical.

**Example**
```bash
curl -X POST "http://localhost:8080/api/v1/embeddings/search?k=3" \
  -H "Content-Type: text/plain" \
  -d "How does load balancing work?"
```

---

## 3. RAG (Retrieval-Augmented Generation)

### `POST /api/v1/rag/ask`
The full RAG pipeline. Accepts a natural language question, retrieves the most relevant chunks from the knowledge base, and returns an LLM-generated answer grounded in those chunks.

**Pipeline:**
```
query → embed → vector search → build context → LLM → answer
```

**Request body:**
```json
{
  "query": "How does consistent hashing work?",
  "topK": 5
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `query` | string | Yes | The question to answer |
| `topK` | int | No | Chunks to retrieve for context. Defaults to `rag.default-top-k` (5) |

**Response `200 OK`**
```json
{
  "query": "How does consistent hashing work?",
  "answer": "Consistent hashing is a technique used to distribute data across nodes in a way that minimises reorganisation when nodes are added or removed. Each node and key is assigned a position on a virtual ring...",
  "sourceChunks": [
    {
      "chunkId": "c3d4e5f6-a1b2-7890-abcd-ef1234567890",
      "documentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "chunkIndex": 11,
      "content": "Consistent hashing places both nodes and keys on a ring...",
      "score": 0.06123450
    },
    {
      "chunkId": "e5f6a1b2-c3d4-7890-abcd-ef1234567890",
      "documentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "chunkIndex": 12,
      "content": "Virtual nodes (vnodes) improve load distribution by...",
      "score": 0.09841200
    }
  ]
}
```

| Response field | Type | Description |
|---|---|---|
| `query` | string | The original question |
| `answer` | string | LLM-generated answer based solely on retrieved context |
| `sourceChunks` | array | Chunks used as context, ranked by similarity (closest first) |

**Example**
```bash
curl -X POST http://localhost:8080/api/v1/rag/ask \
  -H "Content-Type: application/json" \
  -d '{"query": "How does consistent hashing work?", "topK": 5}'
```

**Errors**

| Status | Reason |
|---|---|
| `400` | Query is blank or request body is null |
| `500` | OpenAI API call failed after retries |

> **No chunks found:** If no relevant chunks exist (e.g. no documents ingested or embeddings not yet generated), the API returns `200` with `"answer": "I don't have enough information to answer that."` and an empty `sourceChunks` array.

---

## 4. Error Responses

All errors follow a consistent structure:

```json
{
  "status": 404,
  "message": "Document not found: a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "timestamp": "2024-03-15T10:35:00Z"
}
```

| HTTP Status | Meaning |
|---|---|
| `400 Bad Request` | Invalid input (blank query, empty file, bad UUID) |
| `404 Not Found` | Resource does not exist |
| `409 Conflict` | Document already ingested |
| `413 Payload Too Large` | File exceeds 50 MB |
| `422 Unprocessable Entity` | Document could not be processed (corrupt PDF etc.) |
| `500 Internal Server Error` | Unexpected server error |

---

## 5. End-to-End Workflow

Follow these steps to go from a raw document to an answered question:

```bash
# 1. Ingest a PDF
curl -X POST http://localhost:8080/api/v1/documents \
  -F "file=@system-design.pdf"
# → note the documentId in the response

# 2. Generate embeddings for the document
curl -X POST http://localhost:8080/api/v1/embeddings/generate/{documentId}

# 3. Ask a question
curl -X POST http://localhost:8080/api/v1/rag/ask \
  -H "Content-Type: application/json" \
  -d '{"query": "What is the CAP theorem?", "topK": 5}'
```

> **Tip:** Step 2 can also be run globally after ingesting multiple documents:
> ```bash
> curl -X POST http://localhost:8080/api/v1/embeddings/generate
> ```
