-- Add remittance_description column to disbursement_disbursement (Django migration 0015)
ALTER TABLE public.disbursement_disbursement ADD COLUMN IF NOT EXISTS remittance_description character varying(250) NOT NULL DEFAULT '';
