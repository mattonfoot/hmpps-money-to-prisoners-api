-- Add missing columns to security_savedsearch to match Django model
ALTER TABLE public.security_savedsearch ADD COLUMN last_result_count integer NOT NULL DEFAULT 0;
ALTER TABLE public.security_savedsearch ADD COLUMN site_url character varying(1000);
