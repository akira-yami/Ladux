-- Reference data only: no demo users, credentials, products or inventory.
INSERT INTO roles(name) VALUES ('ADMIN'), ('STAFF'), ('CUSTOMER') ON CONFLICT (name) DO NOTHING;
