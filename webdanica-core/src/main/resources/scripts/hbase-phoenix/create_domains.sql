DROP INDEX IF EXISTS domains_updated_time_idx ON domains;
DROP INDEX IF EXISTS domains_danicastatus_idx ON domains;
DROP INDEX IF EXISTS domains_tld_idx ON domains;
DROP TABLE IF EXISTS domains;

CREATE TABLE domains (
  domain VARCHAR PRIMARY KEY,
  danicastatus INTEGER,
  inserted_time TIMESTAMP,
  updated_time TIMESTAMP,
  danicastatus_reason VARCHAR(256),
  tld VARCHAR(64),
  danica_parts VARCHAR[],
  analyzed INTEGER,
  danicacount INTEGER,
  notes VARCHAR
);

CREATE INDEX domains_updated_time_idx ON domains (updated_time);
CREATE INDEX domains_danicastatus_idx ON domains (danicastatus);
CREATE INDEX domains_tld_idx ON domains (tld);
