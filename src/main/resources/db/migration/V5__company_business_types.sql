CREATE TABLE company_business_types (
    company_login varchar(120) NOT NULL,
    business_type varchar(32)  NOT NULL,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    PRIMARY KEY (company_login, business_type)
);