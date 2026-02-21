-- Add embedding column to document_chunks
ALTER TABLE document_chunks
ADD COLUMN embedding VECTOR(1536);

-- Create index for fast similarity search
CREATE INDEX idx_chunks_embedding
ON document_chunks
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);