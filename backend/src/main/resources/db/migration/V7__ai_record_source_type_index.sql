ALTER TABLE ai_recognition_record
    ADD INDEX idx_doc_type_confirmed_order_id (doc_type, confirmed_order_id, id);
