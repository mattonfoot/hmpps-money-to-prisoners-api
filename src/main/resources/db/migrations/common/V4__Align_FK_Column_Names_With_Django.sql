-- Align FK column names with Django convention: {field}_id

-- credit_credit: prison → prison_id
ALTER TABLE credit_credit RENAME COLUMN prison TO prison_id;

-- disbursement_disbursement: prison → prison_id
ALTER TABLE disbursement_disbursement RENAME COLUMN prison TO prison_id;
