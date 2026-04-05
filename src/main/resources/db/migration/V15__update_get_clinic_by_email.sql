CREATE OR REPLACE FUNCTION public.get_clinic_by_email(p_email text)
 RETURNS TABLE(
    id uuid,
    name character varying,
    tax_id character varying,
    email character varying,
    phone character varying,
    address text,
    city character varying,
    country character varying,
    logo_url character varying,
    subscription_plan character varying,
    subscription_expires_at timestamp with time zone,
    active boolean,
    settings text,
    created_at timestamp with time zone,
    updated_at timestamp with time zone,
    deleted boolean,
    deleted_at timestamp with time zone,
    version integer,
    registration_number character varying,
    activity_code character varying,
    bank_account character varying,
    vat_payer boolean,
    veterinary_license_number character varying
 )
 LANGUAGE sql
 STABLE SECURITY DEFINER
AS $function$
    SELECT id, name, tax_id, email, phone, address, city, country,
           logo_url, subscription_plan::VARCHAR, subscription_expires_at,
           active, settings::TEXT, created_at, updated_at,
           deleted, deleted_at, version,
           registration_number, activity_code, bank_account, vat_payer, veterinary_license_number
    FROM clinic
    WHERE email = p_email
      AND deleted = false
    LIMIT 1;
$function$;
