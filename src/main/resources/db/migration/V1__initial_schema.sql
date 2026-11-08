--
-- PostgreSQL database dump
--

\restrict PSbeKm4Z91XDPHb1ExpvzCov8sJPVVEDb5c0831RJ5vIQ8iaaTBn5tvBo4uHRIB

-- Dumped from database version 18.6
-- Dumped by pg_dump version 18.6

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: accounts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.accounts (
    id uuid NOT NULL,
    account_number character varying(255) NOT NULL,
    balance_minor_units bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    currency character varying(3) NOT NULL,
    status character varying(255) NOT NULL,
    version bigint NOT NULL,
    user_id uuid,
    account_type character varying(20) DEFAULT 'CUSTOMER'::character varying NOT NULL,
    CONSTRAINT accounts_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'FROZEN'::character varying, 'CLOSED'::character varying])::text[]))),
    CONSTRAINT chk_account_balance CHECK ((balance_minor_units >= 0)),
    CONSTRAINT chk_account_owner CHECK (((((account_type)::text = 'CUSTOMER'::text) AND (user_id IS NOT NULL)) OR (((account_type)::text = 'CLEARING'::text) AND (user_id IS NULL))))
);


--
-- Name: app_users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.app_users (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    email character varying(150) NOT NULL,
    full_name character varying(100) NOT NULL,
    password character varying(255) NOT NULL,
    role character varying(255) NOT NULL,
    CONSTRAINT app_users_role_check CHECK (((role)::text = ANY ((ARRAY['USER'::character varying, 'ADMIN'::character varying])::text[])))
);


--
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_logs (
    id uuid NOT NULL,
    action character varying(100) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    details text,
    entity_id uuid,
    entity_type character varying(100) NOT NULL,
    user_id uuid
);


--
-- Name: financial_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.financial_transactions (
    id uuid NOT NULL,
    amount_minor_units bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    currency character varying(3) NOT NULL,
    description character varying(255),
    status character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    initiated_by uuid NOT NULL,
    CONSTRAINT financial_transactions_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying, 'REVERSED'::character varying])::text[]))),
    CONSTRAINT financial_transactions_type_check CHECK (((type)::text = ANY ((ARRAY['DEPOSIT'::character varying, 'WITHDRAWAL'::character varying, 'TRANSFER'::character varying])::text[])))
);


--
-- Name: idempotency_records; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.idempotency_records (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    expires_at timestamp(6) without time zone,
    idempotency_key character varying(255) NOT NULL,
    request_hash character varying(64) NOT NULL,
    response_body text,
    response_status integer,
    status character varying(255) NOT NULL,
    user_id uuid NOT NULL,
    CONSTRAINT idempotency_records_status_check CHECK (((status)::text = ANY ((ARRAY['PROCESSING'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying])::text[])))
);


--
-- Name: ledger_entries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ledger_entries (
    id uuid NOT NULL,
    amount_minor_units bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    currency character varying(3) NOT NULL,
    entry_type character varying(255) NOT NULL,
    account_id uuid NOT NULL,
    transaction_id uuid NOT NULL,
    CONSTRAINT ledger_entries_entry_type_check CHECK (((entry_type)::text = ANY ((ARRAY['DEBIT'::character varying, 'CREDIT'::character varying])::text[])))
);


--
-- Name: accounts accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_pkey PRIMARY KEY (id);


--
-- Name: app_users app_users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT app_users_pkey PRIMARY KEY (id);


--
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- Name: financial_transactions financial_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.financial_transactions
    ADD CONSTRAINT financial_transactions_pkey PRIMARY KEY (id);


--
-- Name: idempotency_records idempotency_records_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.idempotency_records
    ADD CONSTRAINT idempotency_records_pkey PRIMARY KEY (id);


--
-- Name: ledger_entries ledger_entries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ledger_entries
    ADD CONSTRAINT ledger_entries_pkey PRIMARY KEY (id);


--
-- Name: app_users uk4vj92ux8a2eehds1mdvmks473; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT uk4vj92ux8a2eehds1mdvmks473 UNIQUE (email);


--
-- Name: accounts uk6kplolsdtr3slnvx97xsy2kc8; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT uk6kplolsdtr3slnvx97xsy2kc8 UNIQUE (account_number);


--
-- Name: idempotency_records uk_user_idempotency_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.idempotency_records
    ADD CONSTRAINT uk_user_idempotency_key UNIQUE (user_id, idempotency_key);


--
-- Name: idx_audit_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_created ON public.audit_logs USING btree (created_at);


--
-- Name: idx_audit_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_user ON public.audit_logs USING btree (user_id);


--
-- Name: uq_clearing_account_currency; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_clearing_account_currency ON public.accounts USING btree (currency) WHERE ((account_type)::text = 'CLEARING'::text);


--
-- Name: financial_transactions fk2qxn6ahie0pj0uu8ayvj0oqjk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.financial_transactions
    ADD CONSTRAINT fk2qxn6ahie0pj0uu8ayvj0oqjk FOREIGN KEY (initiated_by) REFERENCES public.app_users(id);


--
-- Name: ledger_entries fk3iqdvytm7focledt7uowxlo15; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ledger_entries
    ADD CONSTRAINT fk3iqdvytm7focledt7uowxlo15 FOREIGN KEY (transaction_id) REFERENCES public.financial_transactions(id);


--
-- Name: ledger_entries fkccr7q587j5bf49xqb0fqieefp; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ledger_entries
    ADD CONSTRAINT fkccr7q587j5bf49xqb0fqieefp FOREIGN KEY (account_id) REFERENCES public.accounts(id);


--
-- Name: idempotency_records fknaqc2j0dbyo27wp1sfb2dg7k8; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.idempotency_records
    ADD CONSTRAINT fknaqc2j0dbyo27wp1sfb2dg7k8 FOREIGN KEY (user_id) REFERENCES public.app_users(id);


--
-- Name: accounts fkp8a77bvm08mtag15qjw63a217; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT fkp8a77bvm08mtag15qjw63a217 FOREIGN KEY (user_id) REFERENCES public.app_users(id);


--
-- Name: audit_logs fkqtxpcyjfyvcehqtn8n73di8du; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT fkqtxpcyjfyvcehqtn8n73di8du FOREIGN KEY (user_id) REFERENCES public.app_users(id);


--
-- PostgreSQL database dump complete
--

\unrestrict PSbeKm4Z91XDPHb1ExpvzCov8sJPVVEDb5c0831RJ5vIQ8iaaTBn5tvBo4uHRIB

