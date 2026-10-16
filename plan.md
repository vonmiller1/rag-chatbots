# Improvement plan

## Current baseline

The service accepts PDF uploads, extracts and chunks text, stores vectors and source metadata in Qdrant, and answers questions with retrieved context through OpenRouter. Qdrant is shared across requests. Embeddings currently come from a deterministic token-hashing implementation; this is a baseline, not a trained semantic model. Authentication, document-scoped search, deletion, and asynchronous ingestion are not implemented.

## Priorities

### 1. Improve retrieval quality

- Replace the hashing embedding implementation with a configurable, production embedding provider.
- Use the same model and preprocessing for document chunks and questions.
- Define a safe migration or collection-recreation path when embedding model or vector dimension changes; re-embed stored documents.
- Evaluate retrieval against a small representative set of questions and expected source passages before selecting defaults.

**Completion criteria:** matching query/document vectors are produced by the selected model, existing data can be re-indexed without mixing vector spaces, and retrieval quality is measured on a repeatable evaluation set.

### 2. Add document lifecycle and isolation

- Add authentication and associate uploaded documents with an owner or tenant.
- Scope retrieval to the authorized owner's documents.
- Add document listing and deletion, including removal of associated Qdrant points.
- Persist document metadata and ingestion status so operations do not depend on vector payloads alone.

**Completion criteria:** unauthorized callers cannot read or delete another owner's documents; document deletion removes metadata and vectors; API behavior is covered by focused tests.

### 3. Harden ingestion and external calls

- Validate actual PDF content as well as the submitted filename; handle scanned or text-empty files explicitly.
- Bound upload size and processing cost, and avoid unbounded in-memory PDF reads for large uploads.
- Add explicit timeout, retry, and error-mapping policies for Qdrant and OpenRouter calls.
- Make ingestion resilient to partial failures and safe to retry without creating duplicate chunks.

**Completion criteria:** invalid, empty, oversized, and textless documents fail with actionable client errors; dependency failures return stable API errors; retries do not leave duplicate or orphaned data.

### 4. Improve operations and scale

- Move ingestion to a background job once document volume or request latency requires it; expose status to clients.
- Add structured logs, metrics, tracing, and health checks that distinguish application health from dependency readiness.
- Add backups and a documented restore procedure for Qdrant and persisted metadata.
- Add deployment packaging and environment-specific configuration only when a target deployment is selected.

**Completion criteria:** operators can diagnose ingestion and dependency failures, restore persisted data, and observe job progress under representative load.

## Scope guidance

Implement and measure retrieval quality before tuning ranking or prompt behavior. Add authorization before exposing document APIs beyond a trusted local environment. Keep production deployment work tied to an actual hosting target rather than introducing infrastructure prematurely.
