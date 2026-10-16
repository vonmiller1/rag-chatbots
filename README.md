# RAG Chatbot

A Java 21 and Spring Boot service for asking questions about uploaded PDF documents. It extracts and chunks PDF text, stores chunk vectors in Qdrant, retrieves relevant passages, and uses an OpenRouter chat model to draft an answer with source excerpts.

> **Prototype note:** The current embedding implementation is a deterministic hashing baseline, not a pretrained semantic embedding model. Treat retrieval quality as a demonstration, not production-grade semantic search. See [plan.md](plan.md) for a focused improvement roadmap.

## Features

- PDF upload, text extraction with Apache PDFBox, and configurable text chunking
- Vector storage and similarity search through Qdrant
- Grounded answer generation with a configurable OpenRouter model
- Source passages and relevance scores returned with each answer
- Request validation, centralized API error handling, and Spring Boot Actuator health endpoint
- OpenAPI / Swagger UI documentation

## Request flow

1. `POST /upload` validates a PDF, extracts its text, splits it into chunks, and writes the chunks and vectors to Qdrant.
2. `POST /ask` embeds the question, searches the shared Qdrant collection, and filters results by the configured relevance threshold.
3. If relevant passages are found, the service sends them with the question to OpenRouter and returns the answer and source passages. If none pass the threshold, it returns a fixed “I don't know” answer without calling the chat model.

## Technology

| Component | Role |
| --- | --- |
| Java 21, Spring Boot 3 | Application and REST API |
| Apache PDFBox | PDF text extraction |
| Qdrant | Vector collection and similarity search |
| OpenRouter | Chat completion provider |
| Maven | Build and dependency management |
| Docker Compose | Local Qdrant service |

## Prerequisites

- Java 21
- Maven 3.6.3 or newer
- Docker with the Compose plugin
- An OpenRouter API key for answer generation

## Run locally

Clone the repository and enter its directory:

```bash
git clone https://github.com/vonmiller1/rag-chatbots.git
cd rag-chatbots
```

Start Qdrant:

```bash
docker compose up -d
```

Set the OpenRouter key in the shell where the application will run.

PowerShell:

```powershell
$env:OPENROUTER_API_KEY = "your-openrouter-api-key"
```

macOS / Linux:

```bash
export OPENROUTER_API_KEY="your-openrouter-api-key"
```

Start the service:

```bash
mvn spring-boot:run
```

The API listens on `http://localhost:8080`. Swagger UI is available at `http://localhost:8080/swagger-ui/index.html`; the Actuator health endpoint is `http://localhost:8080/actuator/health`.

Qdrant is configured at `http://localhost:6333`. Compose stores its data in the named `qdrant_data` volume, so uploaded vectors persist across container restarts.

## Configuration

Defaults are in [`src/main/resources/application.yml`](src/main/resources/application.yml). Override them with Spring Boot configuration or environment variables:

| Setting | Default | Description |
| --- | --- | --- |
| `OPENROUTER_API_KEY` | Empty | OpenRouter bearer token; required when a question reaches answer generation |
| `OPENROUTER_BASE_URL` | `https://openrouter.ai/api/v1` | OpenRouter-compatible API base URL |
| `OPENROUTER_MODEL` | `openai/gpt-4o-mini` | Chat model identifier |
| `QDRANT_BASE_URL` | `http://localhost:6333` | Qdrant HTTP endpoint |
| `QDRANT_COLLECTION_NAME` | `document_chunks` | Collection created and queried by the application |
| `RAG_CHUNK_SIZE` | `900` | Target text chunk size |
| `RAG_CHUNK_OVERLAP` | `150` | Overlap between adjacent chunks |
| `RAG_DEFAULT_TOP_K` | `5` | Number of candidate passages to retrieve by default |
| `RAG_MAX_CONTEXT_CHARS` | `7000` | Maximum context size used when building the prompt |
| `RAG_MIN_RELEVANCE_SCORE` | `0.15` | Minimum similarity score for a source to be included |
| `EMBEDDING_DIMENSION` | `384` | Vector length; must match the Qdrant collection configuration |

The keys above follow Spring Boot's environment-variable binding conventions for the YAML property names. The application creates its Qdrant collection at startup if it is missing. Changing the embedding dimension for an existing collection requires recreating that collection and re-indexing its documents.

## API

### Upload a PDF

`POST /upload` — multipart form upload with a required `file` field. Only non-empty files whose names end in `.pdf` are accepted.

```bash
curl -X POST http://localhost:8080/upload \
  -F "file=@sample.pdf"
```

Example response:

```json
{
  "documentId": "generated-document-id",
  "fileName": "sample.pdf",
  "chunksStored": 18
}
```

### Ask a question

`POST /ask` — JSON body with a non-blank `question` and optional `topK` (integer from 1 to 20).

```bash
curl -X POST http://localhost:8080/ask \
  -H "Content-Type: application/json" \
  -d '{"question":"What is the main topic of the document?","topK":5}'
```

Example response:

```json
{
  "answer": "The document discusses ...",
  "sources": [
    {
      "documentName": "sample.pdf",
      "chunkIndex": 2,
      "score": 0.82,
      "text": "A retrieved passage from the document ..."
    }
  ]
}
```

Questions search the configured collection; the current API does not scope searches to a particular `documentId`.

## Project layout

```text
src/
├── main/
│   ├── java/com/example/rag/
│   │   ├── api/          # REST endpoints, DTOs, and error handling
│   │   ├── config/       # Typed application settings and HTTP clients
│   │   ├── llm/          # Chat client and OpenRouter integration
│   │   ├── service/      # Ingestion, PDF extraction, chunking, and Q&A
│   │   └── vector/       # Vector-store interface and Qdrant adapter
│   └── resources/
│       └── application.yml
└── test/                 # Service unit tests
```

## Current limitations

- Hashing embeddings are useful as a dependency-free baseline but do not provide the semantic quality of a trained embedding model.
- Documents share one collection; there is no user isolation, authentication, or document-specific search endpoint.
- Ingestion is synchronous, and the API supports PDF files only.
- The service does not yet include operational safeguards such as rate limiting, malware scanning, or a production secret-management strategy.

Do not expose the service or Qdrant publicly without adding appropriate access controls and operational protections.

## Development

Run the unit test suite with:

```bash
mvn test
```

See [plan.md](plan.md) for prioritized next steps and suggested completion criteria.
