# FHIR Receiver Integration

## 1. Manual vs. Automatic Ingestion

| Aspect       | Manual                                                      | Automatic                                   |
|--------------|-------------------------------------------------------------|---------------------------------------------|
| Trigger      | User uploads a Bundle or pastes JSON                         | Messages arrive over REST API or messaging  |
| Workflow     | Admin UI validates then imports user uploads                 | Endpoint listener handles POST/PUT requests |
| Validation   | Show FHIR validation errors in UI                            | Reject with 422 or quarantine failures      |
| Human review | Required before commit                                       | Optional (audit trail only)                 |

## 2. Idempotency & Deduplication

When the sender resends the same resource (e.g., `Patient/123` or an Observation with identical business ID), avoid duplicates.

### a. Use Resource IDs for Idempotent PUT

```http
PUT /Patient/abc123
Content-Type: application/fhir+json

{
  "resourceType": "Patient",
  "id": "abc123",
  "active": true
}
```  
Re-`PUT`ing the same body will replace (or no-op) unchanged content.

### b. Conditional Create / Update

Use `If-None-Exist` or `If-Match` headers to match on business identifiers:

```http
POST /Patient
If-None-Exist: identifier=http://example.org/ohip|1234567890
Content-Type: application/fhir+json

{
  "resourceType": "Patient",
  "identifier": [
    {
      "system": "http://example.org/ohip",
      "value": "1234567890"
    }
  ]
}
```  
- If a matching Patient exists, returns **200** + existing resource.  
- Otherwise creates a new one.

### c. Transaction Bundles for Atomicity

```json
{
  "resourceType": "Bundle",
  "type": "transaction",
  "entry": [
    {
      "request": { "method": "PUT",  "url": "Patient/abc123" },
      "resource": { ...Patient... }
    },
    {
      "request": { "method": "POST", "url": "Observation" },
      "resource": { ...Observation referencing Patient/abc123... }
    }
  ]
}
```  

## 3. Conflict Resolution & Versioning

- Compare `meta.lastUpdated` timestamps; skip or flag older updates.  
- Merge field-level changes when safe, or surface conflicts.  
- Support ETags (`If-Match`) to prevent lost updates.

## 4. Practical Tips

- Quarantine invalid or conflicting Bundles.  
- Audit incoming messages (origin, timestamps, counts, validation results).  
- Expose a status endpoint for processing results.  
- Leverage built-in conditional operations and transaction Bundles.  

