-- Run this in your Supabase SQL editor
-- Creates the api_keys table with RLS

create table if not exists public.api_keys (
  id          uuid default gen_random_uuid() primary key,
  key         text not null unique,
  name        text not null,
  email       text not null,
  usage_count integer default 0,
  is_active   boolean default true,
  created_at  timestamptz default now(),
  last_used   timestamptz
);

-- Indexes
create index if not exists idx_api_keys_key on public.api_keys(key);
create index if not exists idx_api_keys_email on public.api_keys(email);
create index if not exists idx_api_keys_active on public.api_keys(is_active);

-- Enable RLS
alter table public.api_keys enable row level security;

-- Policy: anyone can INSERT (sign up for a key)
create policy "Anyone can create an API key"
  on public.api_keys for insert
  with check (true);

-- Policy: anyone can read/validate by key value (needed for server auth)
create policy "Anyone can validate API keys"
  on public.api_keys for select
  using (true);

-- Policy: increment usage_count
create policy "Anyone can update usage count"
  on public.api_keys for update
  using (true)
  with check (true);

-- Optional: view to expose stats without exposing emails
create or replace view public.api_key_stats as
  select
    count(*) as total_keys,
    count(*) filter (where is_active) as active_keys,
    sum(usage_count) as total_requests
  from public.api_keys;

grant select on public.api_key_stats to anon;

-- Comment
comment on table public.api_keys is 'API keys for LocalLLM developer API access';
