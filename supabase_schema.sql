-- =====================================================================
-- DIAMONDS LUXURY CHARTER & FLEET PLATFORM
-- CORE POSTGRESQL SCHEMA FOR SUPABASE
-- Includes multi-tenant tables, indexes, triggers, and Row-Level Security
-- =====================================================================

-- 1. EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. ENUMS
CREATE TYPE employee_role AS ENUM ('admin', 'operator', 'driver', 'crew');
CREATE TYPE booking_status AS ENUM ('pending', 'confirmed', 'active', 'completed', 'cancelled');
CREATE TYPE customer_tier AS ENUM ('platinum', 'centurion', 'royal_diamonds');
CREATE TYPE asset_type AS ENUM ('yacht', 'helicopter', 'jet', 'suv');

-- 3. COMPANIES TABLE (Multi-tenant root)
CREATE TABLE companies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    subdomain VARCHAR(100) UNIQUE NOT NULL,
    logo_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 4. EMPLOYEES TABLE (Linked to auth.users)
CREATE TABLE employees (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    company_id UUID REFERENCES companies(id) ON DELETE CASCADE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role employee_role DEFAULT 'operator' NOT NULL,
    biometrics_enrolled BOOLEAN DEFAULT false NOT NULL,
    device_token TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 5. CUSTOMERS TABLE (VIP Guest Profiles)
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID REFERENCES companies(id) ON DELETE CASCADE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    tier customer_tier DEFAULT 'platinum' NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    CONSTRAINT unique_company_customer_email UNIQUE (company_id, email)
);

-- 6. FLEET TABLE (Yachts, Jets, Helicopters, SUVs)
CREATE TABLE fleet (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID REFERENCES companies(id) ON DELETE CASCADE NOT NULL,
    name VARCHAR(255) NOT NULL,
    type asset_type NOT NULL,
    registration_number VARCHAR(100) NOT NULL,
    capacity INT NOT NULL,
    hourly_rate NUMERIC(12, 2) NOT NULL,
    status VARCHAR(50) DEFAULT 'available' NOT NULL, -- 'available', 'maintenance', 'in_use'
    current_gps_lat DOUBLE PRECISION,
    current_gps_lon DOUBLE PRECISION,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 7. BOOKINGS TABLE
CREATE TABLE bookings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID REFERENCES companies(id) ON DELETE CASCADE NOT NULL,
    customer_id UUID REFERENCES customers(id) ON DELETE RESTRICT NOT NULL,
    fleet_id UUID REFERENCES fleet(id) ON DELETE RESTRICT NOT NULL,
    primary_staff_id UUID REFERENCES employees(id) ON DELETE SET NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    status booking_status DEFAULT 'pending' NOT NULL,
    total_price NUMERIC(12, 2) NOT NULL,
    pickup_location TEXT,
    dropoff_location TEXT,
    pax_count INT DEFAULT 1 NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 8. INDEXES FOR HIGH-PERFORMANCE QUERYING
CREATE INDEX idx_employees_company ON employees(company_id);
CREATE INDEX idx_customers_company ON customers(company_id);
CREATE INDEX idx_fleet_company ON fleet(company_id);
CREATE INDEX idx_bookings_company ON bookings(company_id);
CREATE INDEX idx_bookings_customer ON bookings(customer_id);
CREATE INDEX idx_bookings_fleet ON bookings(fleet_id);
CREATE INDEX idx_bookings_time_range ON bookings(start_time, end_time);

-- 9. AUTOMATIC UPDATED_AT TRIGGER FUNCTION
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = timezone('utc'::text, now());
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply updated_at trigger to all tables
CREATE TRIGGER update_companies_modtime BEFORE UPDATE ON companies FOR EACH ROW EXECUTE PROCEDURE update_updated_at_column();
CREATE TRIGGER update_employees_modtime BEFORE UPDATE ON employees FOR EACH ROW EXECUTE PROCEDURE update_updated_at_column();
CREATE TRIGGER update_customers_modtime BEFORE UPDATE ON customers FOR EACH ROW EXECUTE PROCEDURE update_updated_at_column();
CREATE TRIGGER update_fleet_modtime BEFORE UPDATE ON fleet FOR EACH ROW EXECUTE PROCEDURE update_updated_at_column();
CREATE TRIGGER update_bookings_modtime BEFORE UPDATE ON bookings FOR EACH ROW EXECUTE PROCEDURE update_updated_at_column();

-- 10. ROW LEVEL SECURITY (RLS) POLICIES
-- Enable RLS on all tables
ALTER TABLE companies ENABLE ROW LEVEL SECURITY;
ALTER TABLE employees ENABLE ROW LEVEL SECURITY;
ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE fleet ENABLE ROW LEVEL SECURITY;
ALTER TABLE bookings ENABLE ROW LEVEL SECURITY;

-- Helper Function to resolve current user's company_id
CREATE OR REPLACE FUNCTION get_user_company_id()
RETURNS UUID AS $$
    SELECT company_id FROM public.employees WHERE id = auth.uid();
$$ LANGUAGE sql STABLE SECURITY DEFINER;

-- Policies for companies table
CREATE POLICY "Employees can view their own company details"
    ON companies FOR SELECT
    USING (id = get_user_company_id());

-- Policies for employees table
CREATE POLICY "Employees can view teammate records within their own company"
    ON employees FOR SELECT
    USING (company_id = get_user_company_id());

CREATE POLICY "Admins can manage employees in their own company"
    ON employees FOR ALL
    USING (company_id = get_user_company_id() AND EXISTS (
        SELECT 1 FROM public.employees 
        WHERE id = auth.uid() AND role = 'admin'
    ));

-- Policies for customers table
CREATE POLICY "Employees can view and update customers in their own company"
    ON customers FOR ALL
    USING (company_id = get_user_company_id());

-- Policies for fleet table
CREATE POLICY "Employees can view and update fleet in their own company"
    ON fleet FOR ALL
    USING (company_id = get_user_company_id());

-- Policies for bookings table
CREATE POLICY "Employees can manage bookings in their own company"
    ON bookings FOR ALL
    USING (company_id = get_user_company_id());
